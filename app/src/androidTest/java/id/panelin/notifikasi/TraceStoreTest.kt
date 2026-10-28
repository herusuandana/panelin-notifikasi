package id.panelin.notifikasi

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class TraceStoreTest {
    @Test fun skippedContentIsAbsentAndAttemptHistorySurvivesRetries() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse("Run this fixture with emulator networking disabled", context.getSystemService(android.net.ConnectivityManager::class.java).activeNetworkInfo?.isConnected == true)
        val store = AppStore(context)
        val pkg = "id.panelin.tracefixture"
        store.saveDestination(Destination(pkg,"Trace fixture","https://fixture.invalid/callback/notification/7","trace-secret",true))
        store.enabled = true
        try {
            store.recordSkip(pkg,"sensitive","skip-fixture")
            val skip = store.deliveries().single { it.packageName == pkg && it.state == "skipped" }
            assertEquals("",skip.text)
            assertEquals("",skip.title)
            assertTrue(skip.trace.any { it.stage == "Dilewati di HP" })
            assertTrue(store.enqueue(pkg,"Trace fixture","Pembayaran Masuk","Rp1.456 diterima DANA Bisnis.","","trace-fixture"))
            val pending = store.claim(System.currentTimeMillis())!!
            store.recordAttempt(pending,1,1000)
            store.recordResponse(pending,1,503,42,"JSON valid",null,null)
            store.retry(pending,System.currentTimeMillis()+60000,"Akan dicoba lagi.")
            val delivery = store.deliveries().single { it.packageName == pkg && it.state != "skipped" }
            assertTrue(delivery.trace.any { it.detail.contains("HTTP 503") })
            assertTrue(delivery.trace.any { it.stage == "Dijadwalkan ulang" })
            assertFalse(delivery.trace.toString().contains("trace-secret"))
            val before = store.snapshot()
            store.clearHistory()
            assertEquals(before.todayCount,store.snapshot().todayCount)
            assertFalse(store.enqueue(pkg,"Trace fixture","Pembayaran Masuk","Rp1.456 diterima DANA Bisnis.","","trace-fixture"))
            assertTrue(store.deliveries().any { it.id == delivery.id })
            val second = store.claim(System.currentTimeMillis()+120000)!!
            assertEquals(2,second.attempts)
            store.recordAttempt(second,2,System.currentTimeMillis())
            store.recordResponse(second,2,200,30,"JSON valid",true,"unmatched")
            store.complete(second,"unmatched","Tidak ada transaksi cocok",System.currentTimeMillis())
            val finished=store.deliveries().single { it.id==delivery.id }
            assertTrue(finished.trace.any { it.detail.contains("HTTP 503") })
            assertTrue(finished.trace.any { it.detail.contains("HTTP 200") })
            store.clearHistory()
            assertFalse(store.enqueue(pkg,"Trace fixture","Pembayaran Masuk","Rp1.456 diterima DANA Bisnis.","","trace-fixture"))
            android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath("panelin-secure.db").path,null,android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("WITH RECURSIVE n(x) AS (SELECT 1 UNION ALL SELECT x+1 FROM n WHERE x<2001) INSERT INTO trace(event,kind,created,payload) SELECT event,kind,created,payload FROM trace,n WHERE trace.id=(SELECT MAX(id) FROM trace)")
            }
            store.recordSkip(pkg,"promotion","retention-final")
            android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath("panelin-secure.db").path,null,android.database.sqlite.SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery("SELECT COUNT(*) FROM trace",null).use { assertTrue(it.moveToFirst()); assertEquals(2000,it.getInt(0)) }
                db.rawQuery("SELECT COUNT(*) FROM seen",null).use { assertTrue(it.moveToFirst()); assertEquals(1,it.getInt(0)) }
            }
        } finally {
            store.enabled=false
            store.removeDestination(pkg)
        }
    }
}

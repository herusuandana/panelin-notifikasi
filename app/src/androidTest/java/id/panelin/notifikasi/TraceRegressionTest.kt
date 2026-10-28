package id.panelin.notifikasi

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test

class TraceRegressionTest {
    @Test fun diagnosticsStayPrivateAndCannotHideOrReplayPayments() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse("Run this fixture with emulator networking disabled", context.getSystemService(android.net.ConnectivityManager::class.java).activeNetworkInfo?.isConnected == true)
        val store = AppStore(context)
        val a = Destination("id.panelin.rotation", "Rotation", "https://fixture.invalid/callback/notification/1", "oldsecret", true)
        val b = Destination("id.panelin.tracecheck", "Trace", "https://oldsecret.example/callback/notification/2", "newsecret", true)
        try {
            store.saveDestination(a)
            store.saveDestination(b)
            store.enabled = true
            assertTrue(store.enqueue(a.packageName,a.label,"Pembayaran Masuk","Rp1.456 diterima DANA Bisnis.","","old"))
            store.saveDestination(a.copy(token="rotatedsecret"))
            assertTrue(store.enqueue(b.packageName,b.label,"Pembayaran Masuk","Rp1.692 diterima DANA Bisnis.","","new"))
            val pending = store.claim(System.currentTimeMillis())!!
            store.recordAttempt(pending,1,System.currentTimeMillis())
            assertFalse(store.deliveries().single { it.id==pending.id }.trace.toString().contains("oldsecret"))
            repeat(151) { store.recordSkip(b.packageName,"promotion","promo-$it") }
            assertTrue(store.deliveries(150).any { it.id==pending.id })
            SQLiteDatabase.openDatabase(context.getDatabasePath("panelin-secure.db").path,null,SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("UPDATE destinations SET payload=X'010203' WHERE route=?",arrayOf(Core.eventIdentity(a.packageName)))
            }
            store.recordAttempt(pending,1,System.currentTimeMillis())
            store.recordResponse(pending,1,200,3,"JSON valid",true,"credited")
            store.complete(pending,"credited","Server mengonfirmasi saldo dikreditkan",System.currentTimeMillis())
            assertEquals("credited",store.deliveries(150).single { it.id==pending.id }.outcome)
            store.clearHistory()
            assertFalse(store.enqueue(b.packageName,b.label,"Pembayaran Masuk","Rp1.692 diterima DANA Bisnis.","","new"))
            assertTrue(store.deliveries().any { it.state=="held" })
            assertTrue(store.enqueue(b.packageName,b.label,"Pembayaran Masuk","Rp2.000 diterima DANA Bisnis.","","lease"))
            val last = store.claim(System.currentTimeMillis())!!
            SQLiteDatabase.openDatabase(context.getDatabasePath("panelin-secure.db").path,null,SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("UPDATE queue SET attempts=8,lease_until=1 WHERE id=?",arrayOf(last.id))
            }
            assertNull(store.claim(System.currentTimeMillis()))
            val exhausted=store.deliveries().single { it.id==last.id }
            assertEquals("attention",exhausted.state)
            assertTrue(exhausted.trace.any { it.stage=="Percobaan terputus" })
            assertTrue(exhausted.trace.any { it.stage=="Perlu perhatian" })
        } finally {
            store.enabled=false
        }
    }
}

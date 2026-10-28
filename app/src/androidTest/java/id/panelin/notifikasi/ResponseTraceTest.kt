package id.panelin.notifikasi

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLStreamHandler
import java.net.SocketTimeoutException
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

class ResponseTraceTest {
    @Test fun actualWorkerRecordsOnlySafeResponseFieldsAndPreservesRetryEvidence() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val fixtures = mapOf(
            "1" to (200 to "{\"status\":true,\"outcome\":\"unmatched\",\"sign\":\"trace-secret\",\"message\":\"OTP 123456\"}"),
            "2" to (401 to "<html>trace-secret OTP 123456</html>"),
            "3" to (503 to "{\"message\":\"trace-secret\"}"),
            "4" to (200 to "x".repeat(66000)),
            "5" to (200 to ""),
            "6" to (200 to "TIMEOUT")
        )
        val sent = java.util.concurrent.CopyOnWriteArrayList<String>()
        URL.setURLStreamHandlerFactory { protocol ->
            if (protocol != "https") null else object : URLStreamHandler() {
                override fun openConnection(url: URL): HttpsURLConnection {
                    check(url.host == "fixture.invalid")
                    val id = url.path.substringAfterLast('/')
                    val fixture = fixtures[id] ?: (200 to "{}")
                    return object : HttpsURLConnection(url) {
                        override fun connect() = Unit
                        override fun disconnect() = Unit
                        override fun usingProxy() = false
                        override fun getCipherSuite() = "TLS_AES_128_GCM_SHA256"
                        override fun getLocalCertificates(): Array<Certificate>? = null
                        override fun getServerCertificates(): Array<Certificate> = emptyArray()
                        override fun getOutputStream() = ByteArrayOutputStream().also { sent.add(id) }
                        override fun getResponseCode(): Int {
                            if (fixture.second == "TIMEOUT") throw SocketTimeoutException("trace-secret OTP 123456")
                            return fixture.first
                        }
                        override fun getInputStream() = ByteArrayInputStream(fixture.second.toByteArray())
                        override fun getErrorStream() = ByteArrayInputStream(fixture.second.toByteArray())
                    }
                }
            }
        }
        val store = AppStore(context)
        try {
            store.enabled=true
            for (id in fixtures.keys) {
                val pkg="id.panelin.response$id"
                store.saveDestination(Destination(pkg,"Response $id","https://fixture.invalid/callback/notification/$id","trace-secret",true))
                assertTrue(store.enqueue(pkg,"Response $id","Pembayaran Masuk","Rp1.456 diterima DANA Bisnis.","","event-$id"))
            }
            val deadline=System.currentTimeMillis()+90000
            while (System.currentTimeMillis()<deadline && store.deliveries().count { it.trace.any { step -> step.stage.startsWith("Hasil percobaan") } }<6) Thread.sleep(200)
            val rows=store.deliveries().filter { it.packageName.startsWith("id.panelin.response") }
            assertEquals(6,rows.size)
            for (row in rows) {
                assertTrue(row.trace.any { it.stage.startsWith("Hasil percobaan") })
                assertFalse(row.trace.toString().contains("trace-secret"))
                assertFalse(row.trace.toString().contains("123456"))
            }
            assertEquals("unmatched",rows.single { it.packageName.endsWith("1") }.outcome)
            assertTrue(rows.single { it.packageName.endsWith("2") }.trace.any { it.detail.contains("HTTP 401") && it.detail.contains("HTML") })
            assertTrue(rows.single { it.packageName.endsWith("3") }.trace.any { it.stage=="Dijadwalkan ulang" })
            assertTrue(rows.single { it.packageName.endsWith("4") }.trace.any { it.detail.contains("Respons terlalu besar") })
            assertTrue(rows.single { it.packageName.endsWith("5") }.trace.any { it.detail.contains("Respons kosong") })
            assertTrue(rows.single { it.packageName.endsWith("6") }.trace.any { it.detail.contains("Batas waktu koneksi") })
            // Revoke the destination while attempt diagnostics are written, before output opens.
            android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath("panelin-secure.db").path,null,android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("UPDATE queue SET next_at=9223372036854775807 WHERE state='queued'")
                db.execSQL("CREATE TRIGGER revoke_before_output AFTER INSERT ON trace WHEN NEW.kind='attempt' BEGIN DELETE FROM destinations; END")
            }
            store.saveDestination(Destination("id.panelin.response7","Cancelled","https://fixture.invalid/callback/notification/7","trace-secret",true))
            assertTrue(store.enqueue("id.panelin.response7","Cancelled","Pembayaran Masuk","Rp2.000 diterima DANA Bisnis.","","cancelled"))
            val cancelDeadline=System.currentTimeMillis()+60000
            while (System.currentTimeMillis()<cancelDeadline && store.deliveries().none { it.packageName=="id.panelin.response7" && it.trace.any { step -> step.stage=="Dibatalkan sebelum dikirim" } }) Thread.sleep(200)
            assertTrue(store.deliveries().any { it.packageName=="id.panelin.response7" && it.trace.any { step -> step.stage=="Dibatalkan sebelum dikirim" } })
            assertFalse(sent.contains("7"))
        } finally {
            store.enabled=false
            fixtures.keys.forEach { store.removeDestination("id.panelin.response$it") }
        }
    }
}

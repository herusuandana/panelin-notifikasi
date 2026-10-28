package id.panelin.notifikasi

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLStreamHandler
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection

@RunWith(AndroidJUnit4::class)
class WorkerContractTest {
    @Test fun actualWorkerPostsExistingContractAndRecordsExplicitCreditWithoutNetwork() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val output = ByteArrayOutputStream()
        var request: HttpsURLConnection? = null
        URL.setURLStreamHandlerFactory { protocol ->
            if (protocol != "https") null else object : URLStreamHandler() {
                override fun openConnection(url: URL): HttpsURLConnection {
                    check(url.host == "fixture.invalid") { "No real network allowed" }
                    return object : HttpsURLConnection(url) {
                        override fun connect() = Unit
                        override fun disconnect() = Unit
                        override fun usingProxy() = false
                        override fun getCipherSuite() = "TLS_AES_128_GCM_SHA256"
                        override fun getLocalCertificates(): Array<Certificate>? = null
                        override fun getServerCertificates(): Array<Certificate> = emptyArray()
                        override fun getOutputStream() = output
                        override fun getResponseCode() = 200
                        override fun getInputStream() = ByteArrayInputStream("{\"status\":true,\"outcome\":\"credited\",\"sign\":\"fixture-token\",\"message\":\"OTP 123456\"}".toByteArray())
                    }.also { request = it }
                }
            }
        }
        val store = AppStore(context)
        val pkg = "id.panelin.workerfixture"
        try {
            store.saveDestination(Destination(pkg, "Worker fixture", "https://fixture.invalid/callback/notification/7", "fixture-token", true))
            // An unrelated damaged credential must not suppress or replace a confirmed result.
            android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath("panelin-secure.db").path,null,android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("INSERT INTO destinations(route,payload) VALUES ('unrelated-corrupt',X'010203')")
            }
            store.enabled = true
            assertTrue(store.enqueue(pkg, "Worker fixture", "Pembayaran Masuk", "Rp1.457 diterima DANA Bisnis.", "", "worker-event"))
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<DeliveryWorker>().build()).result.get()
            val deadline = System.currentTimeMillis() + 60000
            var delivered = false
            while (System.currentTimeMillis() < deadline) {
                if (store.deliveries().any { it.packageName == pkg && it.outcome == "credited" }) { delivered = true; break }
                Thread.sleep(200)
            }
            assertTrue("Actual worker must settle local outcome", delivered)
            val json = JSONObject(output.toString("UTF-8"))
            assertEquals("fixture-token", json.getString("sign"))
            assertEquals(pkg, json.getString("pkg"))
            assertEquals("Pembayaran Masuk", json.getString("title"))
            assertEquals("Rp1.457 diterima DANA Bisnis.", json.getString("text"))
            assertEquals("POST", request!!.requestMethod)
            assertFalse(request!!.instanceFollowRedirects)
            val delivery = store.deliveries().single { it.packageName == pkg && it.state != "skipped" }
            assertTrue(delivery.state == "done")
            assertTrue(delivery.trace.any { it.detail.contains("HTTP 200") })
            assertTrue(delivery.trace.any { it.stage.startsWith("Percobaan 1 dimulai") })
            assertFalse(delivery.trace.toString().contains("outcome: credited"))
            assertFalse(delivery.trace.toString().contains("fixture-token"))
            assertFalse(delivery.trace.toString().contains("123456"))
        } finally {
            store.enabled = false
            store.removeDestination(pkg)
        }
    }
}

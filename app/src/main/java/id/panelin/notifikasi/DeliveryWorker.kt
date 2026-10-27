package id.panelin.notifikasi

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import org.json.JSONObject
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class DeliveryWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result = synchronized(sendLock) {
        val store = AppStore(applicationContext)
        try {
            // Limit a run without limiting durable queue size or deleting pending payments.
            repeat(10) {
                if (isStopped || !store.enabled) return@synchronized Result.success()
                val pending = store.claim(System.currentTimeMillis()) ?: run {
                    store.nextDue()?.let { Forwarding.wakeAt(applicationContext, it) }
                    return@synchronized Result.success()
                }
                if (!store.maySend(pending)) {
                    store.release(pending)
                    return@synchronized Result.success()
                }
                send(store, pending)
            }
            store.nextDue()?.let { Forwarding.wakeAt(applicationContext, it) }
            Result.success()
        } catch (_: Exception) {
            // Infrastructure failure: preserve encrypted row and recover expired claim next run.
            Result.retry()
        }
    }

    private fun send(store: AppStore, pending: AppStore.Pending) {
        val endpoint = pending.data.getString("endpoint")
        if (!Core.validEndpoint(endpoint)) {
            store.complete(pending, "attention", "URL tujuan tidak valid; tidak dikirim", System.currentTimeMillis())
            return
        }
        var connection: HttpsURLConnection? = null
        try {
            connection = URL(endpoint).openConnection() as HttpsURLConnection
            connection.instanceFollowRedirects = false
            connection.requestMethod = "POST"
            connection.connectTimeout = 20_000
            connection.readTimeout = 30_000
            connection.useCaches = false
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.setRequestProperty("Accept", "application/json")
            val body = JSONObject().put("sign", pending.data.getString("sign"))
                .put("pkg", pending.data.getString("pkg"))
                .put("title", pending.data.getString("title"))
                .put("text", pending.data.getString("text"))
                .put("bigtext", pending.data.getString("bigtext"))
                .toString().toByteArray(Charsets.UTF_8)
            connection.setFixedLengthStreamingMode(body.size)
            // Re-check immediately before any credential or notification bytes leave the phone.
            if (isStopped || !store.maySend(pending)) {
                store.release(pending)
                return
            }
            connection.outputStream.use { it.write(body) }
            val code = connection.responseCode
            val raw = (if (code in 200..299) connection.inputStream else connection.errorStream)?.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (output.size() <= 65_536) {
                    val size = stream.read(buffer)
                    if (size == -1) break
                    output.write(buffer, 0, size)
                }
                if (output.size() > 65_536) "" else output.toString("UTF-8")
            }.orEmpty()
            val json = try { JSONObject(raw) } catch (_: Exception) { null }
            val outcome = Core.disposition(code, json?.opt("status") == true, json?.optString("outcome").orEmpty())
            if (outcome == "retry") retry(store, pending, connection.getHeaderField("Retry-After"), "HTTP $code; dijadwalkan ulang")
            else store.complete(pending, outcome, detail(outcome, code), System.currentTimeMillis())
        } catch (_: IOException) {
            retry(store, pending, null, "Gangguan jaringan; hasil server belum diketahui")
        } catch (_: Exception) {
            store.complete(pending, "attention", "Pengiriman tidak dapat diproses; periksa konfigurasi", System.currentTimeMillis())
        } finally {
            connection?.disconnect()
        }
    }

    private fun retry(store: AppStore, pending: AppStore.Pending, retryAfter: String?, detail: String) {
        if (pending.attempts >= MAX_ATTEMPTS) {
            store.complete(pending, "attention", "Batas percobaan tercapai; hasil server belum diketahui, periksa mutasi sebelum mengirim ulang", System.currentTimeMillis())
        } else {
            val now = System.currentTimeMillis()
            store.retry(pending, now + Core.retryDelay(pending.attempts, retryAfter, now), detail)
        }
    }

    private fun detail(outcome: String, code: Int): String = when (outcome) {
        "credited" -> "Server mengonfirmasi saldo dikreditkan"
        "duplicate" -> "Server menandai duplikat; tidak dikreditkan ulang"
        "ignored" -> "Diabaikan server; tidak dikreditkan"
        "unmatched" -> "Tidak cocok dengan transaksi; periksa Panelin"
        "ambiguous" -> "Lebih dari satu kecocokan; periksa Panelin"
        "review" -> "Memerlukan peninjauan di Panelin"
        else -> "HTTP $code; server tidak mengonfirmasi hasil yang valid"
    }

    companion object {
        private val sendLock = Any()
        private const val MAX_ATTEMPTS = 8
    }
}

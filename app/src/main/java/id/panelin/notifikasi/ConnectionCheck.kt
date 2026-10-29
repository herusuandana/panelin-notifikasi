package id.panelin.notifikasi

import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

/** Explicit user action only. Never falls back to the payment callback. */
internal fun checkConnection(endpoint: String, token: String): String {
    require(Core.validEndpoint(endpoint))
    require(UiRules.tokenError(token) == null)
    var connection: HttpsURLConnection? = null
    return try {
        connection = URL("$endpoint/check").openConnection() as HttpsURLConnection
        connection.instanceFollowRedirects = false
        connection.requestMethod = "POST"
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.useCaches = false
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.setRequestProperty("Accept", "application/json")
        val body = JSONObject().put("sign", token).toString().toByteArray(Charsets.UTF_8)
        connection.setFixedLengthStreamingMode(body.size)
        connection.outputStream.use { it.write(body) }
        val code = connection.responseCode
        val bytes = (if(code in 200..299) connection.inputStream else connection.errorStream)?.use {
            val result = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while(result.size() <= 4096) {
                val size = it.read(buffer)
                if(size == -1) break
                result.write(buffer, 0, size)
            }
            result.toByteArray()
        } ?: byteArrayOf()
        val json = if(bytes.size <= 4096) runCatching { JSONObject(String(bytes, Charsets.UTF_8)) }.getOrNull() else null
        Core.connectionResult(code, json?.opt("status") == true, json?.optString("outcome").orEmpty())
    } catch (_: Exception) {
        // Never display exceptions, arbitrary server text, or tokens.
        "Koneksi belum dapat diperiksa. Periksa jaringan dan alamat website."
    } finally { connection?.disconnect() }
}

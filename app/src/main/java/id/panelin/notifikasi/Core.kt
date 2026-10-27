package id.panelin.notifikasi

import java.net.URI

/** Pure policy: no Android or network side effects. */
object Core {
    private val sensitive = Regex("(?iu)\\b(gagal|dibatalkan|ditolak|failed|cancelled|canceled|rejected|otp|pin|password|passcode|verification|verifikasi|autentikasi|authentication|promo|promosi|diskon|cashback|voucher|login|log\\s*in)\\b|kode.{0,24}(rahasia|keamanan|masuk)|jangan\\s+bagikan|one[- ]time|transfer\\s+berhasil|pembayaran\\s+berhasil|berhasil\\s+(dikirim|transfer|membayar)|uang\\s+keluar")
    private val incoming = Regex("(?iu)diterima|menerima|pembayaran\\s+masuk|transfer\\s+masuk|dana\\s+masuk|uang\\s+masuk|payment\\s+received|received\\s+(a\\s+)?payment|incoming\\s+(payment|transfer)|credited")

    fun shouldForward(title: String, text: String, bigText: String): Boolean {
        if (title.isBlank() || title.length > 200 || text.length > 4000 || bigText.length > 4000) return false
        val content = "$title\n$text\n$bigText"
        return !sensitive.containsMatchIn(content) && incoming.containsMatchIn(content)
    }

    fun eventIdentity(vararg fields: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        fields.forEach { field ->
            val bytes = field.toByteArray(Charsets.UTF_8)
            digest.update(java.nio.ByteBuffer.allocate(4).putInt(bytes.size).array())
            digest.update(bytes)
        }
        return digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    fun disposition(code: Int, status: Boolean, outcome: String): String = when {
        code == 408 || code == 429 || code in 500..599 -> "retry"
        code in 200..299 && status && outcome in setOf("credited", "ignored", "unmatched", "ambiguous", "review", "duplicate") -> outcome
        else -> "attention"
    }

    fun retryDelay(attempt: Int, retryAfter: String?, now: Long): Long {
        val backoff = (60_000L * (1L shl (attempt - 1).coerceIn(0, 10))).coerceAtMost(21_600_000L)
        val seconds = retryAfter?.trim()?.toLongOrNull()
        val date = if (seconds == null && retryAfter != null) try {
            java.time.ZonedDateTime.parse(retryAfter, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - now
        } catch (_: Exception) { 0L } else 0L
        val requested = seconds?.coerceIn(0, 86400)?.times(1000) ?: date
        return maxOf(backoff, requested).coerceAtMost(86_400_000L)
    }

    fun validEndpoint(value: String): Boolean = try {
        val uri = URI(value)
        value == value.trim() && uri.scheme == "https" && !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null &&
            (uri.port == -1 || uri.port in 1..65535) &&
            Regex("/callback/notification/[0-9]+").matches(uri.rawPath ?: "")
    } catch (_: Exception) { false }
}

data class Destination(val packageName: String, val label: String, val endpoint: String, val token: String, val enabled: Boolean = true)
data class Delivery(val id: Long, val packageName: String, val appLabel: String, val title: String, val text: String, val bigText: String, val createdAt: Long, val state: String, val outcome: String, val attempts: Int, val detail: String)
data class Snapshot(val todayCount: Int, val creditedCount: Int, val queuedCount: Int, val attentionCount: Int, val lastReceivedAt: Long, val lastSentAt: Long)

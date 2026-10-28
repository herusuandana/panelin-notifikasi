package id.panelin.notifikasi

import java.net.URI

// UI_RULES_START
internal object UiRules {
    fun endpointError(value: String): String? {
        val uri = try { URI(value) } catch (_: Exception) { return "Alamat HTTPS tidak valid." }
        return if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.rawUserInfo != null ||
            uri.rawQuery != null || uri.rawFragment != null || uri.port !in -1..65535 || uri.port == 0 ||
            !Regex("/callback/notification/[0-9]+").matches(uri.rawPath ?: ""))
            "Gunakan https://domain/callback/notification/ID angka tanpa login, query, atau fragmen."
        else null
    }
    fun tokenError(value: String): String? = when {
        value.isBlank() -> "Token wajib diisi."
        value.length > 100 -> "Token maksimal 100 karakter."
        value.any { it.isWhitespace() || it.isISOControl() } -> "Token tidak boleh berisi spasi atau karakter kontrol."
        else -> null
    }
    fun outcomeLabel(value: String): String = when (value) {
        "credited" -> "Dikreditkan"
        "ignored" -> "Diabaikan website"
        "unmatched" -> "Tidak cocok"
        "ambiguous" -> "Beberapa transaksi cocok"
        "review" -> "Perlu tinjauan"
        "duplicate" -> "Duplikat"
        else -> "Belum ada hasil"
    }
}
// UI_RULES_END

package id.panelin.notifikasi

import org.junit.Assert.*
import org.junit.Test

class TracePolicyTest {
    @Test fun skippedNotificationHasAContentFreeReason() {
        assertNull(Core.skipReason("DANA", "Promo cashback 11.11", ""))
        assertEquals("sensitive", Core.skipReason("DANA", "Kode OTP 123456", ""))
        assertNull(Core.skipReason("Transfer berhasil", "Rp10.000", ""))
        assertNull(Core.skipReason("DANA", "Pesan baru", ""))
        assertEquals("invalid", Core.skipReason("", "Rp10.000 diterima", ""))
        assertNull(Core.skipReason("Pembayaran Masuk", "Rp1.456 diterima DANA Bisnis.", ""))
        assertFalse(Core.shouldForward("Pembayaran Masuk", "OTP 123456 diterima", ""))
    }

    @Test fun traceDestinationNeverIncludesCredentialsOrQueries() {
        assertEquals("https://example.com/callback/notification/4", Core.safeDestination("https://example.com/callback/notification/4", listOf("secret")))
        assertEquals("[tujuan disembunyikan]", Core.safeDestination("https://secret.example/callback/notification/4", listOf("secret")))
        assertEquals("[tujuan disembunyikan]", Core.safeDestination("https://user:secret@example.com/callback/notification/4?sign=secret", emptyList()))
    }
}

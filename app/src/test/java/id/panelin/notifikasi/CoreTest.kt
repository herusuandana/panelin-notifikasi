package id.panelin.notifikasi

import org.junit.Assert.*
import org.junit.Test

class CoreTest {
    @Test fun privacyGuardRejectsSecretsPromotionsOutgoingAndNoise() {
        listOf("Kode OTP 123456", "Verification code: 1234", "Jangan bagikan kode ini", "Promo cashback 50%", "Transfer berhasil", "Pembayaran berhasil", "Masukkan PIN", "Login dari perangkat baru").forEach {
            assertFalse(it, Core.shouldForward("Informasi", it, ""))
        }
        assertFalse(Core.shouldForward("Pembayaran Masuk", "Pembayaran gagal diterima", ""))
        assertFalse(Core.shouldForward("Pembayaran Masuk", "Rp1.000 diterima lalu dibatalkan", ""))
        assertFalse(Core.shouldForward("Pembayaran Masuk", "Rp1.000 diterima DANA Bisnis." + "x".repeat(4000), ""))
        assertFalse(Core.shouldForward("", "", ""))
        assertFalse(Core.shouldForward("DANA", "Pesan baru", ""))
        assertTrue(Core.shouldForward("Pembayaran Masuk", "Rp50.000 diterima DANA Bisnis", ""))
        assertTrue(Core.shouldForward("Transfer masuk", "Anda menerima Rp10.000 dari pelanggan", ""))
    }

    @Test fun eventIdentityDeduplicatesExactRepostsButPreservesUpdates() {
        val first = Core.eventIdentity("id.dana", "event:1", "Masuk", "Rp100", "")
        assertEquals(first, Core.eventIdentity("id.dana", "event:1", "Masuk", "Rp100", ""))
        assertNotEquals(first, Core.eventIdentity("id.dana", "event:1", "Masuk", "Rp200", ""))
        assertNotEquals(first, Core.eventIdentity("id.dana", "event:2", "Masuk", "Rp100", ""))
        assertNotEquals(Core.eventIdentity("a", "bc", "", "", ""), Core.eventIdentity("ab", "c", "", "", ""))
    }

    @Test fun responsePolicyDoesNotConfuseHttpSuccessWithCredit() {
        assertEquals("credited", Core.disposition(200, true, "credited"))
        listOf("ignored", "unmatched", "ambiguous", "review", "duplicate").forEach { assertEquals(it, Core.disposition(200, true, it)) }
        assertEquals("attention", Core.disposition(200, false, "credited"))
        assertEquals("attention", Core.disposition(200, true, "unknown"))
        listOf(408, 429, 500, 503).forEach { assertEquals("retry", Core.disposition(it, false, "")) }
        listOf(301, 401, 404, 422).forEach { assertEquals("attention", Core.disposition(it, false, "")) }
        assertEquals(60_000L, Core.retryDelay(1, null, 0))
        assertEquals(120_000L, Core.retryDelay(2, null, 0))
        assertEquals(120_000L, Core.retryDelay(1, "120", 0))
        assertEquals(86_400_000L, Core.retryDelay(1, "9999999999", 0))
        assertEquals(120_000L, Core.retryDelay(1, "Thu, 01 Jan 1970 00:02:00 GMT", 0))
    }

    @Test fun onlyStrictHttpsCallbackEndpointsAreAccepted() {
        assertTrue(Core.validEndpoint("https://example.com/callback/notification/12"))
        listOf(
            "http://example.com/callback/notification/12",
            "https://user:pass@example.com/callback/notification/12",
            "https://example.com/callback/notification/12?sign=secret",
            "https://example.com/callback/notification/12#fragment",
            "https://example.com/callback/notification/%31",
            "https://example.com/callback/notification/12/",
            "https://example.com/other/12",
            " https://example.com/callback/notification/12",
            "https://example.com/callback/notification/-1"
        ).forEach { assertFalse(it, Core.validEndpoint(it)) }
    }
}

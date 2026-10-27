package id.panelin.notifikasi

import org.junit.Assert.*
import org.junit.Test

class UiRulesTest {
    @Test fun endpointAndTokenValidationMatchBackendLimits() {
        assertNull(UiRules.endpointError("https://client.example/callback/notification/1"))
        assertNotNull(UiRules.endpointError("http://client.example/callback/notification/1"))
        assertNotNull(UiRules.tokenError("x".repeat(101)))
        assertNull(UiRules.tokenError("x".repeat(64)))
    }
}

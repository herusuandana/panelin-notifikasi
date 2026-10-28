package id.panelin.notifikasi

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QueueBehaviorTest {
    @Test fun pausedQueueSurvivesHistoryClearAndCredentialChangeIsHeld() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse("Run this fixture with emulator networking disabled", context.getSystemService(android.net.ConnectivityManager::class.java).activeNetworkInfo?.isConnected == true)
        val store = AppStore(context)
        val pkg = "id.panelin.queuefixture"
        val old = Destination(pkg, "Queue fixture", "https://client.example/callback/notification/1", "fixture-token", false)
        try {
            store.enabled = false
            store.saveDestination(old)
            assertFalse(store.enqueue(pkg, old.label, "Pembayaran Masuk", "Rp1.457 diterima DANA Bisnis.", "", "fixture-event"))
            // Keep network constrained; no fixture request is ever permitted to leave this test.
            store.saveDestination(old.copy(enabled = true))
            store.enabled = true
            assertTrue(store.enqueue(pkg, old.label, "Pembayaran Masuk", "Rp1.457 diterima DANA Bisnis.", "", "fixture-event"))
            store.enabled = false
            assertFalse(store.enqueue(pkg, old.label, "Pembayaran Masuk", "Rp1.457 diterima DANA Bisnis.", "", "fixture-event"))
            store.clearHistory()
            assertTrue(store.deliveries().any { it.packageName == pkg && it.state == "queued" })
            store.saveDestination(old.copy(token = "replacement-token"))
            assertTrue(store.deliveries().any { it.packageName == pkg && it.state == "held" })
            store.clearHistory()
            assertTrue(store.deliveries().any { it.packageName == pkg && it.state == "held" })
        } finally {
            store.enabled = false
            store.removeDestination(pkg)
        }
    }
}

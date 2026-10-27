package id.panelin.notifikasi

import android.app.NotificationManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatusNotificationTest {
    @Test fun statusNotificationDoesNotClaimConnectedWithoutListener() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = AppStore(context)
        val manager = context.getSystemService(NotificationManager::class.java)
        try {
            store.enabled = true
            Forwarding.refreshStatus(context)
            val notification = manager.activeNotifications.single { it.id == 701 }.notification
            assertEquals("Menunggu akses notifikasi", notification.extras.getCharSequence("android.title").toString())
        } finally {
            store.enabled = false
            Forwarding.refreshStatus(context)
        }
        // NotificationManager.cancel crosses Binder; wait for system-service acknowledgement.
        val deadline = android.os.SystemClock.elapsedRealtime() + 5_000
        while (manager.activeNotifications.any { it.id == 701 } && android.os.SystemClock.elapsedRealtime() < deadline) {
            android.os.SystemClock.sleep(50)
        }
        assertFalse("Paused status must disappear after cancellation", manager.activeNotifications.any { it.id == 701 })
    }
}

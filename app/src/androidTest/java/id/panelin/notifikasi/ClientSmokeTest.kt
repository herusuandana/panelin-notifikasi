package id.panelin.notifikasi

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ClientSmokeTest {
    @Test fun clientCanNavigateWithoutGrantingSensitivePermissions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        val device = UiDevice.getInstance(instrumentation)
        assertTrue("App must show navigation", device.wait(Until.hasObject(By.text("Beranda")), 60000))
        assertTrue("Dashboard must load", device.wait(Until.hasObject(By.text("Kendali notifikasi")), 30000))
        val headings = mapOf("Riwayat" to "Riwayat pengiriman", "Aplikasi" to "Aplikasi pilihan", "Pengaturan" to "Privasi jelas. Kendali tetap di perangkat Anda.", "Beranda" to "Kendali notifikasi")
        // ponytail: generous waits for software emulation; these remain real visible UI assertions.
        for (tab in listOf("Riwayat", "Aplikasi", "Pengaturan", "Beranda")) {
            val target = device.wait(Until.findObject(By.text(tab)), 5000)
            requireNotNull(target) { "Missing tab $tab" }.click()
            assertTrue("Selected page must render on $tab", device.wait(Until.hasObject(By.text(headings.getValue(tab))), 30000))
            device.waitForIdle()
            assertTrue("App remains visible on $tab", device.hasObject(By.pkg(context.packageName)))
            device.takeScreenshot(java.io.File(context.getExternalFilesDir(null), "smoke-$tab.png"))
        }
        assertFalse("No notification access is granted implicitly", Forwarding.listenerEnabled(context))
        assertTrue("No destination preconfigured", AppStore(context).destinations().isEmpty())
        assertTrue("No fabricated payment data", AppStore(context).deliveries().isEmpty())
    }
}

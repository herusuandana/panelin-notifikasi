package id.panelin.notifikasi

import android.content.Intent
import android.widget.TextView
import android.view.View
import android.view.ViewGroup
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupTest {
    @Test fun coldStartRendersDashboardWithoutNotificationPermission() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        try {
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                fun containsTitle(view: View): Boolean =
                    (view is TextView && view.text.toString() == "Kendali notifikasi") ||
                    (view is ViewGroup && (0 until view.childCount).any { containsTitle(view.getChildAt(it)) })
                assertTrue("Cold start must render the dashboard", containsTitle(activity.window.decorView))
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}

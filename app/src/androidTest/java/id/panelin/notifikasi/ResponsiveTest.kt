package id.panelin.notifikasi
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
class ResponsiveTest {
 @get:Rule val ui = createAndroidComposeRule<MainActivity>()
 @Test fun settingsBottomRemainsReachable() {
  ui.onNodeWithText("Pengaturan",useUnmergedTree=true).performClick()
  ui.onNode(hasScrollAction()).performScrollToNode(hasText("Bantuan penyiapan"))
  ui.onNodeWithText("Bantuan penyiapan").assertIsDisplayed().performClick()
  ui.onAllNodesWithText("Bantuan penyiapan",useUnmergedTree=true).onFirst().assertExists()
  val i=InstrumentationRegistry.getInstrumentation()
  i.runOnMainSync { assertTrue(ui.activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0) }
  ui.onAllNodesWithText("Mengerti").onFirst().performScrollTo().performClick()
 }
}

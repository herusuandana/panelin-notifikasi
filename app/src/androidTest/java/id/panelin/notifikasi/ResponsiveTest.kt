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
  ui.onNodeWithTag("page-content").performScrollToNode(hasText("Bantuan penyiapan"))
  ui.onNodeWithText("Bantuan penyiapan").assertIsDisplayed().performClick()
  ui.onAllNodesWithText("Bantuan penyiapan",useUnmergedTree=true).onFirst().assertExists()
  val i=InstrumentationRegistry.getInstrumentation()
  i.runOnMainSync { assertTrue(ui.activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0) }
  ui.onAllNodesWithText("Mengerti").onFirst().performScrollTo().assertIsDisplayed().performClick()
  val screen = ui.onRoot().fetchSemanticsNode().boundsInRoot
  for (tab in listOf("Beranda", "Riwayat", "Aplikasi", "Pengaturan")) {
   val node = ui.onNodeWithText(tab, useUnmergedTree=true)
   node.assertIsDisplayed().performClick()
   val bounds = node.fetchSemanticsNode().boundsInRoot
   assertTrue("Navigation clipped: $tab $bounds in $screen", bounds.left >= screen.left && bounds.right <= screen.right && bounds.top >= screen.top && bounds.bottom <= screen.bottom)
  }
 }
}

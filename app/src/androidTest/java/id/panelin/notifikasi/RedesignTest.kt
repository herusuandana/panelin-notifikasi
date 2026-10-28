package id.panelin.notifikasi
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue
class RedesignTest {
 @get:Rule val ui = createAndroidComposeRule<MainActivity>()
 @Test fun navigationUsesCustomFilterAndKeepsNoDestinationDefault() {
  ui.onNodeWithText("Riwayat").performClick()
  ui.onNodeWithText("Riwayat pengiriman").assertIsDisplayed()
  ui.onNodeWithTag("history-filter").performClick()
  ui.onNodeWithText("Perlu perhatian",substring=true).performClick()
  ui.onNodeWithTag("history-filter").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription,"Perlu perhatian"))
  ui.onNodeWithText("Aplikasi",useUnmergedTree=true).performClick()
  ui.onNodeWithText("Aplikasi terhubung").assertIsDisplayed()
  ui.onNodeWithText("Pengaturan",useUnmergedTree=true).performClick()
  ui.onNodeWithText("Pengaturan aplikasi").assertIsDisplayed()
  ui.onNodeWithText("Beranda").performClick()
  ui.onNodeWithText("Notifikasi pembayaran").assertIsDisplayed()
  assertTrue(AppStore(ui.activity).destinations().isEmpty())
 }
}

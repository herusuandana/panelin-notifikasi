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
  ui.onNodeWithContentDescription("history-filter").performClick()
  ui.onNodeWithText("Perlu perhatian",substring=true).performClick()
  ui.onNodeWithContentDescription("history-filter").assertTextContains("Perlu perhatian   ⌄")
  ui.onNodeWithText("Aplikasi",useUnmergedTree=true).performClick()
  ui.onNodeWithText("Aplikasi pilihan").assertIsDisplayed()
  ui.onNodeWithText("Pengaturan",useUnmergedTree=true).performClick()
  ui.onNodeWithText("Privasi jelas. Kendali tetap di perangkat Anda.").assertIsDisplayed()
  ui.onNodeWithText("Beranda").performClick()
  ui.onNodeWithText("Kendali notifikasi").assertIsDisplayed()
  assertTrue(AppStore(ui.activity).destinations().isEmpty())
 }
}

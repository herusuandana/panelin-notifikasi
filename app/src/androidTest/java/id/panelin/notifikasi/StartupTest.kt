package id.panelin.notifikasi
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
class StartupTest {
 @get:Rule val ui = createAndroidComposeRule<MainActivity>()
 @Test fun coldStartRendersDashboardWithoutNotificationPermission() {
  ui.onNodeWithText("Kendali notifikasi").assertIsDisplayed()
  ui.onNodeWithText("Beranda").assertIsSelected()
 }
}

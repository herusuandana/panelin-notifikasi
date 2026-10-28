package id.panelin.notifikasi

import android.view.View
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class EditorSecurityTest {
 @get:Rule val ui = createAndroidComposeRule<MainActivity>()
 @Test fun tokenIsMaskedAndConsentRequiredBeforeSaving() {
  // Reserved .invalid fixture; forwarding stays paused and no transport runs.
  val token = "compose-fixture-not-a-real-token"
  ui.runOnUiThread {
   ui.activity.getSharedPreferences("ui_privacy",0).edit().putBoolean("acknowledged_v1",true).commit()
   AppStore(ui.activity).saveDestination(Destination("id.panelin.fixture","Fixture","https://client.invalid/callback/notification/1",token,false))
  }
  ui.waitUntil(30000) { ui.onAllNodesWithText("Sebelum memulai").fetchSemanticsNodes().isEmpty() }
  ui.onNodeWithText("Aplikasi").performClick()
  ui.onNodeWithTag("page-content").performScrollToNode(hasTestTag("app-search"))
  ui.onNodeWithTag("app-search").assertIsDisplayed().performTextInput("id.panelin.fixture")
  ui.onNodeWithTag("page-content").performScrollToNode(hasText("Kelola tujuan"))
  ui.onNodeWithText("Kelola tujuan").performClick()
  ui.waitUntil(30000) {
   ui.onAllNodesWithTag("token").fetchSemanticsNodes().isNotEmpty() ||
    ui.onAllNodesWithText("Persetujuan akses notifikasi").fetchSemanticsNodes().isNotEmpty()
  }
  if (ui.onAllNodesWithText("Persetujuan akses notifikasi").fetchSemanticsNodes().isNotEmpty()) {
   ui.onNodeWithText("Saya mengerti").performScrollTo().performClick()
  }
  ui.waitUntil(30000) { ui.onAllNodesWithTag("token").fetchSemanticsNodes().isNotEmpty() }
  ui.onNodeWithTag("token").assert(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.Password))
  ui.onNodeWithTag("token").performClick() /* token must never become unmasked */
  ui.activityRule.scenario.onActivity { it.currentFocus?.clearFocus() }
  val rendered = ui.onNodeWithTag("token").fetchSemanticsNode().config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }
  assertFalse(rendered.any { it.text.contains(token) })
  ui.runOnUiThread {
   assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,ui.activity.window.decorView.importantForAutofill)
   assertTrue(ui.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
  }
  ui.onNodeWithTag("token").performTextReplacement("replacement-fixture-token")
  // Keep the keyboard open: scrolling must still reach consent and submit.
  ui.onNodeWithText("Saya berwenang",substring=true).performScrollTo().assertIsDisplayed()
  ui.onNodeWithText("Simpan").performScrollTo().assertIsDisplayed().performClick()
  ui.onNodeWithText("Setujui pengiriman data sebelum menyimpan.").performScrollTo().assertIsDisplayed()
  assertEquals(token,AppStore(ui.activity).destinations().single { it.packageName == "id.panelin.fixture" }.token)
  ui.onNodeWithText("Batal").performScrollTo().performClick()
  ui.activityRule.scenario.recreate()
  ui.onNodeWithText("Aplikasi").assertIsSelected()
  ui.onNodeWithTag("token").assertDoesNotExist()
  ui.onNodeWithTag("page-content").performScrollToNode(hasTestTag("app-search"))
  ui.onNodeWithTag("app-search").assertIsDisplayed()
  assertFalse(AppStore(ui.activity).enabled)
 }
}

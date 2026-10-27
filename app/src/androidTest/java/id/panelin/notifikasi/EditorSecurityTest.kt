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
  ui.waitUntil(30000) { ui.onAllNodesWithText("Privasi, sebelum memulai").fetchSemanticsNodes().isEmpty() }
  ui.onNodeWithText("Aplikasi").performClick()
  ui.onNodeWithContentDescription("app-search").performTextInput("id.panelin.fixture")
  ui.onNode(hasScrollAction()).performScrollToNode(hasText("Kelola tujuan →"))
  ui.onNodeWithText("Kelola tujuan →").performClick()
  ui.onNodeWithContentDescription("token").assert(SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsProperties.Password))
  ui.onNodeWithContentDescription("token").performClick() /* token must never become unmasked */
  ui.activityRule.scenario.onActivity { it.currentFocus?.clearFocus() }
  val rendered = ui.onNodeWithContentDescription("token").fetchSemanticsNode().config.getOrElse(androidx.compose.ui.semantics.SemanticsProperties.Text) { emptyList() }
  assertFalse(rendered.any { it.text.contains(token) })
  ui.runOnUiThread {
   assertEquals(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS,ui.activity.window.decorView.importantForAutofill)
   assertTrue(ui.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
  }
  ui.onNodeWithContentDescription("token").performTextReplacement("replacement-fixture-token")
  ui.onNodeWithText("Simpan").performScrollTo().performClick()
  ui.onNodeWithText("Persetujuan diperlukan sebelum menyimpan tujuan.").performScrollTo().assertIsDisplayed()
  assertEquals(token,AppStore(ui.activity).destinations().single { it.packageName == "id.panelin.fixture" }.token)
  ui.onNodeWithText("Batal").performScrollTo().performClick()
  ui.activityRule.scenario.recreate()
  ui.onNodeWithText("Aplikasi").assertIsSelected()
  ui.onNodeWithContentDescription("token").assertDoesNotExist()
  ui.onNodeWithContentDescription("app-search").assertExists()
  assertFalse(AppStore(ui.activity).enabled)
 }
}

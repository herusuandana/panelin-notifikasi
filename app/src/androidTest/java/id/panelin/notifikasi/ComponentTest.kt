package id.panelin.notifikasi
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
class ComponentTest {
 @get:Rule val ui = createComposeRule()
 @Test fun customControlsKeepCheckedSemantics() {
  ui.setContent { var checked by remember { mutableStateOf(false) }; ToggleRow("Penerusan",checked) { checked=it } }
  ui.onNodeWithText("Penerusan").assertIsOff().performClick().assertIsOn()
 }
 @Test fun disabledCheckboxDoesNotAcceptConsent() {
  ui.setContent { var checked by remember { mutableStateOf(false) }; CheckRow("Persetujuan",checked,false) { checked=it } }
  ui.onNodeWithText("Persetujuan").assertIsNotEnabled().performClick().assertIsOff()
 }
}

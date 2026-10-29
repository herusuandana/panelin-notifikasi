package id.panelin.notifikasi

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class CompactUiTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    private fun ready() {
        ui.waitUntil(30_000) { ui.onAllNodesWithTag("payment-overview").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun tab(label: String) {
        ui.onNode(hasText(label) and isSelectable()).assertIsDisplayed().performClick().assertIsSelected()
    }
    private fun role(value: Role) = SemanticsMatcher.expectValue(SemanticsProperties.Role,value)

    @Test fun compactHomeHasOneSummaryAndNoDuplicatedNavigationActions() {
        ready()
        val config=ui.activity.resources.configuration
        // First-screen density is a baseline-phone assertion, not a large-font/landscape ceiling.
        if(config.screenHeightDp>=600 && config.fontScale<=1.1f) {
            ui.onNodeWithTag("monitoring-status").assertIsDisplayed()
            ui.onNodeWithText("Diterima hari ini").assertIsDisplayed()
            ui.onNodeWithText("Terakhir dikirim").assertIsDisplayed()
            ui.onNode(hasText("Penerusan notifikasi") and role(Role.Switch)).assertIsDisplayed()
        }
        for(label in listOf("Dalam antrean","Perlu perhatian","Terakhir diterima","Terakhir dikirim")) {
            ui.onAllNodesWithText(label).assertCountEquals(1)
        }
        for(label in listOf("Kelola aplikasi","Lihat riwayat","Akses cepat","Akses notifikasi","Optimasi baterai")) {
            ui.onNodeWithText(label).assertDoesNotExist()
        }
        ui.onNodeWithTag("page-content").performScrollToNode(hasTestTag("quick-actions"))
        ui.onNode(hasText("Penerusan notifikasi") and role(Role.Switch)).assertHeightIsAtLeast(48.dp)
        for((label,heading) in listOf("Riwayat" to "Riwayat pengiriman","Aplikasi" to "Aplikasi terhubung","Pengaturan" to "Pengaturan aplikasi","Beranda" to "Notifikasi pembayaran")) {
            tab(label)
            ui.onNodeWithText(heading).assertIsDisplayed()
        }
        tab("Pengaturan")
        ui.onNodeWithTag("page-content").performScrollToNode(hasText("Bantuan penyiapan"))
        ui.onNodeWithText("Bantuan penyiapan").assertIsDisplayed()
    }

    @Test fun actualPullGestureRefreshesEveryPageIncludingShortHistory() {
        ready()
        for((page,label) in listOf("Beranda","Riwayat","Aplikasi","Pengaturan").withIndex()) {
            tab(label)
            val refresh=ui.onNodeWithTag("refresh-$page")
            repeat(3) {
                val before=refresh.fetchSemanticsNode().config[RefreshGeneration]
                ui.onNodeWithTag("page-content").performTouchInput {
                    swipe(Offset(center.x,40f),Offset(center.x,height*.9f),700)
                }
                ui.waitUntil(30_000) {
                    val state=ui.onNodeWithTag("refresh-$page").fetchSemanticsNode().config
                    state[RefreshGeneration] == before+1 && state[RefreshDistance] == 0f
                }
                ui.onNodeWithTag("initial-skeleton").assertDoesNotExist()
            }
        }
    }

    @Test fun filterExposesRadioSelectionAndPreservesItWhenReopened() {
        ready(); tab("Riwayat")
        ui.onNodeWithTag("history-filter").assertHeightIsAtLeast(48.dp).performClick()
        ui.onNode(hasText("Semua aktivitas") and role(Role.RadioButton)).assertIsSelected()
        ui.onNode(hasText("Perlu perhatian") and role(Role.RadioButton)).assertIsNotSelected().performClick()
        ui.onNodeWithTag("history-filter").assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Perlu perhatian")).performClick()
        ui.onNode(hasText("Perlu perhatian") and role(Role.RadioButton)).assertIsSelected()
        ui.onNode(hasText("Semua aktivitas") and role(Role.RadioButton)).assertIsNotSelected()
        ui.onAllNodesWithText("Tutup").assertCountEquals(1)
    }

    @Test fun installedNonLauncherPackageGetsRealIconAndMissingPackageGetsInitial() {
        val pkg=InstrumentationRegistry.getInstrumentation().context.packageName
        // The installed instrumentation APK has no launcher activity. No network fixture or destination.
        assertNotNull(ui.activity.packageManager.getApplicationIcon(pkg))
        ui.runOnUiThread { ui.activity.setContent { MaterialTheme { Column {
            AppIcon("id.panelin.missing.compact.fixture","Fallback")
            DeliveryDetails(Delivery(42,pkg,"Test","","","",1_700_000_000_000,"skipped","duplicate",0,"Tidak dikirim"))
        } } } }
        ui.waitUntil(30_000) {
            ui.onNodeWithTag("app-icon-$pkg").fetchSemanticsNode().config[SemanticsProperties.StateDescription]=="Ikon aplikasi" &&
                ui.onNodeWithTag("app-icon-id.panelin.missing.compact.fixture").fetchSemanticsNode().config[SemanticsProperties.StateDescription]=="Inisial aplikasi"
        }
        ui.onNodeWithText("F").assertIsDisplayed()
        ui.onNodeWithTag("app-icon-$pkg").assertIsDisplayed()
    }
}

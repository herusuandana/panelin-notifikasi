package id.panelin.notifikasi

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

class WalletDesignTest {
    @get:Rule val ui = createAndroidComposeRule<MainActivity>()

    @Test fun overviewShortcutsAndNavigationRemainReachable() {
        ui.onNodeWithTag("page-content").performScrollToNode(hasTestTag("payment-overview"))
        ui.onNodeWithTag("payment-overview").assertIsDisplayed()
        ui.onNodeWithTag("page-content").performScrollToNode(hasTestTag("quick-actions"))
        ui.onNodeWithTag("quick-actions").assertIsDisplayed()
        for (tab in listOf("Riwayat", "Aplikasi", "Pengaturan", "Beranda")) {
            ui.onNode(hasText(tab) and isSelectable()).assertIsDisplayed().performClick().assertIsSelected()
        }
        ui.onNodeWithText("Notifikasi pembayaran").assertIsDisplayed()
    }
}

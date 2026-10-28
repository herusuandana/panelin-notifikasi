package id.panelin.notifikasi

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TraceUiTest {
    @get:Rule val ui = createComposeRule()

    private fun delivery(state: String = "skipped", outcome: String = "duplicate", title: String = "") = Delivery(
        42, "id.example.payment", "Pembayaran", title, "", "", 1_700_000_000_000,
        state, outcome, 0, "Tidak dikirim ke website",
        trace = listOf(
            TraceStep(1_700_000_000_000, "Diterima di HP", "Sumber sudah disetujui"),
            TraceStep(1_700_000_000_100, "Dilewati di HP", "Duplikat lokal")
        )
    )

    @Test fun localSkipIsDistinctFromWebsiteIgnored() {
        assertEquals("Dilewati di HP", deliveryLabel(delivery()))
        assertEquals("Dilewati di HP", deliveryLabel(delivery(outcome = "ignored")))
        assertEquals("Diabaikan website", deliveryLabel(delivery(state = "done", outcome = "ignored")))
    }

    @Test fun emptyPayloadShowsTraceWithoutRevealButton() {
        ui.setContent { Column(Modifier.verticalScroll(rememberScrollState())) { DeliveryDetails(delivery()) } }
        ui.onNodeWithTag("delivery-trace").assertExists()
        ui.onNodeWithText("ID korelasi").assertExists()
        ui.onNodeWithText("42").assertExists()
        ui.onNodeWithText("Duplikat lokal").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Tampilkan isi notifikasi").assertDoesNotExist()
    }

    @Test fun existingPayloadStaysHiddenUntilRequestedAndOldTraceIsExplicit() {
        val d = delivery(state = "done", title = "Pembayaran diterima").copy(trace = emptyList())
        ui.setContent { Column(Modifier.verticalScroll(rememberScrollState())) { DeliveryDetails(d) } }
        ui.onNodeWithText("Pembayaran diterima").assertDoesNotExist()
        ui.onNodeWithText("Jejak belum tersedia atau catatan lama sudah dibersihkan.").performScrollTo().assertIsDisplayed()
        ui.onNodeWithText("Tampilkan isi notifikasi").performScrollTo().performClick()
        ui.onNodeWithText("Pembayaran diterima").performScrollTo().assertIsDisplayed()
    }
}

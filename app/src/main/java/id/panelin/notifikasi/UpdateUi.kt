package id.panelin.notifikasi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/** Place inside the Settings PanelCard. Does not make requests during composition. */
@Composable
internal fun UpdateSettings(controller: UpdateController) {
    val state by controller.state.collectAsState()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pembaruan aplikasi", style = MaterialTheme.typography.titleMedium)
        ToggleRow("Ikut versi uji coba", state.trials, !state.checking && !state.downloading && !state.ready && !state.prompt, controller::setTrials)
        Text(if (state.trials) "Versi uji coba bisa belum stabil." else "Hanya rilis stabil yang ditawarkan.", style = MaterialTheme.typography.bodySmall)
        Action(if (state.checking && state.manual) "Memeriksa…" else "Periksa pembaruan", false, enabled = !state.checking && !state.downloading && !state.prompt && !state.ready) { controller.check(true) }
        if (state.ready && !state.prompt) {
            Button(onClick = controller::download) { Text("Lanjutkan pemasangan") }
            TextButton(onClick = controller::dismiss) { Text("Batal") }
        }
        state.message?.takeIf { !state.prompt }?.let {
            Text(it, color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Place outside Scaffold so it is shared by all pages. */
@Composable
internal fun UpdatePrompt(controller: UpdateController) {
    val state by controller.state.collectAsState()
    val release = state.release ?: return
    if (!state.prompt) return
    Dialog(onDismissRequest = { if (!state.downloading) controller.dismiss() }) {
        Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), tonalElevation = 2.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Versi ${release.tag.drop(1)} tersedia", style = MaterialTheme.typography.titleMedium)
                if (release.prerelease) Text("Versi uji coba", color = MaterialTheme.colorScheme.error)
                if (release.notes.isNotBlank()) Text(release.notes, style = MaterialTheme.typography.bodyMedium)
                Text("Android akan meminta konfirmasi sebelum memasang.", style = MaterialTheme.typography.bodySmall)
                if (state.downloading) {
                    Text("Mengunduh ${(state.progress * 100).toInt()}%")
                    LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                    TextButton(onClick = controller::cancel, modifier = Modifier.fillMaxWidth()) { Text("Batalkan unduhan") }
                } else {
                    state.message?.let { Text(it, color = if (state.error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
                    Button(onClick = controller::download, modifier = Modifier.fillMaxWidth()) {
                        Text(if (state.ready) "Lanjutkan pemasangan" else if (state.error) "Coba lagi" else "Unduh dan pasang")
                    }
                    TextButton(onClick = controller::dismiss, modifier = Modifier.fillMaxWidth()) { Text("Nanti saja") }
                }
            }
        }
    }
}

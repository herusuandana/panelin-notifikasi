package id.panelin.notifikasi

import android.Manifest
import android.content.Intent
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.net.Uri
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.*
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.json.JSONArray
import java.text.DateFormat
import java.util.Date

private val Blue: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF73D9E0) else Color(0xFF006D7A)
private val Ink: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFEDF5F7) else Color(0xFF18343D)
private val Muted: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFACBFC6) else Color(0xFF566A73)
private val Canvas: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF101A20) else Color(0xFFF1F5F6)
private val Paper: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF1B2931) else Color.White
private val Line: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF354750) else Color(0xFFE1E9EC)
private val Green: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFF72D99C) else Color(0xFF237A43)
private val Amber: Color @Composable get() = if (isSystemInDarkTheme()) Color(0xFFFFC06B) else Color(0xFF965400)
private val Round = RoundedCornerShape(24.dp)
private data class SourceApp(val pkg: String, val label: String, val icon: android.graphics.Bitmap? = null)

class MainActivity : ComponentActivity() {
    private lateinit var store: AppStore
    private var destinations by mutableStateOf<List<Destination>>(emptyList())
    private var deliveries by mutableStateOf<List<Delivery>>(emptyList())
    private var apps by mutableStateOf<List<SourceApp>>(emptyList())
    private var snapshot by mutableStateOf<Snapshot?>(null)
    private var enabled by mutableStateOf(false)
    private var loaded by mutableStateOf(false)
    private var loadFailed by mutableStateOf(false)
    private var appsFailed by mutableStateOf(false)
    private var busy by mutableStateOf(false)
    private var acknowledged by mutableStateOf(false)
    private var listener by mutableStateOf(false to false)
    private var postAllowed by mutableStateOf(false)
    private var batteryFree by mutableStateOf(false)
    private var notice by mutableStateOf("")
    private var editing by mutableStateOf<SourceApp?>(null)
    private var detail by mutableStateOf<Delivery?>(null)
    private var info by mutableStateOf<Pair<String,String>?>(null)
    private var confirmation by mutableStateOf<Pair<String,()->Unit>?>(null)
    private var privacyAction: (() -> Unit)? = null
    private var privacyPrompt by mutableStateOf(false)
    private var reading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = AppStore(applicationContext)
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        window.decorView.setFilterTouchesWhenObscured(true)
        window.decorView.importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        window.decorView.isSaveEnabled = false
        window.decorView.isSaveFromParentEnabled = false
        setContent { PanelTheme { App() } }
        lifecycleScope.launch { loadApps() }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) { reload(); delay(6000) }
            }
        }
    }
    private suspend fun loadApps() {
        try {
            apps = withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)
                    .filter { it.activityInfo.packageName != packageName }.distinctBy { it.activityInfo.packageName }
                    .map { SourceApp(it.activityInfo.packageName,it.loadLabel(packageManager).toString(),runCatching { it.loadIcon(packageManager).toBitmap(96,96) }.getOrNull()) }
                    .sortedBy { it.label.lowercase() }
            }; appsFailed = false
        } catch (e: CancellationException) { throw e } catch (_: Exception) { appsFailed = true }
    }
    private suspend fun reload() {
        if (reading || busy) return
        reading = true
        try {
            val result = withContext(Dispatchers.IO) { Triple(store.destinations(),store.deliveries(150),store.snapshot()) }
            destinations = result.first; deliveries = result.second; snapshot = result.third
            enabled = store.enabled
            acknowledged = getSharedPreferences("ui_privacy",MODE_PRIVATE).getBoolean("acknowledged_v1",false)
            loaded = true; loadFailed = false
        } catch (e: CancellationException) { throw e } catch (_: Exception) { loaded = false; loadFailed = true; snapshot = null }
        finally { reading = false }
        listener = Forwarding.listenerEnabled(this) to Forwarding.listenerConnected(this)
        postAllowed = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        batteryFree = (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
    }
    private fun mutate(action: () -> Unit, after: () -> Unit = {}) {
        if (busy) return
        busy = true
        lifecycleScope.launch {
            try { withContext(Dispatchers.IO) { action() }; after() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { notice = "Belum tersimpan. Data tetap ada. Coba lagi." }
            finally { busy = false; reload() }
        }
    }
    private fun openSettings(intent: Intent) {
        try { startActivity(intent) }
        catch (_: ActivityNotFoundException) { notice = "Menu tidak tersedia. Buka pengaturan Android secara manual." }
        catch (_: SecurityException) { notice = "Android membatasi menu ini. Buka Info aplikasi secara manual." }
    }
    private fun privacyGate(action: () -> Unit) {
        if (acknowledged) action() else { privacyAction = action; privacyPrompt = true }
    }
    private fun acceptPrivacy() = mutate({ check(getSharedPreferences("ui_privacy",MODE_PRIVATE).edit().putBoolean("acknowledged_v1",true).commit()) }) {
        acknowledged = true; privacyPrompt = false; privacyAction?.invoke(); privacyAction = null
    }
    private fun changelog() { lifecycleScope.launch {
        val text = withContext(Dispatchers.IO) { runCatching {
            val all = JSONArray(assets.open("changelog.json").bufferedReader().use { it.readText() })
            (0 until all.length()).joinToString("\n\n") { i -> val row = all.getJSONObject(i); val changes = row.getJSONArray("changes")
                "${row.getString("version")} (${row.getInt("code")}) · ${row.getString("date")}\n" + (0 until changes.length()).joinToString("\n") { "• ${changes.getString(it)}" }
            }
        }.getOrDefault("Catatan perubahan belum tersedia. Versi ${BuildConfig.VERSION_NAME}.") }
        info = "Catatan perubahan" to text
    } }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable private fun App() {
        var tab by rememberSaveable { mutableIntStateOf(0) }
        var query by rememberSaveable { mutableStateOf("") }
        var filter by rememberSaveable { mutableIntStateOf(0) }
        var filterOpen by remember { mutableStateOf(false) }
        val historyFilters = listOf("Semua aktivitas","Dalam antrean","Perlu perhatian","Dikreditkan","Dilewati di HP")
        val labels = listOf("Beranda","Riwayat","Aplikasi","Pengaturan")
        val titles = listOf("Notifikasi pembayaran","Riwayat pengiriman","Aplikasi terhubung","Pengaturan aplikasi")
        val subtitles = listOf("Teruskan pembayaran ke website Anda.","Periksa hasil pengiriman ke website.","Pilih aplikasi dan atur tujuan pengiriman.","Izin perangkat, privasi, dan bantuan.")
        val config = LocalConfiguration.current
        val fontScale = LocalDensity.current.fontScale
        val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
        val wide = config.screenWidthDp >= 700 && (fontScale <= 1.4f || config.screenWidthDp >= 900)
        val snackbar = remember { SnackbarHostState() }
        LaunchedEffect(notice) { if(notice.isNotEmpty()) { snackbar.showSnackbar(notice); notice = "" } }
        val secure = editing != null || detail != null || info != null || privacyPrompt || confirmation != null
        DisposableEffect(secure) {
            if(secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            onDispose { }
        }
        Scaffold(containerColor = Canvas, snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = { if(!wide && !keyboardVisible) Surface(color=Paper,shadowElevation=1.dp) {
                val largeText = fontScale > 1.4f || config.screenWidthDp < 350
                Column(Modifier.navigationBarsPadding().padding(horizontal=8.dp,vertical=4.dp)) {
                    labels.indices.chunked(if(largeText) 2 else 4).forEach { group ->
                        Row { group.forEach { i -> NavItem(i,labels[i],tab==i,Modifier.weight(1f)) { tab=i } } }
                    }
                }
            } }
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))) {
                if(wide) Column(Modifier.width(200.dp).fillMaxHeight().background(Paper).verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.padding(vertical=20.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                        Image(painterResource(R.drawable.panelin_logo),null,Modifier.size(32.dp))
                        Text("Panelin",color=Ink,fontSize=20.sp,fontWeight=FontWeight.SemiBold)
                    }
                    labels.forEachIndexed { i,label -> NavItem(i,label,tab==i,Modifier.fillMaxWidth(),horizontal=true) { tab=i } }
                }
                Column(Modifier.weight(1f)) {
                    if(!wide && !keyboardVisible) Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.panelin_logo),null,Modifier.size(28.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Panelin",fontWeight=FontWeight.SemiBold,fontSize=18.sp,color=Ink,modifier=Modifier.weight(1f))
                    }
                    AnimatedContent(targetState=tab,transitionSpec={ fadeIn() togetherWith fadeOut() },label="page",modifier=Modifier.weight(1f)) { page ->
                        Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {
                            LazyColumn(Modifier.widthIn(max=760.dp).fillMaxWidth().imePadding().testTag("page-content"),contentPadding=PaddingValues(if(config.screenWidthDp<360) 16.dp else 24.dp,if(wide) 32.dp else 8.dp,if(config.screenWidthDp<360) 16.dp else 24.dp,24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                                item { Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Text(titles[page],fontSize=30.sp,lineHeight=36.sp,letterSpacing=(-.7).sp,fontWeight=FontWeight.Bold,color=Ink,modifier=Modifier.semantics { heading() })
                                    Text(subtitles[page],color=Muted,fontSize=14.sp)
                                } }
                                if(loadFailed) item { PanelCard { Badge("Penyimpanan belum terbaca",Amber); Copy("Status belum dapat dipastikan. Data tidak dihapus."); Action("Coba lagi") { lifecycleScope.launch { reload() } } } }
                                when(page) {
                                    0 -> {
                                        item { val ready=loaded && enabled && listener.first && listener.second && destinations.any { it.enabled }
                                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFF006975),Color(0xFF0753A0)))).testTag("payment-overview").padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                                                Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                                    Glyph(6,Color.White)
                                                    Text(if(ready) "Aktif" else if(loadFailed) "Status belum tersedia" else if(!loaded) "Memuat status" else "Perlu penyiapan",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha=.18f)).padding(12.dp,8.dp))
                                                }
                                                Column(verticalArrangement=Arrangement.spacedBy(2.dp)) {
                                                    Text("Diterima hari ini",color=Color.White,fontSize=14.sp)
                                                    Text(snapshot?.todayCount?.toString()?:"—",color=Color.White,fontSize=56.sp,lineHeight=64.sp,fontWeight=FontWeight.Bold,letterSpacing=(-2).sp)
                                                    Text("Notifikasi dari sumber pilihan",color=Color.White,fontSize=13.sp,lineHeight=19.sp)
                                                }
                                                HorizontalDivider(color=Color.White.copy(alpha=.22f))
                                                Column(verticalArrangement=Arrangement.spacedBy(6.dp)) {
                                                    Text(when { !loaded -> "Status belum tersedia"; !enabled -> "Penerusan dijeda"; !listener.first -> "Izinkan akses notifikasi"; !listener.second -> "Menghubungkan ke Android"; !ready -> "Pilih aplikasi pembayaran"; else -> "Siap menerima notifikasi" },color=Color.White,fontSize=19.sp,lineHeight=26.sp,fontWeight=FontWeight.SemiBold)
                                                    Text(if(!loaded) "Ringkasan belum dapat dipastikan." else "${destinations.count { it.enabled }} aplikasi aktif dari ${destinations.size} terhubung",color=Color.White,fontSize=13.sp,lineHeight=20.sp)
                                                    Text("Jumlah notifikasi, bukan saldo. Hasil pengiriman dan kredit saldo ada di Riwayat.",color=Color.White,fontSize=12.sp,lineHeight=18.sp)
                                                }
                                            }
                                        }
                                        item { PanelCard {
                                            Section("Akses cepat")
                                            val compact=config.screenWidthDp<360 || fontScale>1.4f
                                            Column(Modifier.testTag("quick-actions"),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                                listOf(0,1,2,3).chunked(if(compact) 2 else 4).forEach { group ->
                                                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                                        group.forEach { i ->
                                                            Shortcut(listOf(2,1,6,3)[i],listOf("Kelola aplikasi","Lihat riwayat","Akses notifikasi","Atur perangkat")[i],Modifier.weight(1f)) {
                                                                when(i) { 0->tab=2; 1->tab=1; 2->privacyGate { openSettings(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }; else->tab=3 }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } }
                                        item { PanelCard {
                                            ToggleRow("Penerusan notifikasi",enabled,loaded && !busy) { value ->
                                                if(value && !acknowledged) privacyGate { }
                                                else if(value && destinations.none { it.enabled }) { tab=2; notice="Hubungkan dan aktifkan satu aplikasi terlebih dahulu." }
                                                else mutate({ store.enabled=value; if(value) Forwarding.reconnect(applicationContext) })
                                            }
                                        } }
                                        if(!acknowledged) item { PanelCard { Section("Sebelum memulai"); Copy("Izin Android mencakup semua notifikasi. Panelin hanya meneruskan sumber yang Anda pilih ke website Anda. Isinya bisa memuat data pribadi."); Action("Baca privasi",false) { info="Privasi" to HelpText.Privacy }; Action("Saya mengerti") { acceptPrivacy() } } }
                                        item { val compact=config.screenWidthDp<360 || fontScale>1.4f
                                            if(compact) Column(verticalArrangement=Arrangement.spacedBy(12.dp)) { Metric("Dalam antrean",snapshot?.queuedCount); Metric("Kredit tercatat",snapshot?.creditedCount) }
                                            else Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { Box(Modifier.weight(1f)) { Metric("Dalam antrean",snapshot?.queuedCount) }; Box(Modifier.weight(1f)) { Metric("Kredit tercatat",snapshot?.creditedCount) } }
                                        }
                                        item { PanelCard { Section("Aktivitas perangkat"); KeyValue("Dalam antrean",snapshot?.queuedCount?.toString()?:"—"); KeyValue("Perlu perhatian",snapshot?.attentionCount?.toString()?:"—"); KeyValue("Terakhir diterima",time(snapshot?.lastReceivedAt?:0)); KeyValue("Terakhir dikirim",time(snapshot?.lastSentAt?:0)); Copy("Kredit tercatat dihitung dari riwayat tersimpan, bukan saldo website. Status Dikreditkan berarti website mengonfirmasi kredit saldo.") } }
                                        item { PanelCard { Section("Persiapan perangkat")
                                            SettingRow(6,"Akses notifikasi",if(listener.first) "Diizinkan Android" else "Wajib untuk membaca sumber pilihan") { privacyGate { openSettings(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } }
                                            SettingRow(7,"Notifikasi status",if(postAllowed) "Diizinkan" else "Izinkan penanda status aplikasi") { if(Build.VERSION.SDK_INT>=33 && !postAllowed) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS),41) else openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,packageName)) }
                                            SettingRow(3,"Pengaturan baterai",if(batteryFree) "Dikecualikan dari optimasi" else "Tinjau pembatasan latar belakang") { openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                                            Action("Sambungkan ulang",false) { Forwarding.reconnect(applicationContext); notice="Meminta Android menyambungkan ulang. Periksa status di Beranda." }
                                        } }
                                        item { Action("Kelola aplikasi") { tab=2 } }
                                    }
                                    1 -> {
                                        item { Action(historyFilters[filter]+"   ⌄",false,tag="history-filter") { filterOpen=true } }
                                        val selected=deliveries.filter { when(filter) { 1->queued(it); 2->attention(it); 3->it.outcome=="credited"; 4->it.state=="skipped"; else->true } }
                                        if(selected.isEmpty()) item { Empty("Belum ada aktivitas","Notifikasi dari aplikasi aktif akan muncul di sini.",1) }
                                        items(selected,key={it.id}) { d -> PanelCard { Row(verticalAlignment=Alignment.CenterVertically) { Glyph(1,Blue); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Section(d.appLabel.ifBlank { d.packageName }); Text(time(d.createdAt),color=Muted,fontSize=12.sp) } }; Badge(deliveryLabel(d),if(d.outcome=="credited") Green else if(attention(d)) Amber else Blue); Copy(if(d.state=="skipped") "Tidak dikirim ke website" else "${d.attempts} percobaan · Isi notifikasi disembunyikan"); Action("Lihat detail",false) { detail=d } } }
                                        if(deliveries.size>=150) item { Copy("Menampilkan hingga 150 pembayaran dan 150 aktivitas lokal.") }
                                        item { Action("Muat ulang",false) { lifecycleScope.launch { reload() } } }
                                    }
                                    2 -> {
                                        item { PanelCard { Badge("${destinations.count { it.enabled }} aktif · ${destinations.size} terhubung",Blue); Copy("Atur URL dan token untuk setiap aplikasi pembayaran.") } }
                                        item { Field(query,{query=it},"Cari nama aplikasi atau paket",tag="app-search") }
                                        if(appsFailed) item { PanelCard { Copy("Daftar aplikasi belum dapat dibaca."); Action("Coba muat aplikasi",false) { lifecycleScope.launch { loadApps() } } } }
                                        val merged=(apps+destinations.filter { d->apps.none { it.pkg==d.packageName } }.map { SourceApp(it.packageName,it.label) }).sortedWith(compareByDescending<SourceApp> { a->destinations.any { it.packageName==a.pkg } }.thenBy { it.label.lowercase() }).filter { it.label.contains(query,true)||it.pkg.contains(query,true) }
                                        if(merged.isEmpty() && !appsFailed) item { Empty("Tidak ada aplikasi ditemukan","Coba nama lain. Daftar mengikuti aplikasi yang bisa ditampilkan Android.",2) }
                                        items(merged,key={it.pkg}) { app -> val configured=destinations.find { it.packageName==app.pkg }
                                            PanelCard { Row(verticalAlignment=Alignment.CenterVertically) { if(app.icon!=null) Image(app.icon.asImageBitmap(),null,Modifier.size(46.dp).clip(RoundedCornerShape(14.dp))) else Glyph(2,Blue,Modifier.size(44.dp)); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Section(app.label); Text(app.pkg,color=Muted,fontSize=11.sp) } }; Badge(if(configured==null) "Belum diatur" else if(configured.enabled) "Aktif" else "Dijeda",if(configured?.enabled==true) Green else Muted); Action(if(configured==null) "Hubungkan aplikasi" else "Kelola tujuan",false) { privacyGate { editing=app } } }
                                        }
                                    }
                                    3 -> {
                                        item { PanelCard { Row(verticalAlignment=Alignment.CenterVertically) { Image(painterResource(R.drawable.panelin_logo),null,Modifier.size(48.dp).padding(6.dp)); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Section("Panelin Notifikasi"); Copy("Versi ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})") } }; Copy("Notifikasi langsung ke website Anda, tanpa server perantara atau pelacakan.") } }
                                        item { Section("Privasi dan data") }
                                        item { PanelCard { SettingRow(6,"Privasi","Akses, tujuan, dan batas pemrosesan") { info="Privasi" to HelpText.Privacy }; SettingRow(1,"Hapus riwayat selesai","Antrean tertunda tetap tersimpan",true) { confirmation="Hapus riwayat selesai di HP ini? Antrean tertunda dan data website tetap ada." to { mutate({store.clearHistory()}) { notice="Riwayat selesai dihapus. Antrean tetap ada." } } } } }
                                        item { Section("Perangkat") }
                                        item { PanelCard { SettingRow(7,"Optimasi baterai",if(batteryFree) "Dikecualikan Android" else "Tinjau pembatasan baterai") { openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }; SettingRow(3,"Aktivitas latar belakang","Panduan sesuai merek HP") { info="Aktivitas latar belakang" to HelpText.Oem }; SettingRow(2,"Info aplikasi di Android","Izin, baterai, dan penyimpanan") { openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName"))) } } }
                                        item { Section("Bantuan") }
                                        item { PanelCard { SettingRow(1,"Catatan pembaruan","Lihat perubahan tiap versi") { changelog() }; SettingRow(6,"Bantuan penyiapan","Hubungkan sumber ke website Anda") { info="Bantuan penyiapan" to HelpText.Setup } } }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if(filterOpen) Sheet("Filter aktivitas",{filterOpen=false}) {
            historyFilters.forEachIndexed { i,value -> Action(value+(if(i==filter) "  ✓" else ""),i==filter) { filter=i;filterOpen=false } }
        }
        if(privacyPrompt) Sheet("Persetujuan akses notifikasi",{privacyPrompt=false;privacyAction=null}) {
            Copy("Izin Android mencakup semua notifikasi. Panelin hanya membaca sumber yang Anda pilih dan aktifkan. Nama paket, judul, teks, dan teks panjang dikirim ke URL HTTPS Anda. Data ini bisa bersifat pribadi. Setiap tujuan memerlukan persetujuan. Tidak ada tujuan bawaan atau pengiriman uji.")
            Action("Saya mengerti",enabled=!busy) { acceptPrivacy() }; Action("Batal",false) { privacyPrompt=false;privacyAction=null }
        }
        editing?.let { app -> Editor(app,destinations.find { it.packageName==app.pkg }) }
        detail?.let { selected ->
            val d = deliveries.find { it.id==selected.id } ?: selected
            Sheet(d.appLabel.ifBlank { "Detail aktivitas" },{detail=null}) {
                DeliveryDetails(d)
                Action("Tutup") { detail=null }
            }
        }
        info?.let { (title,text) -> Sheet(title,{info=null}) { Text(text,color=Ink,fontSize=15.sp,lineHeight=23.sp); Action("Mengerti") { info=null } } }
        confirmation?.let { (text,action) -> Sheet("Konfirmasi penghapusan",{if(!busy)confirmation=null},canDismiss=!busy) { Copy(text); Action("Hapus",enabled=!busy) { action();confirmation=null }; Action("Batal",false,enabled=!busy) { confirmation=null } } }
    }

    @Composable private fun Editor(app: SourceApp,existing: Destination?) {
        // Deliberately remember, never rememberSaveable: no endpoint/token/consent in saved state.
        var endpoint by remember(app.pkg) { mutableStateOf(existing?.endpoint.orEmpty()) }
        var token by remember(app.pkg) { mutableStateOf(existing?.token.orEmpty()) }
        var active by remember(app.pkg) { mutableStateOf(existing?.enabled?:true) }
        var consent by remember(app.pkg) { mutableStateOf(false) }
        var validation by remember(app.pkg) { mutableStateOf("") }
        fun valid(): Boolean {
            val error=UiRules.endpointError(endpoint.trim())?:UiRules.tokenError(token)
            validation=error?:"Format sesuai. Koneksi dan token belum diuji. Tidak ada data dikirim."
            return error==null
        }
        Sheet(app.label,{if(!busy) { token="";editing=null }},canDismiss=!busy) {
            Copy(app.pkg)
            Field(endpoint,{endpoint=it;validation=""},"URL webhook",keyboard=KeyboardType.Uri,tag="endpoint")
            Field(token,{token=it;validation=""},"Token webhook",secret=true,tag="token")
            Copy("Salin URL dan token dari metode pembayaran di website. Jangan gunakan PIN atau OTP dompet.")
            ToggleRow("Aktifkan aplikasi ini",active,!busy) { active=it }
            CheckRow("Saya berwenang memakai tujuan ini dan mengizinkan nama paket, judul, teks, serta teks panjang dikirim ke website tersebut.",consent,!busy) {consent=it}
            Copy("Mengganti URL atau token menahan antrean lama. Jeda tidak menghapus antrean. Pengiriman yang sudah berjalan tidak bisa ditarik.")
            AnimatedVisibility(validation.isNotEmpty()) { Text(validation,color=Amber,fontSize=13.sp,modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) }
            Action("Periksa format",false,enabled=!busy) { valid() }
            Action(if(busy) "Menyimpan…" else "Simpan",enabled=!busy) {
                if(valid()) {
                    if(!consent) validation="Setujui pengiriman data sebelum menyimpan."
                    else { val value=Destination(app.pkg,app.label,endpoint.trim(),token,active); mutate({store.saveDestination(value)}) { token="";editing=null;notice="Tujuan tersimpan." } }
                }
            }
            Action("Batal",false,enabled=!busy) { token="";editing=null }
            if(existing!=null) Action("Hapus konfigurasi aplikasi",false,enabled=!busy) {
                confirmation="Hapus tujuan ini? Notifikasi baru tidak diteruskan. Antrean lama ditahan. Pengiriman yang sudah berjalan tidak bisa ditarik." to {
                    mutate({store.removeDestination(app.pkg)}) { token="";editing=null }
                }
            }
        }
    }
}

@Composable private fun PanelTheme(content: @Composable ()->Unit) {
    val scheme = if(isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    MaterialTheme(colorScheme=scheme.copy(primary=Blue,onPrimary=if(isSystemInDarkTheme()) Color.Black else Color.White,surface=Paper,onSurface=Ink,background=Canvas,onBackground=Ink,outline=Line),content=content)
}
@Composable internal fun DeliveryDetails(d: Delivery) {
    var reveal by remember(d.id) { mutableStateOf(false) }
    Badge(deliveryLabel(d),Blue)
    Copy("${d.packageName}\n${time(d.createdAt)}\n${d.attempts} percobaan")
    KeyValue("ID korelasi",d.id.toString())
    Copy(d.detail)
    if(d.state!="skipped") Copy("Hanya status Dikreditkan yang mengonfirmasi kredit saldo.")
    PanelCard {
        Section("Jejak aktivitas")
        Column(Modifier.fillMaxWidth().testTag("delivery-trace"),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            if(d.trace.isEmpty()) Copy("Jejak belum tersedia atau catatan lama sudah dibersihkan.")
            d.trace.forEachIndexed { index,step ->
                Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Copy(if(step.time<=0) "Waktu belum tersedia" else DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.MEDIUM).format(Date(step.time)))
                    Text("${index+1}. ${step.stage}",color=Ink,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                    if(step.detail.isNotBlank()) Copy(step.detail)
                }
            }
        }
    }
    val payload = listOf(d.title,d.text,d.bigText).filter { it.isNotBlank() }.distinct()
    if(payload.isNotEmpty()) {
        if(reveal) Text(payload.joinToString("\n\n"),color=Ink)
        else Action("Tampilkan isi notifikasi",false) { reveal=true }
    }
}
@Composable internal fun Action(text: String,primary: Boolean=true,enabled: Boolean=true,tag: String="",click: ()->Unit) {
    val bg by animateColorAsState(if(primary) Blue else Canvas,label="button")
    Surface(onClick=click,enabled=enabled,shape=RoundedCornerShape(12.dp),color=if(enabled) bg else Line,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp).testTag(tag).semantics { role=Role.Button }) {
        Text(text,color=if(!enabled) Muted else if(primary) (if(isSystemInDarkTheme()) Color.Black else Color.White) else Blue,fontWeight=FontWeight.SemiBold,fontSize=14.sp,modifier=Modifier.padding(16.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
    }
}
@Composable internal fun PanelCard(content: @Composable ColumnScope.()->Unit) {
    Surface(shape=Round,color=Paper,modifier=Modifier.fillMaxWidth().animateContentSize()) { Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp),content=content) }
}
@Composable private fun Section(text: String) { Text(text,color=Ink,fontSize=17.sp,lineHeight=24.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.semantics { heading() }) }
@Composable private fun Copy(text: String) { Text(text,color=Muted,fontSize=14.sp,lineHeight=21.sp) }
@Composable private fun Badge(text: String,color: Color) { Text(text,color=color,fontSize=12.sp,fontWeight=FontWeight.Medium,modifier=Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha=.08f)).padding(10.dp,7.dp)) }
@Composable private fun Metric(label: String,value: Int?) { PanelCard { Copy(label); Text(value?.toString()?:"—",fontSize=34.sp,color=Ink,fontWeight=FontWeight.Bold) } }
@Composable private fun KeyValue(key: String,value: String) { Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { Text(key,color=Muted,fontSize=13.sp,modifier=Modifier.weight(1f)); Text(value,color=Ink,fontSize=13.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f),textAlign=androidx.compose.ui.text.style.TextAlign.End) } }
@Composable internal fun ToggleRow(title: String,checked: Boolean,enabled: Boolean=true,change: (Boolean)->Unit) {
    val position by animateFloatAsState(if(checked) 1f else 0f,label="toggle")
    val color by animateColorAsState(if(checked) (if(isSystemInDarkTheme()) Color(0xFF30A45A) else Color(0xFF278546)) else Line,label="track")
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Canvas).toggleableCompat(checked,enabled,Role.Switch,change).padding(14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Text(title,color=if(enabled) Ink else Muted,fontSize=14.sp,fontWeight=FontWeight.Medium,modifier=Modifier.weight(1f))
        Box(Modifier.size(51.dp,31.dp).clip(RoundedCornerShape(50)).background(if(enabled) color else Line)) {
            Box(Modifier.padding(3.dp).offset(x=(20*position).dp).size(25.dp).background(Color.White,RoundedCornerShape(50)))
        }
    }
}
private fun Modifier.toggleableCompat(checked: Boolean,enabled: Boolean,role: Role,change: (Boolean)->Unit): Modifier = this.then(Modifier.toggleable(checked,enabled=enabled,role=role,onValueChange=change))
@Composable internal fun CheckRow(title: String,checked: Boolean,enabled: Boolean=true,change: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Canvas).toggleableCompat(checked,enabled,Role.Checkbox,change).padding(14.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(if(checked) Blue else Paper).border(1.dp,if(checked) Blue else Muted,RoundedCornerShape(7.dp)),contentAlignment=Alignment.Center) { if(checked) Glyph(8,MaterialTheme.colorScheme.onPrimary,Modifier.size(17.dp)) }
        Text(title,color=Ink,fontSize=13.sp,lineHeight=20.sp,modifier=Modifier.weight(1f))
    }
}
@Composable private fun Field(value: String,change: (String)->Unit,label: String,secret: Boolean=false,keyboard: KeyboardType=KeyboardType.Text,tag: String="") {
    OutlinedTextField(value,onValueChange=change,label={Text(label,fontSize=14.sp)},textStyle=androidx.compose.ui.text.TextStyle(fontSize=16.sp,color=Ink),singleLine=true,shape=RoundedCornerShape(16.dp),visualTransformation=if(secret) PasswordVisualTransformation() else VisualTransformation.None,keyboardOptions=KeyboardOptions(keyboardType=if(secret) KeyboardType.Password else keyboard,autoCorrectEnabled=false),modifier=Modifier.fillMaxWidth().testTag(tag),colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Line,focusedBorderColor=Blue,unfocusedContainerColor=Paper,focusedContainerColor=Paper,unfocusedTextColor=Ink,focusedTextColor=Ink,unfocusedLabelColor=Muted,focusedLabelColor=Blue))
}
@Composable private fun SettingRow(icon: Int,title: String,detail: String,danger: Boolean=false,click: ()->Unit) {
    Surface(onClick=click,color=Color.Transparent,shape=RoundedCornerShape(14.dp),modifier=Modifier.semantics { role=Role.Button }) {
        Row(Modifier.fillMaxWidth().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background((if(danger) Amber else Blue).copy(alpha=.07f)),contentAlignment=Alignment.Center) { Glyph(icon,if(danger) Amber else Blue) }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) { Text(title,color=if(danger) Amber else Ink,fontSize=14.sp,fontWeight=FontWeight.Medium); Copy(detail) }; Glyph(4,Muted,Modifier.size(16.dp))
        }
    }
}
@Composable private fun Empty(title: String,description: String,icon: Int) { PanelCard { Column(Modifier.fillMaxWidth().padding(vertical=18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(16.dp)) { Box(Modifier.size(58.dp).clip(RoundedCornerShape(18.dp)).background(Blue.copy(alpha=.07f)),contentAlignment=Alignment.Center) { Glyph(icon,Blue) }; Section(title); Text(description,color=Muted,fontSize=13.sp,lineHeight=21.sp,textAlign=androidx.compose.ui.text.style.TextAlign.Center) } } }
@Composable private fun Shortcut(icon: Int,title: String,modifier: Modifier,click: ()->Unit) {
    val tint=when(icon) { 1->if(isSystemInDarkTheme()) Color(0xFFADBBFF) else Color(0xFF505CB2); 6->Green; 3->Amber; else->Blue }
    Surface(onClick=click,color=Color.Transparent,shape=RoundedCornerShape(16.dp),modifier=modifier.heightIn(min=48.dp).semantics { role=Role.Button }) {
        Column(Modifier.padding(vertical=8.dp,horizontal=2.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(tint.copy(alpha=.12f)),contentAlignment=Alignment.Center) { Glyph(icon,tint) }
            Text(title,color=Ink,fontSize=12.sp,lineHeight=17.sp,fontWeight=FontWeight.Medium,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
@Composable private fun NavItem(index: Int,title: String,selected: Boolean,modifier: Modifier,horizontal: Boolean=false,click: ()->Unit) {
    val bg by animateColorAsState(if(selected) Blue.copy(alpha=.14f) else Color.Transparent,label="nav")
    val style = modifier.clip(RoundedCornerShape(16.dp)).background(if(horizontal) bg else Color.Transparent).selectableCompat(selected,click).heightIn(min=48.dp).padding(8.dp,8.dp)
    val color = if(selected) Blue else Muted
    if(horizontal) Row(style,verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Glyph(index,color)
        Text(title,color=color,fontSize=14.sp,fontWeight=if(selected) FontWeight.SemiBold else FontWeight.Normal,modifier=Modifier.weight(1f))
    } else Column(style,horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Box(Modifier.width(56.dp).height(32.dp).clip(RoundedCornerShape(50)).background(bg),contentAlignment=Alignment.Center) { Glyph(index,color) }
        Text(title,color=if(selected) Ink else Muted,fontSize=11.sp,lineHeight=15.sp,fontWeight=if(selected) FontWeight.SemiBold else FontWeight.Normal,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
    }
}
private fun Modifier.selectableCompat(selected: Boolean,click: ()->Unit) = this.then(Modifier.selectable(selected,role=Role.Tab,onClick=click))
@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun Sheet(title: String,dismiss: ()->Unit,canDismiss: Boolean=true,content: @Composable ColumnScope.()->Unit) {
    ModalBottomSheet(onDismissRequest=dismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true,confirmValueChange={ it != SheetValue.Hidden || canDismiss }),containerColor=Paper,shape=RoundedCornerShape(topStart=24.dp,topEnd=24.dp),sheetMaxWidth=560.dp,properties=ModalBottomSheetProperties(securePolicy=androidx.compose.ui.window.SecureFlagPolicy.SecureOn)) {
        val view = androidx.compose.ui.platform.LocalView.current
        DisposableEffect(view) {
            view.rootView.importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            view.rootView.setFilterTouchesWhenObscured(true)
            view.isSaveEnabled = false
            onDispose { }
        }
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(22.dp,0.dp,22.dp,28.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) { Text(title,color=Ink,fontSize=22.sp,lineHeight=28.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f).semantics { heading() }); TextButton(onClick=dismiss,enabled=canDismiss) { Text("Tutup",color=Blue) } }
            content()
        }
    }
}
@Composable private fun Glyph(kind: Int,color: Color,modifier: Modifier=Modifier.size(24.dp)) {
    val vector=remember(kind) { ImageVector.Builder("panelin-$kind",24.dp,24.dp,24f,24f).apply { path(stroke=androidx.compose.ui.graphics.SolidColor(Color.Black),strokeLineWidth=1.8f,strokeLineCap=androidx.compose.ui.graphics.StrokeCap.Round,strokeLineJoin=androidx.compose.ui.graphics.StrokeJoin.Round) {
        when(kind) {
            0->{moveTo(3f,10f);lineTo(12f,3f);lineTo(21f,10f);moveTo(5f,9f);lineTo(5f,21f);lineTo(10f,21f);lineTo(10f,14f);lineTo(14f,14f);lineTo(14f,21f);lineTo(19f,21f);lineTo(19f,9f)}
            1->{moveTo(21f,12f);arcTo(9f,9f,0f,true,true,3f,12f);arcTo(9f,9f,0f,true,true,21f,12f);moveTo(12f,7f);lineTo(12f,12f);lineTo(16f,14f)}
            2->{for(x in listOf(4f,14f))for(y in listOf(4f,14f)){moveTo(x,y);lineTo(x+6,y);lineTo(x+6,y+6);lineTo(x,y+6);close()}}
            3->{for(y in listOf(6f,12f,18f)){moveTo(3f,y);lineTo(21f,y)};moveTo(8f,3f);lineTo(8f,9f);moveTo(16f,9f);lineTo(16f,15f);moveTo(9f,15f);lineTo(9f,21f)}
            4->{moveTo(9f,6f);lineTo(15f,12f);lineTo(9f,18f)}
            6->{moveTo(12f,2f);lineTo(21f,6f);lineTo(20f,14f);lineTo(12f,22f);lineTo(4f,14f);lineTo(3f,6f);close();moveTo(8f,12f);lineTo(11f,15f);lineTo(16f,9f)}
            8->{moveTo(4f,12f);lineTo(10f,18f);lineTo(20f,5f)}
            else->{moveTo(6f,3f);lineTo(18f,3f);lineTo(18f,21f);lineTo(6f,21f);close();moveTo(10f,17f);lineTo(14f,17f)}
        }
    } }.build() }
    Icon(vector,null,modifier,tint=color)
}
private fun queued(d: Delivery)=d.state.lowercase() in setOf("queued","pending","retry","retrying","sending","processing")
private fun attention(d: Delivery)=d.state.lowercase() in setOf("attention","held","failed","error") || d.outcome in setOf("review","ambiguous","unmatched")
internal fun deliveryLabel(d: Delivery)=when { d.state=="skipped"->"Dilewati di HP"; d.state=="held"->"Ditahan · tujuan berubah"; d.outcome in setOf("credited","ignored","unmatched","ambiguous","review","duplicate")->UiRules.outcomeLabel(d.outcome); queued(d)->"Dalam antrean";attention(d)->"Perlu perhatian";d.state in setOf("done","sent","delivered","success")->"Terkirim · hasil belum diketahui";else->"Tercatat" }
private fun time(value: Long)=if(value<=0) "Belum ada" else DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(Date(value))

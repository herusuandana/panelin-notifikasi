package id.panelin.notifikasi

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.widget.*
import org.json.JSONArray
import java.net.URI
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.Executors

// UI_RULES_START
internal object UiRules {
    fun endpointError(value: String): String? {
        val uri = try { URI(value) } catch (_: Exception) { return "Alamat HTTPS tidak valid." }
        return if (uri.scheme != "https" || uri.host.isNullOrBlank() || uri.rawUserInfo != null ||
            uri.rawQuery != null || uri.rawFragment != null || uri.port !in -1..65535 || uri.port == 0 ||
            !Regex("/callback/notification/[0-9]+").matches(uri.rawPath ?: ""))
            "Gunakan HTTPS dengan path /callback/notification/ID angka, tanpa kredensial, query, atau fragmen."
        else null
    }
    fun tokenError(value: String): String? = when {
        value.isBlank() -> "Token wajib diisi."
        value.length > 100 -> "Token maksimal 100 karakter sesuai penerima Panelin."
        value.any { it.isWhitespace() || it.isISOControl() } -> "Token tidak boleh berisi spasi atau karakter kontrol."
        else -> null
    }
    fun outcomeLabel(value: String): String = when (value) {
        "credited" -> "Dikreditkan"
        "ignored" -> "Diabaikan"
        "unmatched" -> "Tidak cocok"
        "ambiguous" -> "Ambigu"
        "review" -> "Perlu tinjauan"
        "duplicate" -> "Duplikat"
        else -> "Belum ada hasil"
    }
}
// UI_RULES_END

class MainActivity : Activity() {
    private val ink = Color.rgb(20, 33, 61)
    private val muted = Color.rgb(100, 116, 139)
    private val blue = Color.rgb(37, 99, 235)
    private val background = Color.rgb(245, 247, 250)
    private val green = Color.rgb(4, 120, 87)
    private val amber = Color.rgb(146, 64, 14)
    private val io = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var store: AppStore
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var navigation: LinearLayout
    private lateinit var loadStatus: TextView
    private var tab = 0
    private var visible = false
    private var busy = false
    private var loaded = false
    private var reading = false
    private var dialogOpen = false
    private var privacyAcknowledged = false
    private var shownListenerState: Pair<Boolean, Boolean>? = null
    private var masterEnabled = false
    private var destinations: List<Destination> = emptyList()
    private var deliveries: List<Delivery> = emptyList()
    private var stats: Snapshot? = null
    private var installed: List<InstalledApp> = emptyList()
    private var appQuery = ""
    private var historyFilter = 0
    private var editor: AlertDialog? = null
    private data class InstalledApp(val packageName: String, val label: String, val icon: Drawable?)
    private class NavIcon(context: Context, private val index: Int, color: Int) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; style = Paint.Style.STROKE; strokeWidth = 1.8f
            strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
        }
        init { importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO }
        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            canvas.save()
            canvas.scale(width / 24f, height / 24f)
            when (index) {
                0 -> {
                    val path = Path().apply { moveTo(3f, 10f); lineTo(12f, 3f); lineTo(21f, 10f); moveTo(5f, 9f); lineTo(5f, 21f); lineTo(10f, 21f); lineTo(10f, 14f); lineTo(14f, 14f); lineTo(14f, 21f); lineTo(19f, 21f); lineTo(19f, 9f) }
                    canvas.drawPath(path, paint)
                }
                1 -> {
                    canvas.drawCircle(12f, 12f, 8.5f, paint)
                    canvas.drawLine(12f, 7f, 12f, 12f, paint); canvas.drawLine(12f, 12f, 16f, 14f, paint)
                }
                2 -> for (x in listOf(4f, 14f)) for (y in listOf(4f, 14f)) canvas.drawRoundRect(RectF(x, y, x + 6f, y + 6f), 1.5f, 1.5f, paint)
                3 -> {
                    for (y in listOf(6f, 12f, 18f)) canvas.drawLine(3f, y, 21f, y, paint)
                    canvas.drawCircle(8f, 6f, 2f, paint); canvas.drawCircle(16f, 12f, 2f, paint); canvas.drawCircle(9f, 18f, 2f, paint)
                }
            }
            canvas.restore()
        }
    }
    private val refresh = object : Runnable {
        override fun run() {
            if (!visible) return
            if (!dialogOpen && !busy) reload(false)
            handler.postDelayed(this, 6000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = AppStore(applicationContext)
        tab = savedInstanceState?.getInt("tab", 0)?.coerceIn(0, 3) ?: 0
        appQuery = savedInstanceState?.getString("appQuery") ?: ""
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)
        window.statusBarColor = background
        window.navigationBarColor = Color.WHITE
        if (Build.VERSION.SDK_INT >= 30) window.insetsController?.setSystemBarsAppearance(
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
            WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS)
        else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(this@MainActivity.background)
            setOnApplyWindowInsetsListener { view, insets ->
                if (Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
                    val keyboard = insets.getInsets(WindowInsets.Type.ime())
                    view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
                } else {
                    @Suppress("DEPRECATION")
                    view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                        insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                }
                insets
            }
        }
        setContentView(root)
        window.decorView.setFilterTouchesWhenObscured(true)
        root.requestApplyInsets()
        header()
        scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val frame = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL }
        content = object : LinearLayout(this) {
            override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                val width = minOf(View.MeasureSpec.getSize(widthMeasureSpec), dp(720))
                super.onMeasure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY), heightMeasureSpec)
            }
        }.apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(12), dp(20), dp(24)) }
        // ponytail: one content column up to 720dp; split panes only if tablet workflows need them.
        frame.addView(content, LinearLayout.LayoutParams(-1, -2))
        scroll.addView(frame)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        navigation = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setBackgroundColor(Color.WHITE)
            elevation = dp(8).toFloat()
        }
        root.addView(navigation, LinearLayout.LayoutParams(-1, -2))
        render()
        loadApps()
    }

    override fun onResume() {
        super.onResume()
        visible = true
        reload(true)
        handler.removeCallbacks(refresh)
        handler.postDelayed(refresh, 6000)
    }
    override fun onPause() {
        visible = false
        handler.removeCallbacks(refresh)
        super.onPause()
    }
    override fun onDestroy() {
        editor?.dismiss()
        io.shutdown()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("tab", tab)
        outState.putString("appQuery", appQuery)
        super.onSaveInstanceState(outState)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41) {
            if (grantResults.firstOrNull() != PackageManager.PERMISSION_GRANTED)
                toast("Izin notifikasi ditolak. Anda dapat mengubahnya melalui pengaturan Android.")
            reload(true)
        }
    }

    private fun header() {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(dp(22), dp(16), dp(22), dp(10)) }
        row.addView(ImageView(this).apply {
            setImageResource(R.drawable.panelin_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(44), dp(44)).apply { marginEnd = dp(12) })
        row.addView(column().apply {
            addView(label("Panelin", 23f, ink, true))
            addView(label("Notifikasi · v${BuildConfig.VERSION_NAME}", 12f, muted))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        loadStatus = label("LOKAL", 10f, muted, true).apply {
            setPadding(dp(10), dp(7), dp(10), dp(7)); background = shape(Color.WHITE, 12)
            contentDescription = "Data disimpan di perangkat ini"
        }
        row.addView(loadStatus)
        root.addView(row)
    }

    private fun reload(force: Boolean) {
        if (reading || busy || isFinishing || isDestroyed) return
        reading = true
        io.execute {
            try {
                val nextDestinations = store.destinations()
                val nextDeliveries = store.deliveries(150)
                val nextStats = store.snapshot()
                val nextEnabled = store.enabled
                val acknowledged = getSharedPreferences("ui_privacy", MODE_PRIVATE).getBoolean("acknowledged_v1", false)
                handler.post {
                    reading = false
                    if (isDestroyed || isFinishing) return@post
                    val changed = !loaded || destinations != nextDestinations || deliveries != nextDeliveries || stats != nextStats || masterEnabled != nextEnabled
                    destinations = nextDestinations; deliveries = nextDeliveries; stats = nextStats; masterEnabled = nextEnabled; privacyAcknowledged = acknowledged; loaded = true
                    loadStatus.text = "LOKAL"
                    val listenerState = Forwarding.listenerEnabled(this) to Forwarding.listenerConnected(this)
                    if (!dialogOpen && (force || changed || (tab == 0 && listenerState != shownListenerState)) && (tab != 2 || force)) render(true)
                }
            } catch (_: Exception) {
                handler.post {
                    reading = false
                    if (!isDestroyed) {
                        loaded = false
                        stats = null
                        loadStatus.text = "GAGAL BACA"
                        if (!dialogOpen) render(true)
                        if (force) toast("Data lokal belum dapat dibaca. Coba lagi; data tidak dihapus.")
                    }
                }
            }
        }
    }
    private fun mutate(action: () -> Unit, after: () -> Unit = {}) {
        if (busy) { toast("Sedang menyimpan. Mohon tunggu."); return }
        busy = true
        loadStatus.text = "MENYIMPAN"
        io.execute {
            val success = try { action(); true } catch (_: Exception) { false }
            handler.post {
                busy = false
                if (isDestroyed || isFinishing) return@post
                loadStatus.text = if (success) "LOKAL" else "GAGAL SIMPAN"
                if (success) { after(); reload(true) } else {
                    toast("Perubahan tidak dapat diselesaikan. Muat ulang untuk memeriksa data tersimpan.")
                    reload(true)
                }
            }
        }
    }
    private fun render(preserveScroll: Boolean = false) {
        val oldY = if (preserveScroll) scroll.scrollY else 0
        content.removeAllViews()
        navigation.removeAllViews()
        listOf("Beranda", "Riwayat", "Aplikasi", "Pengaturan").forEachIndexed { index, title ->
            val item = column().apply {
                gravity = Gravity.CENTER
                minimumHeight = dp(58)
                setPadding(dp(2), dp(5), dp(2), dp(5))
                background = shape(if (tab == index) Color.rgb(239, 246, 255) else Color.WHITE, 16)
                isClickable = true; isFocusable = true
                isSelected = tab == index
                setOnClickListener { tab = index; render(); hideKeyboard() }
            }
            item.addView(NavIcon(this, index, if (tab == index) blue else muted), LinearLayout.LayoutParams(dp(25), dp(25)))
            item.addView(label(title, 10f, if (tab == index) blue else muted, tab == index).apply {
                gravity = Gravity.CENTER; importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            })
            navigation.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        when (tab) { 0 -> home(); 1 -> history(); 2 -> applications(); 3 -> settings() }
        scroll.post { scroll.scrollTo(0, oldY) }
    }

    private fun home() {
        title("Kendali notifikasi", "Notifikasi pembayaran, langsung ke website Anda.")
        if (!privacyAcknowledged) card {
            addView(label("Sebelum memberikan akses", 17f, ink, true))
            addView(label("Izin akses notifikasi Android dapat membaca notifikasi secara luas. Panelin hanya memproses aplikasi yang Anda pilih dan mengirim paket, judul, teks, serta teks panjang langsung ke website pilihan Anda. Data ini dapat berisi informasi pribadi. Tidak ada tujuan atau token bawaan; membuka aplikasi ini tidak mengirim data.", 14f, muted).withTop(8))
            addView(action("Baca privasi & batasan", false) { privacy() }.withTop(12))
            addView(action("Saya mengerti", true) { acknowledgePrivacy {} }.withTop(8))
        }
        val access = Forwarding.listenerEnabled(this)
        val connected = Forwarding.listenerConnected(this)
        shownListenerState = access to connected
        val activeApps = destinations.count { it.enabled }
        card {
            val switch = Switch(this@MainActivity).apply {
                text = "Penerusan notifikasi"; textSize = 17f; setTextColor(ink); minHeight = dp(52)
                isChecked = masterEnabled; isEnabled = loaded && !busy
                setOnCheckedChangeListener { _, checked ->
                    if (checked && !privacyAcknowledged) {
                        isChecked = false
                        privacyGate { }
                    } else if (checked && activeApps == 0) {
                        isChecked = false
                        toast("Atur dan aktifkan setidaknya satu aplikasi terlebih dahulu.")
                        tab = 2; render()
                    } else if (checked != masterEnabled) {
                        mutate({ store.enabled = checked; if (checked) { Forwarding.schedule(applicationContext); Forwarding.reconnect(applicationContext) } })
                    }
                }
            }
            addView(switch)
            val status = when {
                !loaded -> "Status belum dapat dipastikan"
                !masterEnabled -> "Dijeda di perangkat ini"
                !access -> "Akses notifikasi diperlukan"
                !connected -> "Menunggu listener Android"
                activeApps == 0 -> "Belum ada aplikasi aktif"
                else -> "Siap menerima notifikasi"
            }
            addView(badge(status, if (loaded && masterEnabled && connected && access && activeApps > 0) green else amber))
            addView(label("$activeApps aplikasi aktif · ${destinations.size} dikonfigurasi", 13f, muted).withTop(12))
            addView(label("Status ini menunjukkan listener di ponsel, bukan koneksi atau kesehatan server.", 12f, muted).withTop(8))
        }
        section("Ringkasan perangkat")
        val s = stats
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row.addView(metric(s?.todayCount, "Diterima hari ini"), LinearLayout.LayoutParams(0, -2, 1f).apply { marginEnd = dp(10) })
        row.addView(metric(s?.creditedCount, "Kredit tercatat"), LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(row)
        card {
            keyValue("Dalam antrean", s?.queuedCount?.toString() ?: "—")
            keyValue("Perlu perhatian", s?.attentionCount?.toString() ?: "—")
            keyValue("Terakhir diterima", time(s?.lastReceivedAt ?: 0))
            keyValue("Terakhir dikirim", time(s?.lastSentAt ?: 0))
            addView(label("Kredit tercatat dihitung dari riwayat tersimpan, bukan saldo website. HTTP 200 belum tentu saldo masuk; hanya hasil “credited” berarti dikreditkan oleh server.", 12f, muted).withTop(8))
        }
        section("Persiapan perangkat")
        card {
            checklist("Akses notifikasi", if (access) "Diizinkan oleh Android" else "Wajib agar aplikasi dapat membaca notifikasi pilihan Anda", access) {
                privacyGate { openSettings(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            }
            val post = canPostNotifications()
            checklist("Notifikasi aplikasi", if (post) "Diizinkan" else "Untuk pemberitahuan status; bukan izin membaca notifikasi", post) { requestPostNotifications() }
            val exempt = batteryExempt()
            checklist("Pengaturan baterai", if (exempt) "Dikecualikan dari optimasi Android" else "Optimasi dapat menunda pekerjaan latar belakang", exempt) {
                openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
            addView(action("Listener belum terhubung?", false) {
                Forwarding.reconnect(applicationContext)
                toast("Permintaan sambung ulang dikirim ke Android; koneksi belum dijamin.")
                reload(true)
            })
        }
        card {
            addView(label("Mulai dari aplikasi Anda", 16f, ink, true))
            addView(label("Pilih aplikasi pembayaran, masukkan endpoint dan token milik website Anda, lalu beri persetujuan. Tidak ada server bawaan.", 14f, muted).withTop(6))
            addView(action("Kelola aplikasi", true) { tab = 2; render() }.withTop(10))
        }
    }

    private fun history() {
        title("Riwayat pengiriman", "Hasil nyata dari antrean lokal perangkat ini.")
        val filters = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Semua aktivitas", "Dalam antrean", "Perlu perhatian", "Dikreditkan"))
            minimumHeight = dp(48)
            contentDescription = "Filter riwayat"
            setSelection(historyFilter)
        }
        content.addView(filters)
        val list = column()
        content.addView(list)
        fun updateList() {
            list.removeAllViews()
            val selected = deliveries.filter { when (historyFilter) {
                1 -> queued(it)
                2 -> attention(it)
                3 -> it.outcome == "credited"
                else -> true
            } }
            if (selected.isEmpty()) {
                list.addView(emptyCard(if (loaded) "Belum ada aktivitas" else "Memuat riwayat", "Notifikasi dari aplikasi aktif akan muncul di sini. Tidak ada pengiriman percobaan otomatis."))
            } else selected.forEach { item ->
                list.addView(cardView().apply {
                    addView(label(item.appLabel.ifBlank { item.packageName }, 16f, ink, true))
                    addView(label(time(item.createdAt), 12f, muted).withTop(3))
                    addView(badge(deliveryLabel(item), if (item.outcome == "credited") green else if (attention(item)) amber else blue).withTop(10))
                    addView(label("Percobaan: ${item.attempts} · Isi notifikasi disembunyikan", 12f, muted).withTop(9))
                    addView(action("Lihat detail", false) { deliveryDetails(item) })
                })
            }
            if (deliveries.size >= 150) list.addView(label("Menampilkan hingga 150 aktivitas terbaru.", 12f, muted).withTop(10))
        }
        filters.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) { historyFilter = position; updateList() }
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        updateList()
        content.addView(action("Muat ulang", false) { reload(true) }.withTop(12))
    }
    private fun queued(d: Delivery): Boolean = d.state.lowercase() in setOf("queued", "pending", "retry", "retrying", "sending", "processing")
    private fun attention(d: Delivery): Boolean = d.state.lowercase() in setOf("attention", "held", "failed", "error") || d.outcome in setOf("review", "ambiguous", "unmatched")
    private fun deliveryLabel(d: Delivery): String = when {
        d.state == "held" -> "Ditahan · tujuan berubah"
        d.outcome in setOf("credited", "ignored", "unmatched", "ambiguous", "review", "duplicate") -> UiRules.outcomeLabel(d.outcome)
        queued(d) -> "Dalam antrean"
        attention(d) -> "Perlu perhatian"
        d.state.lowercase() in setOf("sent", "delivered", "done", "success") -> "Terkirim · hasil belum diketahui"
        else -> "Tercatat"
    }
    private fun deliveryDetails(d: Delivery) {
        val box = column().apply { setPadding(dp(22), dp(8), dp(22), dp(16)) }
        box.addView(label("${d.packageName}\n${time(d.createdAt)}\n${deliveryLabel(d)}\n${d.attempts} percobaan", 14f, ink))
        box.addView(label("Hasil server menentukan pemrosesan saldo. Duplikat, diabaikan, atau terkirim bukan bukti kredit.", 13f, muted).withTop(12))
        if (d.detail.isNotBlank()) box.addView(label(d.detail, 13f, muted).withTop(10))
        val reveal = action("Tampilkan isi sensitif di perangkat ini", false) {}
        reveal.setOnClickListener {
            reveal.visibility = View.GONE
            box.addView(label(listOf(d.title, d.text, d.bigText).filter { it.isNotBlank() }.distinct().joinToString("\n\n").ifBlank { "Tidak ada isi." }, 14f, ink).apply { setTextIsSelectable(true) }.withTop(12))
        }
        box.addView(reveal.withTop(12))
        val details = AlertDialog.Builder(this).setTitle(d.appLabel).setView(ScrollView(this).apply { addView(box) }).setPositiveButton("Tutup", null).create()
        track(details)
        details.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun loadApps() {
        io.execute {
            val result = try {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
                    .filter { it.activityInfo.packageName != packageName }
                    .distinctBy { it.activityInfo.packageName }
                    .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(packageManager).toString(), try { it.loadIcon(packageManager) } catch (_: Exception) { null }) }
                    .sortedBy { it.label.lowercase() }
            } catch (_: Exception) { emptyList() }
            handler.post { if (!isDestroyed) { installed = result; if (tab == 2 && !dialogOpen) render(true) } }
        }
    }
    private fun applications() {
        title("Aplikasi pilihan", "Hanya aplikasi yang Anda setujui yang diteruskan.")
        content.addView(label("${destinations.count { it.enabled }} aktif · ${destinations.size} dikonfigurasi", 13f, muted))
        val search = input("Cari nama atau paket aplikasi", appQuery).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            contentDescription = "Cari aplikasi terpasang"
            setSingleLine(true)
        }
        content.addView(search.withTop(14))
        val list = column()
        content.addView(list)
        fun updateList(query: String) {
            list.removeAllViews()
            val merged = (installed + destinations.filter { d -> installed.none { it.packageName == d.packageName } }.map { InstalledApp(it.packageName, it.label, null) })
                .sortedWith(compareByDescending<InstalledApp> { a -> destinations.any { it.packageName == a.packageName } }.thenBy { it.label.lowercase() })
                .filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
            if (merged.isEmpty()) list.addView(emptyCard("Tidak ada aplikasi ditemukan", "Cari nama lain. Daftar hanya berisi aplikasi dengan ikon peluncur yang dapat dilihat oleh Android."))
            merged.forEach { app ->
                val configured = destinations.find { it.packageName == app.packageName }
                list.addView(cardView().apply {
                    val row = LinearLayout(this@MainActivity).apply { gravity = Gravity.CENTER_VERTICAL }
                    row.addView(ImageView(this@MainActivity).apply { setImageDrawable(app.icon); importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO },
                        LinearLayout.LayoutParams(dp(42), dp(42)).apply { marginEnd = dp(12) })
                    row.addView(column().apply {
                        addView(label(app.label, 16f, ink, true))
                        addView(label(app.packageName, 11f, muted).withTop(3))
                    }, LinearLayout.LayoutParams(0, -2, 1f))
                    addView(row)
                    addView(badge(if (configured == null) "Belum diatur" else if (configured.enabled) "Aktif" else "Dijeda", if (configured?.enabled == true) green else muted).withTop(12))
                    addView(action(if (configured == null) "Atur penerusan" else "Edit tujuan & izin", false) { editDestination(app, configured) })
                })
            }
        }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { appQuery = s?.toString() ?: ""; updateList(appQuery) }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        updateList(appQuery)
    }
    private fun editDestination(app: InstalledApp, existing: Destination?) {
        if (!privacyAcknowledged) { privacyGate { editDestination(app, existing) }; return }
        val box = column().apply { setPadding(dp(22), dp(6), dp(22), dp(18)) }
        box.addView(label(app.packageName, 12f, muted))
        box.addView(label("Endpoint HTTPS", 13f, ink, true).withTop(16))
        val endpoint = input("https://website-anda/callback/notification/ID", existing?.endpoint ?: "").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            setSingleLine(true)
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        }
        box.addView(endpoint.withTop(5))
        box.addView(label("Token milik metode pembayaran", 13f, ink, true).withTop(14))
        val token = input("Token rahasia", existing?.token ?: "").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            transformationMethod = PasswordTransformationMethod.getInstance()
            setSingleLine(true)
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            isSaveEnabled = false
        }
        token.setFilterTouchesWhenObscured(true)
        box.addView(token.withTop(5))
        box.addView(label("Token tetap tersembunyi. Ganti seluruh isi untuk mengubahnya. Jangan gunakan token website milik orang lain.", 12f, muted).withTop(6))
        val enabled = Switch(this).apply { text = "Aktifkan aplikasi ini"; isChecked = existing?.enabled ?: true; minHeight = dp(52); setTextColor(ink) }
        box.addView(enabled.withTop(8))
        val consent = CheckBox(this).apply {
            text = "Saya mengizinkan isi notifikasi aplikasi ini (paket, judul, teks, dan teks panjang) dikirim ke website yang saya isi. Saya berwenang menggunakan tujuan ini."
            setTextColor(ink); textSize = 13f; minHeight = dp(48)
            isChecked = false
        }
        box.addView(consent.withTop(8))
        box.addView(label("Tidak ada permintaan uji ke server. Perubahan endpoint atau token menahan antrean lama agar tidak dikirim ke tujuan baru. Menjeda aplikasi tidak menghapus antrean.", 12f, muted).withTop(10))
        val validation = label("", 13f, amber).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        box.addView(validation.withTop(8))
        val dialog = AlertDialog.Builder(this).setTitle(app.label)
            .setView(ScrollView(this).apply { addView(box) })
            .setNegativeButton("Batal", null)
            .setNeutralButton("Validasi lokal", null)
            .setPositiveButton("Simpan", null).create()
        dialog.setOnShowListener {
            fun validate(): Boolean {
                val error = UiRules.endpointError(endpoint.text.toString().trim()) ?: UiRules.tokenError(token.text.toString())
                validation.text = error ?: "Format valid secara lokal. Ketersediaan server dan token belum diuji; tidak ada data dikirim."
                return error == null
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { validate() }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (!validate()) return@setOnClickListener
                if (!consent.isChecked) { validation.text = "Persetujuan diperlukan sebelum menyimpan tujuan ini."; return@setOnClickListener }
                val value = Destination(app.packageName, app.label, endpoint.text.toString().trim(), token.text.toString(), enabled.isChecked)
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                mutate({ store.saveDestination(value); Forwarding.schedule(applicationContext) }) {
                    token.text.clear(); dialog.dismiss(); toast("Konfigurasi tersimpan di perangkat.")
                }
                // Keep retry possible if storage fails; mutate itself prevents concurrent writes.
                handler.postDelayed({ if (dialog.isShowing) dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true }, 1500)
            }
        }
        track(dialog)
        dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if (existing != null) {
            val remove = action("Hapus konfigurasi aplikasi", false) {
                AlertDialog.Builder(this).setTitle("Hapus tujuan?")
                    .setMessage("Notifikasi baru dari ${app.label} tidak akan diteruskan. Antrean yang belum dikirim akan ditahan, bukan dialihkan ke tujuan lain. Pengiriman yang sudah berjalan tidak dapat ditarik kembali.")
                    .setNegativeButton("Batal", null).setPositiveButton("Hapus") { _, _ ->
                        mutate({ store.removeDestination(app.packageName) }) { token.text.clear(); dialog.dismiss() }
                    }.show()
            }
            box.addView(remove.withTop(10))
        }
    }

    private fun settings() {
        title("Pengaturan", "Privasi jelas. Kendali tetap di perangkat Anda.")
        card {
            addView(label("Privasi & data", 17f, ink, true))
            addView(label("Notifikasi aplikasi terpilih dikirim langsung dari ponsel ke endpoint Anda. Tidak ada telemetri terpusat atau server perantara Panelin bawaan.", 14f, muted).withTop(8))
            addView(action("Pelajari alur & batasan", false) { privacy() })
            addView(action("Hapus riwayat selesai", false) {
                track(AlertDialog.Builder(this@MainActivity).setTitle("Hapus riwayat selesai?")
                    .setMessage("Hanya aktivitas terminal yang dihapus dari perangkat ini. Antrean tertunda tidak dihapus. Ini tidak menghapus catatan atau transaksi di server Anda.")
                    .setNegativeButton("Batal", null).setPositiveButton("Hapus riwayat") { _, _ -> mutate({ store.clearHistory() }) { toast("Riwayat selesai dihapus; antrean tetap disimpan.") } }.create())
            })
        }
        card {
            addView(label("Keandalan latar belakang", 17f, ink, true))
            addView(label("Status baterai Android: ${if (batteryExempt()) "dikecualikan dari optimasi" else "masih dapat dioptimasi"}. Pengaturan autostart dan pembatasan vendor tidak dapat diverifikasi oleh aplikasi ini.", 14f, muted).withTop(8))
            addView(action("Buka pengaturan baterai", false) { openSettings(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) })
            addView(action("Panduan autostart & OEM", false) { oemHelp() })
            addView(action("Info aplikasi di Android", false) { openSettings(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) })
        }
        card {
            addView(label("Panelin Notifikasi", 17f, ink, true))
            keyValue("Versi aplikasi", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            addView(label("Aplikasi pendamping untuk website Panelin Anda. Status listener bukan bukti bahwa endpoint dapat dijangkau.", 13f, muted).withTop(8))
            addView(action("Yang baru di versi ini", false) { changelog() })
            addView(action("Bantuan penyiapan", false) { help() })
        }
    }
    private fun privacy() = info("Privasi & batasan", "• Hanya paket aplikasi yang Anda pilih dan aktifkan yang diproses. Izin Android secara teknis memberikan akses notifikasi yang luas; pilih aplikasi dengan hati-hati.\n\n• Paket, judul, teks, dan teks panjang dapat mengandung data sensitif. Tujuan ditentukan sendiri dan wajib HTTPS. Token disimpan terenkripsi oleh penyimpanan aplikasi, tidak ditampilkan dalam riwayat.\n\n• Antrean lokal mempertahankan tujuan dan token awal. Mengganti endpoint atau token, maupun menghapus konfigurasi, menahan antrean yang belum dikirim; tidak mengalihkannya ke tujuan baru. Antrean ditahan tetap disimpan untuk peninjauan, bukan dikirim otomatis.\n\n• Menjeda penerusan menghentikan pemrosesan sesuai kendali layanan; proses yang sudah dikirim tidak dapat ditarik kembali. Cabut token di website jika akses harus segera dihentikan.\n\n• Server menentukan saldo. HTTP 200 bukan jaminan kredit. Fingerprint server dapat menganggap pembayaran dengan akun, paket, judul, dan nominal berulang sebagai duplikat; aplikasi tidak mengubah aturan itu.\n\n• Hapus riwayat hanya menghapus aktivitas selesai di perangkat; data yang telah diterima website mengikuti kebijakan website Anda.")
    private fun help() = info("Bantuan penyiapan", "1. Pada website Anda, buka metode pembayaran yang menerima notifikasi. Salin endpoint HTTPS /callback/notification/ID angka dan tokennya. Jangan membagikan token.\n\n2. Di tab Aplikasi, pilih aplikasi pembayaran yang benar. Masukkan endpoint dan token, beri persetujuan, lalu simpan. Validasi lokal hanya memeriksa format.\n\n3. Izinkan akses notifikasi melalui pengaturan Android dan aktifkan penerusan di Beranda. Tinjau pembatasan baterai/autostart.\n\n4. Tunggu notifikasi pembayaran nyata. Periksa hasil di Riwayat dan konfirmasi transaksi di website. Membuka aplikasi atau melakukan validasi lokal tidak mengirim webhook percobaan.\n\nFormat preset DANA yang diverifikasi server: paket id.dana, judul Pembayaran Masuk, teks Rp{nominal} diterima DANA Bisnis. Filter lokal menolak OTP, promo, dan transaksi keluar yang jelas; keputusan parsing tetap di server.\n\nJika tidak ada riwayat: cek paket pilihan, sakelar aktif, akses notifikasi, koneksi listener, dan pembatasan vendor. Jika pengiriman gagal: periksa endpoint, token, jaringan, dan konfigurasi website. Jangan membagikan isi notifikasi atau token saat meminta bantuan.")
    private fun oemHelp() = info("Autostart & pembatasan vendor", "Nama menu berbeda pada setiap versi Android dan vendor. Aplikasi ini tidak dapat memastikan apakah autostart sudah diizinkan.\n\nXiaomi / Redmi / POCO: cari Autostart dan Penghemat baterai aplikasi; pilih tanpa pembatasan bila tersedia.\n\nOPPO / realme / vivo: cari Peluncuran otomatis, Aktivitas latar belakang, atau Manajemen baterai aplikasi.\n\nSamsung: periksa Batas penggunaan latar belakang dan pastikan aplikasi tidak berada di daftar tidur lelap.\n\nPerangkat lain: buka Info aplikasi → Baterai dan izinkan aktivitas latar belakang sesuai kebijakan perangkat.\n\nSetelah memulai ulang ponsel, buka aplikasi dan periksa kembali status listener. Force stop dapat menghentikan pekerjaan sampai aplikasi dibuka lagi. Tidak ada pengaturan yang menjamin pengiriman real-time.")
    private fun changelog() {
        io.execute {
            val text = try {
                val entries = JSONArray(assets.open("changelog.json").bufferedReader().use { it.readText() })
                (0 until entries.length()).joinToString("\n\n") { index ->
                    val entry = entries.getJSONObject(index)
                    val changes = entry.optJSONArray("changes") ?: JSONArray()
                    "${entry.optString("version")} (${entry.optInt("code")}) · ${entry.optString("date")}\n" +
                        (0 until changes.length()).joinToString("\n") { "• ${changes.getString(it)}" }
                }.ifBlank { "Belum ada catatan perubahan dalam paket ini." }
            } catch (_: Exception) { "Catatan perubahan lokal belum tersedia dalam paket ini.\nVersi terpasang: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})." }
            handler.post { if (!isDestroyed && visible) info("Catatan perubahan", text) }
        }
    }

    private fun canPostNotifications(): Boolean = Build.VERSION.SDK_INT < 33 || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    private fun acknowledgePrivacy(after: () -> Unit) {
        mutate({
            check(getSharedPreferences("ui_privacy", MODE_PRIVATE).edit().putBoolean("acknowledged_v1", true).commit())
        }) { privacyAcknowledged = true; after() }
    }
    private fun privacyGate(after: () -> Unit) {
        if (privacyAcknowledged) { after(); return }
        track(AlertDialog.Builder(this).setTitle("Persetujuan akses notifikasi")
            .setMessage("Akses Android dapat membaca semua notifikasi. Panelin memfilter aplikasi pilihan Anda sebelum membaca isinya. Paket, judul, teks, dan teks panjang dari aplikasi aktif diteruskan langsung ke website HTTPS yang Anda tentukan; isinya bisa sensitif. Tidak ada server atau pengiriman uji bawaan. Anda tetap harus menyetujui setiap tujuan. Lanjutkan hanya jika Anda memahami akses ini.")
            .setNegativeButton("Batal", null).setPositiveButton("Saya mengerti") { dialog, _ ->
                dialog.dismiss()
                acknowledgePrivacy(after)
            }.create())
    }
    private fun requestPostNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && !canPostNotifications()) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 41)
        else openSettings(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }
    private fun batteryExempt(): Boolean = (getSystemService(POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
    private fun openSettings(intent: Intent) {
        try { startActivity(intent) }
        catch (_: ActivityNotFoundException) { toast("Menu tidak tersedia. Buka pengaturan Android → Aplikasi → Panelin Notifikasi.") }
        catch (_: SecurityException) { toast("Android membatasi pembukaan menu ini. Buka pengaturan secara manual.") }
    }
    private fun track(dialog: AlertDialog) {
        editor = dialog
        dialogOpen = true
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener {
            dialogOpen = false
            editor = null
            if (visible) reload(true)
        }
        dialog.show()
        dialog.window?.setBackgroundDrawable(shape(Color.WHITE, 22))
    }
    private fun info(title: String, text: String) {
        val body = label(text, 14f, ink).apply { setPadding(dp(22), dp(8), dp(22), dp(20)) }
        track(AlertDialog.Builder(this).setTitle(title).setView(ScrollView(this).apply { addView(body) }).setPositiveButton("Mengerti", null).create())
    }
    private fun title(title: String, subtitle: String) {
        content.addView(label(title, 27f, ink, true).apply { if (Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true })
        content.addView(label(subtitle, 14f, muted).withTop(7))
        content.addView(Space(this), LinearLayout.LayoutParams(1, dp(8)))
    }
    private fun section(text: String) { content.addView(label(text, 17f, ink, true).withTop(19)) }
    private fun metric(value: Int?, title: String): LinearLayout = cardView().apply {
        addView(label(value?.toString() ?: "—", 31f, blue, true))
        addView(label(title, 13f, muted).withTop(4))
    }
    private fun card(block: LinearLayout.() -> Unit) { content.addView(cardView().apply(block)) }
    private fun cardView() = column().apply {
        background = shape(Color.WHITE, 22)
        setPadding(dp(18), dp(17), dp(18), dp(15))
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(12) }
    }
    private fun emptyCard(title: String, description: String): LinearLayout = cardView().apply {
        addView(label(title, 17f, ink, true))
        addView(label(description, 14f, muted).withTop(8))
    }
    private fun LinearLayout.keyValue(key: String, value: String) {
        val row = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP; setPadding(0, dp(9), 0, dp(9)) }
        row.addView(label(key, 13f, muted), LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(label(value, 13f, ink, true).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(row)
    }
    private fun LinearLayout.checklist(title: String, detail: String, okay: Boolean, onClick: () -> Unit) {
        val row = column().apply {
            minimumHeight = dp(76); setPadding(0, dp(10), 0, dp(10))
            isClickable = true; isFocusable = true
            contentDescription = "$title. $detail. Ketuk untuk membuka pengaturan."
            addView(label("${if (okay) "✓" else "○"}  $title  ›", 15f, if (okay) green else ink, true))
            addView(label(detail, 12f, muted).withTop(5))
            setOnClickListener { onClick() }
        }
        addView(row)
    }
    private fun label(text: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        this.text = text; textSize = size; setTextColor(color)
        if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        includeFontPadding = true
    }
    private fun badge(text: String, color: Int) = label(text, 12f, color, true).apply {
        setPadding(dp(10), dp(7), dp(10), dp(7))
        background = shape(Color.argb(18, Color.red(color), Color.green(color), Color.blue(color)), 10)
        layoutParams = LinearLayout.LayoutParams(-2, -2)
    }
    private fun action(title: String, primary: Boolean, onClick: () -> Unit) = Button(this).apply {
        text = title; textSize = 13f; isAllCaps = false
        setTextColor(if (primary) Color.WHITE else blue)
        minHeight = dp(48); minimumHeight = dp(48)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        background = shape(if (primary) blue else Color.rgb(239, 246, 255), 13)
        stateListAnimator = null
        setOnClickListener { onClick() }
    }
    private fun input(hint: String, value: String) = EditText(this).apply {
        this.hint = hint; setText(value); textSize = 14f; setTextColor(ink); setHintTextColor(muted)
        setPadding(dp(12), dp(13), dp(12), dp(13)); minHeight = dp(50)
        background = shape(this@MainActivity.background, 12)
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
        isSaveEnabled = false
    }
    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun shape(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
    private fun <T : View> T.withTop(margin: Int): T = apply {
        val params = (layoutParams as? LinearLayout.LayoutParams) ?: LinearLayout.LayoutParams(-1, -2)
        params.topMargin = dp(margin); layoutParams = params
    }
    private fun dp(value: Int) = (value * resources.displayMetrics.density + .5f).toInt()
    private fun time(value: Long): String = if (value <= 0) "Belum ada" else DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(value))
    private fun toast(text: String) { Toast.makeText(this, text, Toast.LENGTH_LONG).show() }
    private fun hideKeyboard() { (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(root.windowToken, 0); root.clearFocus() }
}

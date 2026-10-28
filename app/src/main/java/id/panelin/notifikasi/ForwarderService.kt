package id.panelin.notifikasi

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object Forwarding {
    @Volatile internal var connected = false

    fun listenerEnabled(context: Context): Boolean {
        val expected = ComponentName(context, ForwarderService::class.java)
        return Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?.split(':')?.any { ComponentName.unflattenFromString(it) == expected } == true
    }

    fun listenerConnected(context: Context): Boolean = connected && listenerEnabled(context)

    fun reconnect(context: Context) {
        if (listenerEnabled(context)) NotificationListenerService.requestRebind(ComponentName(context, ForwarderService::class.java))
    }

    fun refreshStatus(context: Context) {
        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        if (!AppStore(context).enabled) { manager.cancel(701); return }
        if (android.os.Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) return
        manager.createNotificationChannel(android.app.NotificationChannel("monitoring", "Status pemantauan", android.app.NotificationManager.IMPORTANCE_LOW))
        val title = when {
            !listenerEnabled(context) -> "Menunggu akses notifikasi"
            !listenerConnected(context) -> "Menunggu sambungan Android"
            else -> "Panelin sedang memantau"
        }
        val intent = android.app.PendingIntent.getActivity(context, 0, android.content.Intent(context, MainActivity::class.java), android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(701, Notification.Builder(context, "monitoring").setSmallIcon(R.drawable.ic_status)
            .setContentTitle(title).setContentText("Hanya aplikasi pilihan Anda · ketuk untuk mengelola")
            .setContentIntent(intent).setOngoing(true).setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE).build())
    }

    fun schedule(context: Context) {
        refreshStatus(context)
        val manager = WorkManager.getInstance(context.applicationContext)
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val request = OneTimeWorkRequestBuilder<DeliveryWorker>().setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 60, TimeUnit.SECONDS).build()
        // Append closes the enqueue-vs-finishing-worker race without parallel immediate senders.
        manager.enqueueUniqueWork("panelin-delivery", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        // WorkManager restores this after process death/reboot; never need a boot receiver.
        manager.enqueueUniquePeriodicWork("panelin-recovery", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DeliveryWorker>(15, TimeUnit.MINUTES).setConstraints(constraints).build())
    }

    internal fun wakeAt(context: Context, time: Long) {
        val delay = (time - System.currentTimeMillis()).coerceAtLeast(1_000)
        val request = OneTimeWorkRequestBuilder<DeliveryWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 60, TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniqueWork("panelin-wakeup", ExistingWorkPolicy.REPLACE, request)
    }
}

class ForwarderService : NotificationListenerService() {
    private val capture = Executors.newSingleThreadExecutor()
    private lateinit var store: AppStore

    override fun onCreate() {
        super.onCreate()
        store = AppStore(this)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Forwarding.connected = true
        Forwarding.schedule(this)
        // Recover still-visible notifications after process death; durable event IDs prevent replay.
        try { activeNotifications?.forEach { onNotificationPosted(it) } } catch (_: SecurityException) { Forwarding.connected = false }
    }

    override fun onListenerDisconnected() {
        Forwarding.connected = false
        Forwarding.refreshStatus(this)
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        Forwarding.connected = false
        Forwarding.refreshStatus(this)
        capture.shutdown()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null || capture.isShutdown) return
        capture.execute {
            try {
                // Consent and package selection are checked BEFORE notification extras are touched.
                val destination = store.selected(sbn.packageName) ?: return@execute
                val notification = sbn.notification ?: return@execute
                val key = "${sbn.key}:${sbn.postTime}"
                if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
                    store.recordSkip(sbn.packageName, "group", key)
                    return@execute
                }
                val extras = notification.extras ?: run {
                    store.recordSkip(sbn.packageName, "invalid", key)
                    return@execute
                }
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
                val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
                val reason = Core.skipReason(title, text, big)
                if (reason != null) {
                    store.recordSkip(sbn.packageName, reason, key)
                    return@execute
                }
                // postTime distinguishes reuse of a notification ID; content digest preserves updates.
                store.enqueue(sbn.packageName, destination.label, title, text, big, "${sbn.key}:${sbn.postTime}")
            } catch (_: Exception) {
                // Persist a content-free visible warning; never log notification data or tokens.
                runCatching { store.recordCaptureFailure() }
                // ponytail: no plaintext fallback on Keystore/disk failure; device storage must be repaired.
            }
        }
    }
}

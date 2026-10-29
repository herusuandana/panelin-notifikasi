package id.panelin.notifikasi

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.security.KeyStore
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** SQLite transactions protect queue state; sensitive columns use Keystore AES-GCM. */
class AppStore(context: Context) {
    private val app = context.applicationContext
    private val db: SQLiteDatabase get() = database(app).writableDatabase
    private val preferences = app.getSharedPreferences("forwarding", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = preferences.getBoolean("enabled", false)
        set(value) {
            synchronized(lock) {
                check(preferences.edit().putBoolean("enabled", value).commit()) { "Pengaturan gagal disimpan" }
            }
            if (value) Forwarding.schedule(app) else Forwarding.refreshStatus(app)
        }

    fun destinations(): List<Destination> = synchronized(lock) {
        db.query("destinations", arrayOf("route", "payload"), null, null, null, null, null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(destination(decrypt(cursor.getBlob(1), "destination:${cursor.getString(0)}")))
            }.sortedBy { it.label.lowercase() }
        }
    }

    internal fun recordCaptureFailure() {
        preferences.edit().putLong("capture_failure", System.currentTimeMillis()).commit()
    }

    internal fun selected(packageName: String): Destination? = synchronized(lock) {
        if (!enabled) return@synchronized null
        findDestination(packageName)?.takeIf { it.enabled }
    }

    fun saveDestination(value: Destination) {
        require(value.packageName.matches(Regex("[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)+"))) { "Paket tidak valid" }
        require(Core.validEndpoint(value.endpoint)) { "Gunakan URL HTTPS /callback/notification/ID tanpa query" }
        require(value.token.isNotBlank() && value.token.length <= 100 && value.token.none { it.isWhitespace() || it.isISOControl() }) { "Token tidak valid" }
        synchronized(lock) {
            val route = route(value.packageName)
            val previous = findDestination(value.packageName)
            transaction {
                db.insertWithOnConflict("destinations", null, ContentValues().apply {
                    put("route", route)
                    put("payload", encrypt(destinationJson(value).toString(), "destination:$route"))
                }, SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) }
                // Never redirect pending payment data to edited credentials. Explicit review is required.
                if (previous != null && (previous.endpoint != value.endpoint || previous.token != value.token)) {
                    traceHold(route, "Konfigurasi tujuan berubah. Antrean lama ditahan.")
                    db.execSQL("UPDATE queue SET state='held', detail='Konfigurasi tujuan berubah; antrean lama ditahan' WHERE route=? AND state IN ('queued','sending')", arrayOf(route))
                }
            }
        }
        Forwarding.schedule(app)
    }

    fun removeDestination(packageName: String) = synchronized(lock) {
        transaction {
            val route = route(packageName)
            db.delete("destinations", "route=?", arrayOf(route))
            traceHold(route, "Tujuan dihapus. Antrean ditahan.")
            db.execSQL("UPDATE queue SET state='held', detail='Tujuan dihapus; antrean ditahan' WHERE route=? AND state IN ('queued','sending')", arrayOf(route))
        }
    }

    fun enqueue(packageName: String, appLabel: String, title: String, text: String, bigText: String, key: String): Boolean {
        val inserted = synchronized(lock) {
            val destination = selected(packageName) ?: return@synchronized false
            if (!Core.shouldForward(title, text, bigText)) return@synchronized false
            val event = Core.eventIdentity(packageName, key, title, text, bigText)
            val payload = destinationJson(destination).put("title", title).put("text", text).put("bigtext", bigText).put("label", appLabel)
            var inserted = false
            transaction {
                // Keep dedup tombstones when visible terminal history is cleared.
                val seen = db.insertWithOnConflict("seen", null, ContentValues().apply { put("event", event) }, SQLiteDatabase.CONFLICT_IGNORE)
                if (seen == -1L) {
                    recordSkip(packageName, "duplicate", key)
                    return@transaction
                }
                val row = db.insertOrThrow("queue", null, ContentValues().apply {
                put("event", event)
                put("route", route(packageName))
                put("payload", encrypt(payload.toString(), "queue:$event"))
                put("created", System.currentTimeMillis())
                put("state", "queued")
                put("outcome", "")
                put("attempts", 0)
                put("detail", "Menunggu pengiriman")
                put("next_at", 0)
                put("lease_until", 0)
                put("sent_at", 0)
                })
                inserted = row != -1L
                addTrace(event, "observed", "Notifikasi terbaca", "Sumber aktif dan format pembayaran sesuai")
                addTrace(event, "queued", "Masuk antrean", "Menunggu koneksi dan izin pengiriman")
            }
            inserted
        }
        if (inserted) Forwarding.schedule(app)
        return inserted
    }

    internal fun recordSkip(packageName: String, reason: String, key: String) = synchronized(lock) {
        val destination = selected(packageName) ?: return@synchronized
        val event = Core.eventIdentity(packageName, key, reason)
        db.rawQuery("SELECT 1 FROM trace WHERE event=? AND kind='skip' LIMIT 1", arrayOf(event)).use {
            if (it.moveToFirst()) return@synchronized
        }
        addTrace(event, "skip", "Dilewati di HP", Core.skipLabel(reason), packageName, destination.label, reason)
    }

    private fun traceHold(route: String, detail: String) {
        db.rawQuery("SELECT event FROM queue WHERE route=? AND state IN ('queued','sending')", arrayOf(route)).use { cursor ->
            while (cursor.moveToNext()) addTrace(cursor.getString(0), "held", "Antrean ditahan", detail)
        }
    }

    private fun addTrace(event: String, kind: String, stage: String, detail: String, pkg: String = "", label: String = "", reason: String = "", time: Long = System.currentTimeMillis()) {
        val data = JSONObject().put("stage", stage.take(100)).put("detail", detail.take(1200)).put("pkg", pkg.take(200)).put("label", label.take(200)).put("reason", reason.take(40))
        try {
            db.insertOrThrow("trace", null, ContentValues().apply {
                put("event", event); put("kind", kind); put("created", time)
                put("payload", encrypt(data.toString(), "trace:$event:$time"))
            })
            // ponytail: keep the latest 2000 diagnostic steps, never prune payment rows or dedup tombstones.
            db.execSQL("DELETE FROM trace WHERE id NOT IN (SELECT id FROM trace ORDER BY id DESC LIMIT 2000)")
        } catch (_: Exception) {
            // Diagnostics must never turn an acknowledged payment into a retry or roll back its queue row.
            runCatching { recordCaptureFailure() }
        }
    }

    private fun traces(event: String): List<TraceStep> = db.query("trace", null, "event=?", arrayOf(event), null, null, "id ASC").use { cursor ->
        buildList {
            while (cursor.moveToNext()) {
                val time = cursor.long("created")
                val data = decrypt(cursor.getBlob(cursor.getColumnIndexOrThrow("payload")), "trace:$event:$time")
                add(TraceStep(time, data.getString("stage"), data.getString("detail")))
            }
        }
    }

    private fun traceSecrets(): List<String>? = try {
        val secrets = destinations().map { it.token }.toMutableList()
        db.query("queue", null, null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) secrets.add(payload(cursor).getString("sign"))
        }
        secrets
    } catch (_: Exception) {
        runCatching { recordCaptureFailure() }
        null // Fail closed for diagnostics, never interfere with the payment result.
    }

    internal fun recordAttempt(pending: Pending, attempt: Int, time: Long) = synchronized(lock) {
        val secrets = traceSecrets()
        val destination = if (secrets == null) "[tujuan disembunyikan]" else Core.safeDestination(pending.data.optString("endpoint"), secrets)
        addTrace(pending.event, "attempt", "Percobaan $attempt dimulai", destination + "\nJika proses terputus, hasil pengiriman belum diketahui.", time = time)
    }

    internal fun recordCancelled(pending: Pending) = synchronized(lock) {
        addTrace(pending.event, "cancelled", "Dibatalkan sebelum dikirim", "Pemantauan atau tujuan berubah. Tidak ada data dikirim pada percobaan ini.")
    }

    internal fun recordResponse(pending: Pending, attempt: Int, code: Int?, duration: Long, category: String, status: Boolean?, outcome: String?) = synchronized(lock) {
        val secrets = traceSecrets()
        val validated = outcome?.takeIf { value -> value in setOf("credited", "ignored", "unmatched", "ambiguous", "review", "duplicate") && secrets != null && secrets.none { it.isNotEmpty() && value.contains(it, true) } }
        val detail = "Percobaan $attempt · ${duration.coerceAtLeast(0)} ms\n" +
            (code?.let { "HTTP $it\n" } ?: "Belum ada kode HTTP\n") + category +
            (status?.let { "\nstatus: $it" } ?: "") + (validated?.let { "\noutcome: $it" } ?: "")
        // Only allowlisted fields and fixed categories, never raw response or exception messages.
        addTrace(pending.event, "response", "Hasil percobaan $attempt", detail)
    }

    fun deliveries(limit: Int = 100): List<Delivery> = synchronized(lock) {
        db.query("queue", null, null, null, null, null, "CASE WHEN state='done' THEN 1 ELSE 0 END, id DESC", limit.coerceIn(1, 500).toString()).use { cursor ->
            buildList {
                val failure = preferences.getLong("capture_failure", 0)
                if (failure > 0) add(Delivery(-1, "", "Panelin Notifikasi", "", "", "", failure, "attention", "", 0, "Penyimpanan notifikasi gagal; periksa ruang penyimpanan dan konfigurasi perangkat"))
                while (cursor.moveToNext()) {
                    val payload = try { payload(cursor) } catch (_: Exception) { null }
                    add(Delivery(cursor.long("id"), payload?.optString("pkg").orEmpty(), payload?.optString("label").orEmpty(),
                        payload?.optString("title").orEmpty(), payload?.optString("text").orEmpty(), payload?.optString("bigtext").orEmpty(),
                        cursor.long("created"), if (payload == null) "attention" else cursor.string("state"), cursor.string("outcome"),
                        cursor.int("attempts"), if (payload == null) "Data terenkripsi tidak dapat dibuka" else cursor.string("detail"),
                        runCatching { traces(cursor.string("event")) }.getOrElse { listOf(TraceStep(cursor.long("created"), "Jejak tidak tersedia", "Catatan terenkripsi tidak dapat dibuka")) }))
                }
            }
        }.let { payments ->
            val skipped = db.query("trace", null, "kind='skip'", null, null, null, "id DESC", limit.coerceIn(1, 500).toString()).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val time = cursor.long("created")
                        val event = cursor.string("event")
                        val data = runCatching { decrypt(cursor.getBlob(cursor.getColumnIndexOrThrow("payload")), "trace:$event:$time") }.getOrNull() ?: continue
                        add(Delivery(-cursor.long("id") - 1, data.optString("pkg"), data.optString("label"), "", "", "", time, "skipped", data.optString("reason"), 0, data.getString("detail"), listOf(TraceStep(time, "Notifikasi terbaca", "Isi tidak disimpan"), TraceStep(time, "Dilewati di HP", data.getString("detail")))))
                    }
                }
            }
            (payments + skipped).sortedByDescending { it.createdAt }
        }
    }

    fun snapshot(): Snapshot = synchronized(lock) {
        val start = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        db.rawQuery("""SELECT COALESCE(SUM(CASE WHEN created>=? THEN 1 ELSE 0 END),0),
            COALESCE(SUM(CASE WHEN outcome='credited' THEN 1 ELSE 0 END),0),
            COALESCE(SUM(CASE WHEN state IN ('queued','sending') THEN 1 ELSE 0 END),0),
            COALESCE(SUM(CASE WHEN state IN ('attention','held') OR outcome IN ('unmatched','ambiguous','review') THEN 1 ELSE 0 END),0),
            COALESCE(MAX(created),0), COALESCE(MAX(sent_at),0) FROM queue""", arrayOf(start.toString())).use {
            it.moveToFirst()
            Snapshot(it.getInt(0), it.getInt(1), it.getInt(2), it.getInt(3) + if (preferences.getLong("capture_failure", 0) > 0) 1 else 0, it.getLong(4), it.getLong(5))
        }
    }

    /** Held/pending records are not history and cannot be accidentally deleted here. */
    fun clearHistory() = synchronized(lock) {
        transaction {
            db.delete("trace", "kind='skip' OR event IN (SELECT event FROM queue WHERE state IN ('done','attention'))", null)
            db.delete("queue", "state IN ('done','attention')", null)
        }
    }

    internal data class Pending(val id: Long, val event: String, val data: JSONObject, val attempts: Int)

    internal fun claim(now: Long): Pending? = synchronized(lock) {
        if (!enabled) return@synchronized null
        var result: Pending? = null
        transaction {
            db.rawQuery("SELECT event FROM queue WHERE state='sending' AND lease_until<=?", arrayOf(now.toString())).use { cursor ->
                while (cursor.moveToNext()) addTrace(cursor.getString(0), "recovery", "Percobaan terputus", "Hasil sebelumnya belum diketahui. Antrean dipulihkan.")
            }
            db.execSQL("UPDATE queue SET state='queued', lease_until=0 WHERE state='sending' AND lease_until<=?", arrayOf(now))
            db.query("queue", null, "state='queued' AND next_at<=?", arrayOf(now.toString()), null, null, "id ASC").use { cursor ->
                while (cursor.moveToNext()) {
                    val payload = try { payload(cursor) } catch (_: Exception) {
                        finish(cursor.long("id"), "attention", "", "Data terenkripsi tidak dapat dibuka", now)
                        addTrace(cursor.string("event"), "terminal", "Perlu perhatian", "Data terenkripsi tidak dapat dibuka")
                        continue
                    }
                    val destination = findDestination(payload.getString("pkg"))
                    if (destination == null || destination.endpoint != payload.getString("endpoint") || destination.token != payload.getString("sign")) {
                        finish(cursor.long("id"), "held", "", "Konfigurasi tujuan berbeda; antrean ditahan", now)
                        addTrace(cursor.string("event"), "held", "Antrean ditahan", "Konfigurasi tujuan berbeda")
                        continue
                    }
                    if (!destination.enabled) continue
                    val id = cursor.long("id")
                    if (cursor.int("attempts") >= 8) {
                        finish(id, "attention", "", "Batas percobaan tercapai; periksa mutasi sebelum mengirim ulang", now)
                        addTrace(cursor.string("event"), "terminal", "Perlu perhatian", "Batas percobaan tercapai. Hasil sebelumnya belum diketahui. Periksa mutasi.")
                        continue
                    }
                    val attempts = cursor.int("attempts") + 1
                    db.execSQL("UPDATE queue SET state='sending', attempts=?, lease_until=? WHERE id=?", arrayOf(attempts, now + 180_000, id))
                    result = Pending(id, cursor.string("event"), payload, attempts)
                    break
                }
            }
        }
        result
    }

    internal fun maySend(pending: Pending): Boolean = synchronized(lock) {
        if (!enabled) return@synchronized false
        val destination = findDestination(pending.data.getString("pkg")) ?: return@synchronized false
        if (!destination.enabled || destination.endpoint != pending.data.getString("endpoint") || destination.token != pending.data.getString("sign")) return@synchronized false
        db.rawQuery("SELECT state FROM queue WHERE id=?", arrayOf(pending.id.toString())).use { it.moveToFirst() && it.getString(0) == "sending" }
    }

    internal fun release(pending: Pending) = synchronized(lock) {
        db.execSQL("UPDATE queue SET state='queued', attempts=MAX(attempts-1,0), lease_until=0 WHERE id=? AND state='sending'", arrayOf(pending.id))
    }

    internal fun complete(pending: Pending, outcome: String, detail: String, now: Long) = synchronized(lock) {
        finish(pending.id, if (outcome in setOf("attention", "review", "ambiguous", "unmatched")) "attention" else "done", outcome, detail, now)
        addTrace(pending.event, "terminal", "Hasil akhir", detail, time = now)
    }

    internal fun retry(pending: Pending, next: Long, detail: String) = synchronized(lock) {
        // A concurrent edit may have held this record: do not undo that decision.
        val changed = db.update("queue", ContentValues().apply {
            put("state", "queued"); put("next_at", next); put("lease_until", 0); put("detail", detail)
        }, "id=? AND state='sending'", arrayOf(pending.id.toString()))
        if (changed > 0) addTrace(pending.event, "retry", "Dijadwalkan ulang", detail + "\nWaktu berikutnya: " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(next)))
    }

    internal fun nextDue(): Long? = synchronized(lock) {
        if (!enabled) return@synchronized null
        // Filter by current opt-in so a disabled route does not cause a busy retry loop.
        val routes = destinations().filter { it.enabled }.map { route(it.packageName) }.toSet()
        var earliest: Long? = null
        db.rawQuery("SELECT route,next_at,lease_until,state FROM queue WHERE state IN ('queued','sending')", null).use { cursor ->
            while (cursor.moveToNext()) if (cursor.getString(0) in routes) {
                val due = if (cursor.getString(3) == "sending") cursor.getLong(2) else cursor.getLong(1)
                earliest = earliest?.let { minOf(it, due) } ?: due
            }
        }
        earliest
    }

    private fun finish(id: Long, state: String, outcome: String, detail: String, now: Long) {
        db.update("queue", ContentValues().apply {
            put("state", state); put("outcome", outcome); put("detail", detail); put("lease_until", 0)
            if (state == "done" || outcome in setOf("review", "ambiguous", "unmatched")) put("sent_at", now)
        }, "id=?", arrayOf(id.toString()))
    }

    private fun findDestination(packageName: String): Destination? {
        val route = route(packageName)
        return db.query("destinations", arrayOf("payload"), "route=?", arrayOf(route), null, null, null).use {
            if (it.moveToFirst()) destination(decrypt(it.getBlob(0), "destination:$route")) else null
        }
    }

    private fun destination(json: JSONObject) = Destination(json.getString("pkg"), json.getString("label"), json.getString("endpoint"), json.getString("sign"), json.getBoolean("enabled") && json.optInt("relay_consent") == 2)
    private fun destinationJson(value: Destination) = JSONObject().put("pkg", value.packageName).put("label", value.label).put("endpoint", value.endpoint).put("sign", value.token).put("enabled", value.enabled).put("relay_consent", 2)
    private fun route(packageName: String) = Core.eventIdentity(packageName)
    private fun payload(cursor: Cursor) = decrypt(cursor.getBlob(cursor.getColumnIndexOrThrow("payload")), "queue:${cursor.string("event")}")
    private fun Cursor.string(column: String) = getString(getColumnIndexOrThrow(column))
    private fun Cursor.long(column: String) = getLong(getColumnIndexOrThrow(column))
    private fun Cursor.int(column: String) = getInt(getColumnIndexOrThrow(column))
    private inline fun transaction(block: () -> Unit) {
        db.beginTransaction()
        try { block(); db.setTransactionSuccessful() } finally { db.endTransaction() }
    }

    companion object {
        private val lock = Any()
        private var helper: SQLiteOpenHelper? = null
        private fun database(context: Context): SQLiteOpenHelper = synchronized(lock) {
            helper ?: object : SQLiteOpenHelper(context, "panelin-secure.db", null, 2) {
                override fun onConfigure(db: SQLiteDatabase) {
                    db.rawQuery("PRAGMA secure_delete=ON", null).use { check(it.moveToFirst() && it.getInt(0) == 1) }
                }
                override fun onCreate(db: SQLiteDatabase) {
                    db.execSQL("CREATE TABLE destinations(route TEXT PRIMARY KEY, payload BLOB NOT NULL)")
                    db.execSQL("CREATE TABLE seen(event TEXT PRIMARY KEY)")
                    db.execSQL("CREATE TABLE queue(id INTEGER PRIMARY KEY AUTOINCREMENT, event TEXT UNIQUE NOT NULL, route TEXT NOT NULL, payload BLOB NOT NULL, created INTEGER NOT NULL, state TEXT NOT NULL, outcome TEXT NOT NULL, attempts INTEGER NOT NULL, detail TEXT NOT NULL, next_at INTEGER NOT NULL, lease_until INTEGER NOT NULL, sent_at INTEGER NOT NULL)")
                    db.execSQL("CREATE INDEX queue_due ON queue(state,next_at)")
                    createTrace(db)
                }
                override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
                    if (oldVersion < 2) createTrace(db)
                }
                private fun createTrace(db: SQLiteDatabase) {
                    db.execSQL("CREATE TABLE trace(id INTEGER PRIMARY KEY AUTOINCREMENT, event TEXT NOT NULL, kind TEXT NOT NULL, created INTEGER NOT NULL, payload BLOB NOT NULL)")
                    db.execSQL("CREATE INDEX trace_event ON trace(event,id)")
                }
            }.also { helper = it }
        }
        private fun key(): SecretKey {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            (store.getKey("panelin.forwarding.v1", null) as? SecretKey)?.let { return it }
            return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
                init(KeyGenParameterSpec.Builder("panelin.forwarding.v1", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setKeySize(256).build())
            }.generateKey()
        }
        private fun encrypt(value: String, aad: String): ByteArray {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
            return byteArrayOf(1) + cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        }
        private fun decrypt(value: ByteArray, aad: String): JSONObject {
            require(value.size >= 29 && value[0] == 1.toByte())
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, value.copyOfRange(1, 13)))
            cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
            return JSONObject(String(cipher.doFinal(value.copyOfRange(13, value.size)), Charsets.UTF_8))
        }
    }
}

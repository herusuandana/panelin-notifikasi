package id.panelin.notifikasi

import android.app.Activity
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import java.io.File
import java.net.URI
import java.security.MessageDigest
import javax.net.ssl.HttpsURLConnection

// BEGIN PURE UPDATE POLICY
internal data class UpdateVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<UpdateVersion> {
    override fun compareTo(other: UpdateVersion) = compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
}
internal data class UpdateRelease(val tag: String, val name: String, val url: String, val sha256: String, val size: Long, val prerelease: Boolean, val notes: String)
internal object UpdatePolicy {
    const val PIN = "70f30880391bd102c4392f1ef489fc58bd6f90d15a405a1ecbd068b22fe634e8"
    const val MAX_APK = 100L * 1024 * 1024
    fun version(tag: String): UpdateVersion? {
        if (!Regex("v(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)").matches(tag)) return null
        val parts = tag.drop(1).split('.').map { it.toIntOrNull() ?: return null }
        return UpdateVersion(parts[0], parts[1], parts[2])
    }
    fun release(tag: String, name: String, url: String, digest: String, size: Long, prerelease: Boolean, draft: Boolean, notes: String): UpdateRelease? {
        version(tag) ?: return null
        if (draft || name != "panelin-notifikasi-${tag.drop(1)}-release.apk" || size !in 1..MAX_APK || !Regex("sha256:[a-fA-F0-9]{64}").matches(digest)) return null
        if (url != "https://github.com/herusuandana/panelin-notifikasi/releases/download/$tag/$name") return null
        return UpdateRelease(tag, name, url, digest.drop(7).lowercase(), size, prerelease, notes.take(4000).filter { !it.isISOControl() || it == '\n' || it == '\t' })
    }
    fun select(releases: List<UpdateRelease>, installedName: String, trials: Boolean): UpdateRelease? {
        val installed = version("v$installedName") ?: return null
        return releases.filter { (trials || !it.prerelease) && version(it.tag)!! > installed }.maxByOrNull { version(it.tag)!! }
    }
    fun downloadUrl(value: String): Boolean = try {
        val uri = URI(value)
        uri.scheme == "https" && uri.host in setOf("github.com", "release-assets.githubusercontent.com") && uri.port == -1 && uri.rawUserInfo == null && uri.rawFragment == null
    } catch (_: Exception) { false }
    fun archiveAllowed(pkg: String?, currentPkg: String, code: Long, installedCode: Long, flags: Int, installedFlags: Int, signers: Set<String>, installedSigners: Set<String>) =
        pkg == currentPkg && code > installedCode && flags and 2 == 0 && installedFlags and 2 == 0 && signers == setOf(PIN) && installedSigners == signers
    fun resumeInstall(pending: Boolean, permission: Boolean) = pending && permission
}
// END PURE UPDATE POLICY

internal data class UpdateState(
    val checking: Boolean = false,
    val manual: Boolean = false,
    val release: UpdateRelease? = null,
    val prompt: Boolean = false,
    val downloading: Boolean = false,
    val progress: Float = 0f,
    val message: String? = null,
    val error: Boolean = false,
    val trials: Boolean = false,
    val ready: Boolean = false
)

/** Activity-owned. Call check(false) on real foregrounds, resume() onResume, close() onDestroy. */
internal class UpdateController(private val activity: Activity) {
    private val preferences = activity.getSharedPreferences("updates", Activity.MODE_PRIVATE)
    private val mutable = MutableStateFlow(UpdateState(trials = preferences.getBoolean("prerelease", false)))
    val state = mutable.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var job: Job? = null
    @Volatile private var connection: HttpsURLConnection? = null
    private var readyFile: File? = null
    private var pendingPermission = false
    private var externalActivity = false
    private var installing = false
    private var handedToInstaller = false
    private var closed = false
    private val directory get() = File(activity.filesDir, "updates")

    private val cleanup = scope.launch(Dispatchers.IO) {
            val files = directory.listFiles().orEmpty().sortedByDescending { it.lastModified() }
            val retained = files.firstOrNull { it.extension == "apk" && System.currentTimeMillis() - it.lastModified() < 86400000L }
            files.filter { it != retained }.forEach { it.delete() }
    }

    fun setTrials(value: Boolean) {
        if (closed || job?.isCompleted == false || externalActivity || installing) return
        preferences.edit().putBoolean("prerelease", value).apply()
        mutable.value = UpdateState(trials = value)
        check(true)
    }

    fun check(manual: Boolean = false) {
        if (closed || job?.isCompleted == false || externalActivity || installing || mutable.value.prompt || mutable.value.ready) return
        mutable.value = mutable.value.copy(checking = true, manual = manual, message = null, error = false)
        job = scope.launch {
            try {
                val candidate = withContext(Dispatchers.IO) { withTimeout(90000) { fetchRelease(mutable.value.trials) } }
                mutable.value = mutable.value.copy(checking = false, release = candidate, prompt = candidate != null,
                    message = if (manual && candidate == null) "Belum ada pembaruan di kanal ini." else null)
            } catch (_: TimeoutCancellationException) {
                mutable.value = mutable.value.copy(checking = false, message = if (manual) "Pemeriksaan terlalu lama. Coba lagi." else null, error = manual)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: Exception) {
                mutable.value = mutable.value.copy(checking = false, message = if (manual) "Belum bisa memeriksa pembaruan. Coba lagi nanti." else null, error = manual)
            } finally { mutable.value = mutable.value.copy(checking = false) }
        }
    }

    private suspend fun fetchRelease(trials: Boolean): UpdateRelease? {
        val items = mutableListOf<UpdateRelease>()
        // ponytail: scan at most 100 releases; an incomplete catalog fails closed, never claims current.
        for (page in 1..5) {
            currentCoroutineContext().ensureActive()
            val url = "https://api.github.com/repos/herusuandana/panelin-notifikasi/releases?per_page=20&page=$page"
            val body = request(url, false) { conn ->
                conn.inputStream.use { input ->
                    val bytes = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val n = input.read(buffer)
                        if (n < 0) break
                        require(bytes.size() + n <= 1024 * 1024)
                        bytes.write(buffer, 0, n)
                    }
                    bytes.toString("UTF-8")
                }
            }
            val array = JSONArray(body)
            require(array.length() <= 20)
            for (i in 0 until array.length()) {
                val release = array.getJSONObject(i)
                if (!release.has("draft") || !release.has("prerelease")) continue
                val assets = release.optJSONArray("assets") ?: continue
                require(assets.length() <= 40)
                for (a in 0 until assets.length()) {
                    val asset = assets.getJSONObject(a)
                    UpdatePolicy.release(release.optString("tag_name"), asset.optString("name"), asset.optString("browser_download_url"), asset.optString("digest"), asset.optLong("size", -1), release.getBoolean("prerelease"), release.getBoolean("draft"), release.optString("body"))?.let(items::add)
                }
            }
            if (array.length() < 20) return UpdatePolicy.select(items, installed().versionName.orEmpty(), trials)
        }
        error("Incomplete release catalog")
    }

    private suspend fun <T> request(initial: String, download: Boolean, consume: suspend (HttpsURLConnection) -> T): T {
        var url = initial
        repeat(6) { hop ->
            currentCoroutineContext().ensureActive()
            if (download) require(UpdatePolicy.downloadUrl(url))
            else require(url.startsWith("https://api.github.com/repos/herusuandana/panelin-notifikasi/releases?per_page=20&page="))
            val conn = java.net.URL(url).openConnection() as HttpsURLConnection
            connection = conn
            try {
                conn.instanceFollowRedirects = false
                conn.connectTimeout = 15000
                conn.readTimeout = 20000
                conn.useCaches = false
                conn.setRequestProperty("User-Agent", "Panelin-Notifikasi-Updater")
                conn.setRequestProperty("Accept", if (download) "application/octet-stream" else "application/vnd.github+json")
                conn.setRequestProperty("Accept-Encoding", "identity")
                val status = conn.responseCode
                if (download && status in setOf(301, 302, 303, 307, 308)) {
                    require(hop < 5)
                    url = URI(url).resolve(conn.getHeaderField("Location") ?: error("Missing redirect")).toString()
                } else {
                    require(status == 200)
                    return consume(conn)
                }
            } finally { connection = null; conn.disconnect() }
        }
        error("Too many redirects")
    }

    fun download() {
        val release = mutable.value.release ?: return
        if (closed || job?.isCompleted == false || externalActivity || installing) return
        if (readyFile != null) { install(); return }
        mutable.value = mutable.value.copy(downloading = true, progress = 0f, message = null, error = false)
        job = scope.launch {
            var completed: File? = null
            try {
                cleanup.join()
                completed = withContext(Dispatchers.IO) {
                    require(directory.isDirectory || directory.mkdirs())
                    directory.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 86400000L }?.forEach { it.delete() }
                    val partial = File.createTempFile("update-", ".part", directory)
                    val final = File(directory, partial.nameWithoutExtension + ".apk")
                    try {
                        withTimeout(180000) { request(release.url, true) { conn ->
                            require(conn.contentLengthLong == -1L || conn.contentLengthLong == release.size)
                            conn.inputStream.use { input -> partial.outputStream().use { output ->
                                var count = 0L
                                var reported = -1
                                val buffer = ByteArray(32768)
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    val n = input.read(buffer)
                                    if (n < 0) break
                                    count += n
                                    require(count <= release.size && count <= UpdatePolicy.MAX_APK)
                                    output.write(buffer, 0, n)
                                    val percent = (count * 100 / release.size).toInt()
                                    if (percent != reported) {
                                        reported = percent
                                        mutable.value = mutable.value.copy(progress = count.toFloat() / release.size)
                                    }
                                }
                                output.fd.sync()
                                require(count == release.size)
                            } }
                        } }
                        validateFile(partial, release)
                        require(partial.setReadOnly())
                        require(partial.renameTo(final))
                        currentCoroutineContext().ensureActive()
                        completed = final
                        final
                    } catch (failure: Throwable) { final.delete(); throw failure
                    } finally { partial.delete() }
                }
                readyFile = completed
                mutable.value = mutable.value.copy(downloading = false, ready = true)
                install()
            } catch (_: TimeoutCancellationException) {
                completed?.delete()
                mutable.value = mutable.value.copy(downloading = false, ready = false, error = true, message = "Unduhan terlalu lama. Coba lagi.")
            } catch (cancelled: CancellationException) {
                completed?.delete()
                throw cancelled
            } catch (_: Exception) {
                completed?.delete()
                mutable.value = mutable.value.copy(downloading = false, ready = false, error = true, message = "Pembaruan belum bisa dipasang. Coba lagi.")
            } finally { mutable.value = mutable.value.copy(downloading = false) }
        }
    }

    internal fun validateFile(file: File, release: UpdateRelease) {
        require(file.isFile && file.length() == release.size && file.length() <= UpdatePolicy.MAX_APK)
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(32768)
            var count = 0L
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                count += n
                require(count <= release.size)
                digest.update(buffer, 0, n)
            }
            require(count == release.size)
        }
        require(hex(digest.digest()) == release.sha256)
        val archive = activity.packageManager.getPackageArchiveInfo(file.path, signatureFlags()) ?: error("Unreadable archive")
        requireArchive(archive, installed())
        require(archive.versionName == release.tag.drop(1))
        // PackageManager exposes metadata/signers, not a complete cryptographic APK verifier.
        // Android's user-confirmed package installer is the final signature verifier.
    }

    internal fun requireArchive(archive: PackageInfo, current: PackageInfo) {
        require(UpdatePolicy.archiveAllowed(archive.packageName, activity.packageName, code(archive), code(current), archive.applicationInfo?.flags ?: 2, current.applicationInfo?.flags ?: 2, signers(archive), signers(current)))
    }
    @Suppress("DEPRECATION") private fun installed() = activity.packageManager.getPackageInfo(activity.packageName, signatureFlags())
    @Suppress("DEPRECATION") private fun signatureFlags() = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
    @Suppress("DEPRECATION") private fun code(info: PackageInfo) = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()
    @Suppress("DEPRECATION") private fun signers(info: PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures
        return signatures?.map { hex(MessageDigest.getInstance("SHA-256").digest(it.toByteArray())) }?.toSet().orEmpty()
    }
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun install() {
        val file = readyFile ?: return
        if (!activity.packageManager.canRequestPackageInstalls()) {
            try {
                pendingPermission = true
                externalActivity = true
                activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}")))
            } catch (_: Exception) {
                pendingPermission = false; externalActivity = false
                mutable.value = mutable.value.copy(error = true, message = "Izin pemasangan belum tersedia. Coba lagi.")
            }
            return
        }
        launchInstaller(file)
    }

    private fun launchInstaller(file: File) {
        val release = mutable.value.release ?: return
        if (installing || closed) return
        installing = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) { validateFile(file, release) }
                val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.updates", file)
                @Suppress("DEPRECATION")
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.clipData = ClipData.newRawUri("Pembaruan Panelin", uri)
                externalActivity = true
                activity.startActivity(intent)
                handedToInstaller = true
                mutable.value = mutable.value.copy(prompt = false, message = "Lanjutkan pemasangan di layar Android.")
            } catch (_: Exception) {
                externalActivity = false
                mutable.value = mutable.value.copy(error = true, message = "Pemasangan belum bisa dibuka. Coba lagi.")
            } finally { installing = false }
        }
    }

    fun resume() {
        if (!externalActivity) return
        externalActivity = false
        val pending = pendingPermission
        pendingPermission = false
        if (UpdatePolicy.resumeInstall(pending, activity.packageManager.canRequestPackageInstalls())) {
            readyFile?.let(::launchInstaller)
        } else if (pending) {
            mutable.value = mutable.value.copy(message = "Izinkan pemasangan untuk melanjutkan.", error = true)
        } else if (handedToInstaller) {
            handedToInstaller = false
            readyFile?.delete(); readyFile = null
            mutable.value = UpdateState(trials = mutable.value.trials)
            check(false)
        }
    }

    fun dismiss() {
        if (closed || job?.isCompleted == false || externalActivity || installing) return
        readyFile?.delete(); readyFile = null
        mutable.value = UpdateState(trials = mutable.value.trials)
    }
    fun cancel() {
        job?.cancel(); connection?.disconnect()
        mutable.value = mutable.value.copy(downloading = false, message = "Unduhan dibatalkan.", error = false)
    }
    fun close() {
        closed = true
        scope.cancel(); connection?.disconnect()
        if (!handedToInstaller) { readyFile?.delete(); readyFile = null }
        // A final read-only APK stays available to an already-open Android installer.
        // ponytail: process death discards pending consent; a fresh user click downloads again.
    }
}

package id.panelin.notifikasi

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.net.URL
import java.net.URLStreamHandler
import java.net.SocketTimeoutException
import java.security.cert.Certificate
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.net.ssl.HttpsURLConnection

/** Run this class alone: URL's test-only factory is process-global. No real network. */
class UpdateSecurityTest {
    @Test fun realControllerChecksEveryForegroundSilentlyAndRejectsUnsafeArchives() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val preferences = context.getSharedPreferences("updates", 0)
        val previousTrial = preferences.getBoolean("prerelease", false)
        preferences.edit().putBoolean("prerelease", false).commit()
        val requests = AtomicInteger()
        val response = AtomicReference("[]")
        URL.setURLStreamHandlerFactory { protocol ->
            if (protocol != "https") null else object : URLStreamHandler() {
                override fun openConnection(url: URL): HttpsURLConnection {
                    check(url.host == "api.github.com")
                    check(url.path == "/repos/herusuandana/panelin-notifikasi/releases")
                    requests.incrementAndGet()
                    return object : HttpsURLConnection(url) {
                        override fun connect() = Unit
                        override fun disconnect() = Unit
                        override fun usingProxy() = false
                        override fun getCipherSuite() = "TLS_AES_128_GCM_SHA256"
                        override fun getLocalCertificates(): Array<Certificate>? = null
                        override fun getServerCertificates(): Array<Certificate> = emptyArray()
                        override fun getResponseCode(): Int {
                            if (response.get() == "TIMEOUT") throw SocketTimeoutException("secret fixture detail")
                            return 200
                        }
                        override fun getInputStream() = ByteArrayInputStream(response.get().toByteArray())
                    }
                }
            }
        }
        lateinit var controller: UpdateController
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val field = MainActivity::class.java.getDeclaredField("updater")
                    field.isAccessible = true
                    controller = field.get(activity) as UpdateController
                }
                fun awaitCheck() {
                    val deadline = System.currentTimeMillis() + 10000
                    while (controller.state.value.checking && System.currentTimeMillis() < deadline) Thread.sleep(30)
                    instrumentation.waitForIdleSync()
                    assertFalse("check timeout", controller.state.value.checking)
                }
                awaitCheck()
                assertNull(controller.state.value.message)
                assertFalse(controller.state.value.prompt)
                repeat(2) {
                    val before = requests.get()
                    scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED)
                    scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED)
                    awaitCheck()
                    assertEquals(before + 1, requests.get())
                    assertFalse(controller.state.value.prompt)
                    assertNull(controller.state.value.message)
                }
                // Cancelling Android's installer must release ready state and check again.
                val abandoned = java.io.File(context.filesDir, "updates/installer-cancel-fixture.apk")
                abandoned.parentFile!!.mkdirs(); abandoned.writeText("fixture")
                val beforeReturn = requests.get()
                scenario.onActivity {
                    fun field(name: String, value: Any?) {
                        UpdateController::class.java.getDeclaredField(name).apply { isAccessible = true }.set(controller, value)
                    }
                    field("readyFile", abandoned)
                    field("externalActivity", true)
                    field("handedToInstaller", true)
                    controller.resume()
                }
                awaitCheck()
                assertEquals(beforeReturn + 1, requests.get())
                assertFalse(controller.state.value.ready)
                assertFalse(abandoned.exists())
                response.set("TIMEOUT")
                scenario.onActivity { controller.check(false) }; awaitCheck()
                assertNull(controller.state.value.message)
                assertFalse(controller.state.value.prompt)
                scenario.onActivity { controller.check(true) }; awaitCheck()
                assertTrue(controller.state.value.error)
                assertFalse(controller.state.value.message.orEmpty().contains("secret"))
                val digest = "a".repeat(64)
                val trial = """[{"tag_name":"v999.0.0","draft":false,"prerelease":true,"body":"Catatan uji","assets":[{"name":"panelin-notifikasi-999.0.0-release.apk","browser_download_url":"https://github.com/herusuandana/panelin-notifikasi/releases/download/v999.0.0/panelin-notifikasi-999.0.0-release.apk","digest":"sha256:$digest","size":100}]}]"""
                response.set(trial.replace("\"prerelease\":true", "\"prerelease\":false").replace("sha256:$digest", "sha256:bad"))
                scenario.onActivity { controller.check(false) }; awaitCheck()
                assertFalse(controller.state.value.prompt)
                response.set("x".repeat(1024 * 1024 + 1))
                scenario.onActivity { controller.check(false) }; awaitCheck()
                assertFalse(controller.state.value.prompt)
                assertNull(controller.state.value.message)
                val stable = trial.replace("\"prerelease\":true", "\"prerelease\":false")
                for (version in listOf(BuildConfig.VERSION_NAME, "0.0.1")) {
                    response.set(stable.replace("999.0.0", version))
                    scenario.onActivity { controller.check(false) }; awaitCheck()
                    assertFalse(controller.state.value.prompt)
                    assertNull(controller.state.value.message)
                }
                response.set(stable)
                scenario.onActivity { controller.check(false) }; awaitCheck()
                assertTrue(controller.state.value.prompt)
                scenario.onActivity { controller.dismiss() }
                response.set(trial)
                scenario.onActivity { controller.check(false) }; awaitCheck()
                assertFalse(controller.state.value.prompt)
                scenario.onActivity { controller.setTrials(true) }; awaitCheck()
                assertTrue(controller.state.value.prompt)
                val device = UiDevice.getInstance(instrumentation)
                assertNotNull(device.wait(Until.findObject(By.text("Versi 999.0.0 tersedia")), 5000))
                assertNotNull(device.findObject(By.text("Nanti saja")))
                scenario.onActivity { activity ->
                    @Suppress("DEPRECATION")
                    val current = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_SIGNATURES)
                    fun rejected(archive: PackageInfo) {
                        try { controller.requireArchive(archive, current); fail("unsafe archive accepted") } catch (_: IllegalArgumentException) { }
                    }
                    val fake = PackageInfo().apply {
                        packageName = "wrong.package"
                        versionCode = Int.MAX_VALUE
                        applicationInfo = ApplicationInfo().apply { flags = 0 }
                    }
                    rejected(fake)
                    fake.packageName = activity.packageName
                    rejected(fake) // No signature, including on API26.
                    fake.applicationInfo!!.flags = ApplicationInfo.FLAG_DEBUGGABLE
                    rejected(fake)
                    val invalid = java.io.File.createTempFile("update-fixture-", ".apk", activity.cacheDir)
                    try {
                        invalid.writeText("not an APK")
                        val release = UpdateRelease("v999.0.0", "fixture.apk", "", "a".repeat(64), invalid.length(), false, "")
                        try { controller.validateFile(invalid, release); fail("digest mismatch accepted") } catch (_: IllegalArgumentException) { }
                        val hash = java.security.MessageDigest.getInstance("SHA-256").digest(invalid.readBytes()).joinToString("") { "%02x".format(it.toInt() and 255) }
                        try { controller.validateFile(invalid, release.copy(sha256 = hash)); fail("malformed APK accepted") } catch (_: IllegalStateException) { }
                    } finally { invalid.delete() }
                    assertFalse(UpdatePolicy.resumeInstall(true, false))
                    assertFalse(UpdatePolicy.resumeInstall(false, true))
                    assertTrue(UpdatePolicy.resumeInstall(true, true))
                    controller.dismiss()
                }
            }
        } finally { preferences.edit().putBoolean("prerelease", previousTrial).commit() }
    }
}

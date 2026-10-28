package id.panelin.notifikasi

import org.junit.Assert.*
import org.junit.Test

class UpdatePolicyTest {
    private val digest = "sha256:" + "a".repeat(64)
    private fun release(tag: String = "v1.7.0", trial: Boolean = false, url: String = "https://github.com/herusuandana/panelin-notifikasi/releases/download/$tag/panelin-notifikasi-${tag.drop(1)}-release.apk", hash: String = digest, size: Long = 100) =
        UpdatePolicy.release(tag, "panelin-notifikasi-${tag.drop(1)}-release.apk", url, hash, size, trial, false, "notes")

    @Test fun exactVersionSyntaxOnly() {
        for (bad in listOf("1.7.0", "v01.7.0", "v1.7", "v1.7.0-rc1", "v1.7.0/", "v999999999999.0.0")) assertNull(UpdatePolicy.version(bad))
        assertNotNull(UpdatePolicy.version("v1.7.0"))
    }
    @Test fun versionsCompareNumericallyNotLexically() {
        assertTrue(UpdatePolicy.version("v1.10.0")!! > UpdatePolicy.version("v1.9.9")!!)
    }
    @Test fun stableChannelNeverOffersTrials() {
        assertNull(UpdatePolicy.select(listOf(release(trial = true)!!), "1.6.0", false))
        assertNotNull(UpdatePolicy.select(listOf(release(trial = true)!!), "1.6.0", true))
    }
    @Test fun highestVersionSelectedAndNoDowngradeOrSameVersion() {
        assertEquals("v1.10.0", UpdatePolicy.select(listOf(release("v1.7.0")!!, release("v1.10.0")!!), "1.6.0", false)?.tag)
        assertNull(UpdatePolicy.select(listOf(release()!!), "1.7.0", true))
        assertNull(UpdatePolicy.select(listOf(release()!!), "9.0.0", true))
        assertNull(UpdatePolicy.select(listOf(release()!!), "bad", true))
    }
    @Test fun exactRepositoryAssetPathAndHttpsRequired() {
        val good = release()!!.url
        for (url in listOf(good.replace("https:", "http:"), good.replace("github.com", "github.com.evil.test"), good.replace("github.com", "user@github.com"), "$good?x=1", "$good#x", good.replace("herusuandana", "other"), good.replace("v1.7.0/", "v1.8.0/"), good.replace("github.com", "github.com:443"))) assertNull(release(url = url))
    }
    @Test fun mandatoryDigestAndBoundedSize() {
        for (hash in listOf("", "a".repeat(64), "sha256:" + "z".repeat(64), digest + " ")) assertNull(release(hash = hash))
        for (size in listOf(0L, -1L, UpdatePolicy.MAX_APK + 1)) assertNull(release(size = size))
        assertNotNull(release())
    }
    @Test fun redirectsOnlyAllowExactHttpsHosts() {
        assertTrue(UpdatePolicy.downloadUrl("https://release-assets.githubusercontent.com/a?sig=fixture"))
        for (url in listOf("http://github.com/a", "https://api.github.com/a", "https://evil.test/a", "https://user@github.com/a", "https://github.com:444/a", "https://github.com/a#f")) assertFalse(UpdatePolicy.downloadUrl(url))
    }
    @Test fun archiveRequiresNewCodePackageReleaseAndPinnedInstalledSigner() {
        val pin = UpdatePolicy.PIN
        assertTrue(UpdatePolicy.archiveAllowed("id.panelin.notifikasi", "id.panelin.notifikasi", 9, 8, 0, 0, setOf(pin), setOf(pin)))
        assertFalse(UpdatePolicy.archiveAllowed("other", "id.panelin.notifikasi", 9, 8, 0, 0, setOf(pin), setOf(pin)))
        for (code in listOf(7L, 8L)) assertFalse(UpdatePolicy.archiveAllowed("id.panelin.notifikasi", "id.panelin.notifikasi", code, 8, 0, 0, setOf(pin), setOf(pin)))
        for (flags in listOf(2 to 0, 0 to 2)) assertFalse(UpdatePolicy.archiveAllowed("id.panelin.notifikasi", "id.panelin.notifikasi", 9, 8, flags.first, flags.second, setOf(pin), setOf(pin)))
        for (certs in listOf(emptySet(), setOf("bad"), setOf(pin, "bad"))) assertFalse(UpdatePolicy.archiveAllowed("id.panelin.notifikasi", "id.panelin.notifikasi", 9, 8, 0, 0, certs, setOf(pin)))
        assertFalse(UpdatePolicy.archiveAllowed("id.panelin.notifikasi", "id.panelin.notifikasi", 9, 8, 0, 0, setOf(pin), setOf("bad")))
    }
    @Test fun permissionResumeIsOneShotAndNeverInstallsWithoutPermission() {
        assertFalse(UpdatePolicy.resumeInstall(false, true))
        assertFalse(UpdatePolicy.resumeInstall(true, false))
        assertTrue(UpdatePolicy.resumeInstall(true, true))
    }
    @Test fun metadataNotesAreBounded() {
        val item = UpdatePolicy.release("v1.7.0", "panelin-notifikasi-1.7.0-release.apk", release()!!.url, digest, 100, false, false, "x".repeat(20000))!!
        assertTrue(item.notes.length <= 4000)
        assertNull(UpdatePolicy.release(item.tag, "wrong.apk", item.url, digest, 100, false, false, ""))
        assertNull(UpdatePolicy.release(item.tag, item.name, item.url, digest, 100, false, true, ""))
    }
}

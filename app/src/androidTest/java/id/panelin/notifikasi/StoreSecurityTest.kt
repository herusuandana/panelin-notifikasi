package id.panelin.notifikasi

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StoreSecurityTest {
    @Test fun credentialsSurviveReopenWithoutPlaintextPreferences() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val packageName = "id.panelin.testfixture"
        val token = "instrumentation-private-token-never-network"
        val destination = Destination(packageName, "Fixture", "https://client.example/callback/notification/1", token, false)
        try {
            AppStore(context).saveDestination(destination)
            assertEquals(destination, AppStore(context).destinations().single { it.packageName == packageName })
            context.databaseList().filter { it.startsWith("panelin") }.forEach { name ->
                val bytes = context.getDatabasePath(name).readBytes().toString(Charsets.ISO_8859_1)
                assertFalse("Token leaked into database", bytes.contains(token))
                assertFalse("Endpoint leaked into database", bytes.contains(destination.endpoint))
            }
            val preferences = context.dataDir.resolve("shared_prefs")
            if (preferences.exists()) preferences.walkTopDown().filter { it.isFile }.forEach {
                assertFalse("Token leaked into preferences", it.readText().contains(token))
            }
        } finally {
            AppStore(context).removeDestination(packageName)
        }
    }

    @Test fun oldDestinationRequiresFreshConsentWithoutLosingCredentials() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertFalse("Run offline", context.getSystemService(android.net.ConnectivityManager::class.java).activeNetworkInfo?.isConnected == true)
        val store = AppStore(context)
        val value = Destination("id.panelin.legacyconsent", "Legacy", "https://fixture.invalid/callback/notification/1", "fixture-token", true)
        try {
            store.enabled = false
            store.saveDestination(value)
            val route = Core.eventIdentity(value.packageName)
            val legacy = org.json.JSONObject().put("pkg", value.packageName).put("label", value.label)
                .put("endpoint", value.endpoint).put("sign", value.token).put("enabled", true)
            val encrypt = AppStore.Companion::class.java.getDeclaredMethod("encrypt", String::class.java, String::class.java).apply { isAccessible = true }
            val encrypted = encrypt.invoke(AppStore.Companion, legacy.toString(), "destination:$route") as ByteArray
            android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath("panelin-secure.db").path, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use {
                it.execSQL("UPDATE destinations SET payload=? WHERE route=?", arrayOf(encrypted, route))
            }
            assertEquals(value.copy(enabled = false), store.destinations().single { it.packageName == value.packageName })
            store.enabled = true
            assertNull(store.selected(value.packageName))
            assertFalse(store.enqueue(value.packageName, value.label, "Custom", "Saldo +1000", "", "legacy"))
            store.enabled = false
            store.saveDestination(value) // Explicit renewed consent retains the same credentials.
            assertEquals(value, store.destinations().single { it.packageName == value.packageName })
        } finally {
            store.enabled = false
            store.removeDestination(value.packageName)
        }
    }

    @Test fun insecureDestinationIsRejectedBeforePersistence() {
        val store = AppStore(InstrumentationRegistry.getInstrumentation().targetContext)
        for (endpoint in listOf("http://client.example/callback/notification/1", "https://client.example/callback/notification/1?token=bad", "https://user:password@client.example/callback/notification/1")) {
            var rejected = false
            try { store.saveDestination(Destination("id.panelin.testfixture", "Fixture", endpoint, "test-token", false)) }
            catch (_: IllegalArgumentException) { rejected = true }
            assertTrue("Unsafe endpoint must be rejected: $endpoint", rejected)
            assertFalse(store.destinations().any { it.packageName == "id.panelin.testfixture" })
        }
    }
}

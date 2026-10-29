package id.panelin.notifikasi

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.net.URLStreamHandler
import java.security.cert.Certificate
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

/** Fresh instrumentation process only. In-memory HTTPS, no external requests. */
class ConnectionCheckTest {
    @Test fun realRequestContainsOnlyTokenAndNeverFallsBackToPaymentCallback() {
        var code=200
        var response="{\"status\":true,\"outcome\":\"connected\"}"
        var requests=0
        val body=ByteArrayOutputStream()
        URL.setURLStreamHandlerFactory { protocol ->
            if(protocol!="https") null else object : URLStreamHandler() {
                override fun openConnection(url: URL): HttpsURLConnection {
                    assertEquals("example.invalid",url.host)
                    assertEquals("/callback/notification/12/check",url.path)
                    requests++
                    body.reset()
                    return object : HttpsURLConnection(url) {
                        override fun connect() = Unit
                        override fun disconnect() = Unit
                        override fun usingProxy() = false
                        override fun getCipherSuite() = "fixture"
                        override fun getLocalCertificates(): Array<Certificate>? = null
                        override fun getServerCertificates(): Array<Certificate> = emptyArray()
                        override fun getOutputStream(): java.io.OutputStream {
                            assertEquals("POST",requestMethod)
                            assertFalse(instanceFollowRedirects)
                            assertEquals("application/json; charset=utf-8",getRequestProperty("Content-Type"))
                            return body
                        }
                        override fun getResponseCode() = code
                        override fun getInputStream() = ByteArrayInputStream(response.toByteArray())
                        override fun getErrorStream() = ByteArrayInputStream(response.toByteArray())
                    }
                }
            }
        }
        val endpoint="https://example.invalid/callback/notification/12"
        assertEquals(Core.connectionResult(200,true,"connected"),checkConnection(endpoint,"fixture-token"))
        val sent=JSONObject(body.toString("UTF-8"))
        assertEquals(1,sent.length()); assertEquals("fixture-token",sent.getString("sign"))
        code=404
        assertEquals(Core.connectionResult(404,false,""),checkConnection(endpoint,"fixture-token"))
        assertEquals(2,requests) // No fallback to callback after old website returns 404.
        code=200;response="{\"status\":true,\"outcome\":\"credited\",\"secret\":\"fixture-token\"}"
        assertEquals(Core.connectionResult(200,true,"credited"),checkConnection(endpoint,"fixture-token"))
        response="x".repeat(5000)
        assertEquals(Core.connectionResult(200,false,""),checkConnection(endpoint,"fixture-token"))
        response="<html>fixture-token</html>"
        assertFalse(checkConnection(endpoint,"fixture-token").contains("fixture-token"))
    }
}

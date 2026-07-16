package dev.oranegonzales.ledgerrail.mobile.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ApiClientFactoryTest {
    private val factory = ApiClientFactory()

    @Test
    fun `normalization adds Retrofit's required trailing slash`() {
        assertEquals(
            "https://ledgerrail-core.onrender.com/",
            factory.normalizeServerUrl(" https://ledgerrail-core.onrender.com "),
        )
    }

    @Test
    fun `remote cleartext URLs are rejected`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            factory.normalizeServerUrl("http://ledgerrail.example.com")
        }

        assertEquals("Use an HTTPS server URL", failure.message)
    }

    @Test
    fun `credentials in URL are rejected`() {
        val failure = assertThrows(IllegalArgumentException::class.java) {
            factory.normalizeServerUrl("https://user:password@example.com")
        }

        assertEquals("Do not put credentials in the server URL", failure.message)
    }
}

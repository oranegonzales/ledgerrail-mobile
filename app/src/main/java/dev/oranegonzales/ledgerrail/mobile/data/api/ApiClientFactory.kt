package dev.oranegonzales.ledgerrail.mobile.data.api

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

internal data class ApiClient(
    val api: LedgerRailApi,
    val problemAdapter: JsonAdapter<ApiProblemDto>,
)

internal class ApiClientFactory {
    private val clients = ConcurrentHashMap<String, ApiClient>()

    fun client(serverUrl: String): ApiClient {
        val normalizedUrl = normalizeServerUrl(serverUrl)
        return clients.getOrPut(normalizedUrl) { buildClient(normalizedUrl) }
    }

    private fun buildClient(serverUrl: String): ApiClient {
        val moshi = Moshi.Builder()
            .add(BigDecimalJsonAdapter)
            .add(UuidJsonAdapter)
            .addLast(KotlinJsonAdapterFactory())
            .build()
        val httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(100, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request: Request = chain.request().newBuilder()
                    .header("Accept", "application/json, application/problem+json")
                    .header("User-Agent", "LedgerRail-Mobile/0.2")
                    .build()
                chain.proceed(request)
            }
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(httpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        return ApiClient(
            api = retrofit.create(LedgerRailApi::class.java),
            problemAdapter = moshi.adapter(ApiProblemDto::class.java),
        )
    }

    internal fun normalizeServerUrl(value: String): String {
        val candidate = value.trim().let { if (it.endsWith('/')) it else "$it/" }
        val uri = try {
            URI(candidate)
        } catch (exception: Exception) {
            throw IllegalArgumentException("Enter a valid server URL", exception)
        }
        val localHost = uri.host.equals("localhost", ignoreCase = true) || uri.host == "127.0.0.1"
        val https = uri.scheme.equals("https", ignoreCase = true)
        val localHttp = uri.scheme.equals("http", ignoreCase = true) && localHost
        require(https || localHttp) {
            "Use an HTTPS server URL"
        }
        require(!uri.host.isNullOrBlank()) { "Enter a valid server URL" }
        require(uri.userInfo == null) { "Do not put credentials in the server URL" }
        require(uri.rawQuery == null && uri.rawFragment == null) {
            "The server URL cannot contain a query or fragment"
        }
        return candidate
    }
}

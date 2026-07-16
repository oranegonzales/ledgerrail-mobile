package dev.oranegonzales.ledgerrail.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface LedgerRailApi {
    @GET("actuator/health/liveness")
    suspend fun health(): Response<HealthDto>

    @GET("api/v1/transfers")
    suspend fun transfers(
        @Header("X-Portfolio-Key") apiKey: String,
        @Query("accountId") accountId: String,
    ): Response<List<TransferDto>>

    @POST("api/v1/transfers")
    suspend fun createTransfer(
        @Header("X-Portfolio-Key") apiKey: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body request: CreateTransferDto,
    ): Response<TransferDto>

    @GET("api/v1/transfers/{id}/ledger-entries")
    suspend fun ledgerEntries(
        @Header("X-Portfolio-Key") apiKey: String,
        @Path("id") transferId: String,
    ): Response<List<LedgerEntryDto>>
}

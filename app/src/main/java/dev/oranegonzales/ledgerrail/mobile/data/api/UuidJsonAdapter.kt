package dev.oranegonzales.ledgerrail.mobile.data.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.util.UUID

internal object UuidJsonAdapter {
    @FromJson
    fun fromJson(value: String): UUID = UUID.fromString(value)

    @ToJson
    fun toJson(value: UUID): String = value.toString()
}

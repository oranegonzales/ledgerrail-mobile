package dev.oranegonzales.ledgerrail.mobile.data.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.math.BigDecimal

internal object BigDecimalJsonAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): BigDecimal = reader.nextString().toBigDecimal()

    @ToJson
    fun toJson(writer: JsonWriter, value: BigDecimal?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value)
        }
    }
}

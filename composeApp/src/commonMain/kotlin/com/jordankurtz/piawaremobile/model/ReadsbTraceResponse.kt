package com.jordankurtz.piawaremobile.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ReadsbTraceResponse(
    val icao: String,
    val timestamp: Double,
    val trace: List<TraceEntry>,
)

@Serializable(with = TraceEntrySerializer::class)
data class TraceEntry(
    val timeOffset: Double,
    val latitude: Double,
    val longitude: Double,
    val altitude: String?,
)

internal object TraceEntrySerializer : KSerializer<TraceEntry> {
    override val descriptor: SerialDescriptor = JsonArray.serializer().descriptor

    override fun serialize(
        encoder: Encoder,
        value: TraceEntry,
    ) = throw UnsupportedOperationException()

    override fun deserialize(decoder: Decoder): TraceEntry {
        val array = (decoder as JsonDecoder).decodeJsonElement().jsonArray
        return TraceEntry(
            timeOffset = array[0].jsonPrimitive.content.toDouble(),
            latitude = array[1].jsonPrimitive.content.toDouble(),
            longitude = array[2].jsonPrimitive.content.toDouble(),
            altitude = array.getOrNull(3)?.takeIf { it != JsonNull }?.jsonPrimitive?.content,
        )
    }
}

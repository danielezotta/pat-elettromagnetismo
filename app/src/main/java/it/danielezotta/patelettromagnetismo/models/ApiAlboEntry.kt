package it.danielezotta.patelettromagnetismo.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HtmlStrippingStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("HtmlStrippingString", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String {
        val encodedString = decoder.decodeString()
        return encodedString.replace(Regex("<[^>]*>"), "").replace(Regex("\\s+"), " ") // Remove HTML tags
    }

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }
}

@Serializable
data class ApiAlboEntry(

    @Serializable(with = HtmlStrippingStringSerializer::class)
    val IDATTO: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val DATAFINEVALIDITA: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val NOMEIMPRESA: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val INDIRIZZO: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val AREA: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val DATAINIZIOVALIDITA: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val IDATTIVITA: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val IDPRATICA: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val NUMEROAUTORIZZAZIONE: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val ATTO_STATO: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val DOCUMENTO: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val COMUNE_INDI: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val ALLEGATONOMEFILE: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val TIPODOC: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val DATAAUTORIZZAZIONE: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val gridKey: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val actions: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val ALLEGATODIMFILE: String,
    @Serializable(with = HtmlStrippingStringSerializer::class)
    val OGGETTODOC: String
)
package it.danielezotta.patelettromagnetismo.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
data class ArcGISFeature(
    val attributes: Map<String, JsonElement>,
    val geometry: Geometry? = null
)

@Serializable
data class Geometry(
    val x: Double,
    val y: Double
)

@Serializable
data class ArcGISResponse(
    val features: List<ArcGISFeature>,
    val fields: List<Field>? = null,
    val exceededTransferLimit: Boolean? = null,
    val objectIdFieldName: String? = null,
    val uniqueIdField: UniqueIdField? = null,
    val globalIdFieldName: String? = null,
    val hasZ: Boolean? = null,
    val hasM: Boolean? = null,
    val spatialReference: SpatialReference? = null,
    val geometryType: String? = null,
    val count: Int? = null,
    val error: ArcGISError? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class Field(
    val name: String,
    val type: String,
    val alias: String,
    val length: Int? = null,
    @Contextual val domain: Any? = null,
    @Contextual val defaultValue: Any? = null
)

@Serializable
data class UniqueIdField(
    val name: String,
    val isSystemMaintained: Boolean
)

@Serializable
data class SpatialReference(
    val wkid: Int,
    val latestWkid: Int? = null,
    val vcsWkid: Int? = null,
    val latestVcsWkid: Int? = null
)

@Serializable
data class ArcGISError(
    val code: Int,
    val message: String,
    val details: List<String>? = null
)

@Serializable
data class RTVImpianto(
    @SerialName("OBJECTID")
    val objectId: Int,
    @SerialName("Nome_Impianto")
    val nomeImpianto: String? = null,
    @SerialName("Comune")
    val comune: String? = null,
    @SerialName("Provincia")
    val provincia: String? = null,
    @SerialName("Indirizzo")
    val indirizzo: String? = null,
    @SerialName("Tipologia")
    val tipologia: String? = null,
    @SerialName("Frequenza")
    val frequenza: String? = null,
    @SerialName("Potenza")
    val potenza: String? = null,
    @SerialName("Tipo_Antenna_Sorgente")
    val tipoAntennaSorgente: String? = null,
    @SerialName("Coordinate_X_Sostegno")
    val coordinateXSostegno: Double? = null,
    @SerialName("Coordinate_Y_Sostegno")
    val coordinateYSostegno: Double? = null,
    @SerialName("Codice_Comune_Sostegno")
    val codiceComuneSostegno: String? = null,
    @SerialName("ID_Sostegno")
    val idSostegno: Int? = null,
    val geometry: Geometry? = null
)

@Serializable
data class StazioneRadiobase(
    @SerialName("OBJECTID")
    val objectId: Int,
    @SerialName("ID_Sostegno")
    val idSostegno: Int,
    @SerialName("X_Sostegno")
    val xSostegno: Double,
    @SerialName("Y_Sostegno")
    val ySostegno: Double,
    @SerialName("Comune")
    val comune: String? = null,
    @SerialName("Istat")
    val istat: Int? = null,
    @SerialName("Operatore")
    val operatore: String? = null,
    @SerialName("Tecnologia")
    val tecnologia: String? = null,
    @SerialName("Frequenza")
    val frequenza: String? = null,
    @SerialName("Potenza")
    val potenza: String? = null,
    val geometry: Geometry? = null
)

@Serializable
data class ServiceMetadata(
    val currentVersion: Double,
    val serviceItemId: String,
    val maxRecordCount: Int,
    val spatialReference: SpatialReference,
    val layers: List<LayerInfo>,
    val supportedQueryFormats: String,
    val supportedExportFormats: List<String>
)

@Serializable
data class LayerInfo(
    val id: Int,
    val name: String,
    val geometryType: String,
    val spatialReference: SpatialReference,
    val lastModified: Long? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class RelatedRecord(
    val attributes: Map<String, JsonElement>
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class RelatedRecordGroup(
    val objectId: Int,
    val count: Int? = null,
    val relatedRecords: List<RelatedRecord>? = null
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonIgnoreUnknownKeys
data class RelatedRecordsResponse(
    val relatedRecordGroups: List<RelatedRecordGroup>,
    val fields: List<Field>? = null
)

@Serializable
data class ImpiantoDetail(
    val objectId: Int,
    val direzioni: String?,
    val ragioneSociale: String?,
    val impianto: Int?
)

@Serializable
data class MisuraCEM(
    val objectId: Int,
    val dataOra: String? = null,
    val medVm: String? = null,
    val durata: String? = null,
    val comune: String? = null,
    val quotaTerr: String? = null,
    val luogo: String? = null,
    val grafico: String? = null,
    val anno: Int? = null,
    val geometry: Geometry? = null
)

@Serializable
data class AttachmentInfo(
    val id: Int,
    val name: String,
    val contentType: String,
    val size: Int,
    val keywords: String? = null,
    val exifInfo: JsonElement? = null
)

@Serializable
data class AttachmentGroup(
    val parentObjectId: Int,
    val parentGlobalId: String? = null,
    val attachmentInfos: List<AttachmentInfo>
)

@Serializable
data class AttachmentResponse(
    val fields: List<Field>? = null,
    val attachmentGroups: List<AttachmentGroup>
)

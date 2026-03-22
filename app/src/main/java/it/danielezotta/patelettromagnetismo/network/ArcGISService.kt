package it.danielezotta.patelettromagnetismo.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import it.danielezotta.patelettromagnetismo.models.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull

// Extension functions for JsonElement
private fun JsonElement?.toIntSafe(): Int? = this?.jsonPrimitive?.intOrNull ?: this?.jsonPrimitive?.doubleOrNull?.toInt()
private fun JsonElement?.toDoubleSafe(): Double? = this?.jsonPrimitive?.doubleOrNull
private fun JsonElement?.toStringSafe(): String? = this?.jsonPrimitive?.contentOrNull

class ArcGISService(private val httpClient: HttpClient) {
    
    companion object {
        private const val BASE_URL = "https://services5.arcgis.com/9T5RxYdubL4b1BrS/arcgis/rest/services"
        private const val RTV_IMPIANTI_SERVICE = "RTV_Impianti"
        private const val STAZIONI_RADIOBASE_SERVICE = "Stazioni_Radiobase"
        private const val MISURE_CEM_SERVICE = "Misure_CEM_TOT"
    }

    suspend fun getServiceMetadata(service: String): ServiceMetadata? {
        return try {
            val url = "$BASE_URL/$service/FeatureServer"
            httpClient.get(url) {
                parameter("f", "json")
            }.body<ServiceMetadata>()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRTVImpianti(
        where: String = "1=1",
        outFields: String = "*",
        returnGeometry: Boolean = true,
        outSR: Int = 4326,
        resultOffset: Int = 0,
        resultRecordCount: Int = 8000
    ): ArcGISResponse? {
        return try {
            val url = "$BASE_URL/$RTV_IMPIANTI_SERVICE/FeatureServer/0/query"
            
            val response: HttpResponse = httpClient.get(url) {
                parameter("f", "json")
                parameter("where", where)
                parameter("outFields", outFields)
                parameter("returnGeometry", returnGeometry)
                parameter("outSR", outSR)
                parameter("resultOffset", resultOffset)
                parameter("resultRecordCount", resultRecordCount)
                parameter("maxRecordCountFactor", 4)
                parameter("cacheHint", true)
                parameter("orderByFields", "OBJECTID ASC")
                parameter("spatialRel", "esriSpatialRelIntersects")
            }
            
            val responseBody = response.bodyAsText()
            Json { ignoreUnknownKeys = true }.decodeFromString(ArcGISResponse.serializer(), responseBody)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStazioniRadiobase(
        where: String = "1=1",
        outFields: String = "*",
        returnGeometry: Boolean = true,
        outSR: Int = 4326,
        resultOffset: Int = 0,
        resultRecordCount: Int = 8000
    ): ArcGISResponse? {
        return try {
            val url = "$BASE_URL/$STAZIONI_RADIOBASE_SERVICE/FeatureServer/1/query"
            
            val response: HttpResponse = httpClient.get(url) {
                parameter("f", "json")
                parameter("where", where)
                parameter("outFields", outFields)
                parameter("returnGeometry", returnGeometry)
                parameter("outSR", outSR)
                parameter("resultOffset", resultOffset)
                parameter("resultRecordCount", resultRecordCount)
                parameter("maxRecordCountFactor", 4)
                parameter("cacheHint", true)
                parameter("orderByFields", "OBJECTID ASC")
                parameter("spatialRel", "esriSpatialRelIntersects")
            }
            
            val responseBody = response.bodyAsText()
            Json { ignoreUnknownKeys = true }.decodeFromString(ArcGISResponse.serializer(), responseBody)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRTVImpiantiById(objectId: Int): RTVImpianto? {
        return try {
            val response = getRTVImpianti(
                where = "OBJECTID=$objectId",
                outFields = "*",
                returnGeometry = true
            )
            response?.features?.firstOrNull()?.let { feature ->
                RTVImpianto(
                    objectId = feature.attributes["OBJECTID"] as Int,
                    nomeImpianto = feature.attributes["Nome_Impianto"] as? String,
                    comune = feature.attributes["Comune"] as? String,
                    provincia = feature.attributes["Provincia"] as? String,
                    indirizzo = feature.attributes["Indirizzo"] as? String,
                    tipologia = feature.attributes["Tipologia"] as? String,
                    frequenza = feature.attributes["Frequenza"] as? String,
                    potenza = feature.attributes["Potenza"] as? String,
                    geometry = feature.geometry
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getStazioniRadiobaseById(objectId: Int): StazioneRadiobase? {
        return try {
            val response = getStazioniRadiobase(
                where = "OBJECTID=$objectId",
                outFields = "*",
                returnGeometry = true
            )
            response?.features?.firstOrNull()?.let { feature ->
                StazioneRadiobase(
                    objectId = feature.attributes["OBJECTID"] as Int,
                    idSostegno = feature.attributes["ID_Sostegno"] as Int,
                    xSostegno = feature.attributes["X_Sostegno"] as Double,
                    ySostegno = feature.attributes["Y_Sostegno"] as Double,
                    comune = feature.attributes["Comune"] as? String,
                    istat = feature.attributes["Istat"] as? Int,
                    operatore = feature.attributes["Operatore"] as? String,
                    tecnologia = feature.attributes["Tecnologia"] as? String,
                    frequenza = feature.attributes["Frequenza"] as? String,
                    potenza = feature.attributes["Potenza"] as? String,
                    geometry = feature.geometry
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getRTVImpiantiByComune(comune: String): List<RTVImpianto> {
        return try {
            val response = getRTVImpianti(
                where = "Comune='$comune'",
                outFields = "*",
                returnGeometry = true
            )
            response?.features?.mapNotNull { feature ->
                try {
                    RTVImpianto(
                        objectId = feature.attributes["OBJECTID"] as Int,
                        nomeImpianto = feature.attributes["Nome_Impianto"] as? String,
                        comune = feature.attributes["Comune"] as? String,
                        provincia = feature.attributes["Provincia"] as? String,
                        indirizzo = feature.attributes["Indirizzo"] as? String,
                        tipologia = feature.attributes["Tipologia"] as? String,
                        frequenza = feature.attributes["Frequenza"] as? String,
                        potenza = feature.attributes["Potenza"] as? String,
                        geometry = feature.geometry
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getStazioniRadiobaseByComune(comune: String): List<StazioneRadiobase> {
        return try {
            val response = getStazioniRadiobase(
                where = "Comune='$comune'",
                outFields = "*",
                returnGeometry = true
            )
            response?.features?.mapNotNull { feature ->
                try {
                    StazioneRadiobase(
                        objectId = feature.attributes["OBJECTID"] as Int,
                        idSostegno = feature.attributes["ID_Sostegno"] as Int,
                        xSostegno = feature.attributes["X_Sostegno"] as Double,
                        ySostegno = feature.attributes["Y_Sostegno"] as Double,
                        comune = feature.attributes["Comune"] as? String,
                        istat = feature.attributes["Istat"] as? Int,
                        operatore = feature.attributes["Operatore"] as? String,
                        tecnologia = feature.attributes["Tecnologia"] as? String,
                        frequenza = feature.attributes["Frequenza"] as? String,
                        potenza = feature.attributes["Potenza"] as? String,
                        geometry = feature.geometry
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getRelatedRecords(
        objectId: Int,
        relationshipId: Int = 0,
        outFields: String = "*",
        returnGeometry: Boolean = false
    ): RelatedRecordsResponse? {
        return try {
            val url = "$BASE_URL/$STAZIONI_RADIOBASE_SERVICE/FeatureServer/1/queryRelatedRecords"
            
            val response: HttpResponse = httpClient.get(url) {
                parameter("f", "json")
                parameter("objectIds", objectId)
                parameter("relationshipId", relationshipId)
                parameter("outFields", outFields)
                parameter("returnGeometry", returnGeometry)
                parameter("definitionExpression", "1=1")
            }
            
            val json = response.bodyAsText()
            Json.decodeFromString<RelatedRecordsResponse>(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getImpiantoDetails(
        objectId: Int,
        outFields: String = "Direzioni,Ragione_sociale,OBJECTID"
    ): ImpiantoDetail? {
        return try {
            val url = "$BASE_URL/$STAZIONI_RADIOBASE_SERVICE/FeatureServer/6/query"
            
            val response: HttpResponse = httpClient.get(url) {
                parameter("f", "json")
                parameter("objectIds", objectId)
                parameter("outFields", outFields)
                parameter("outSR", "102100")
                parameter("returnGeometry", false)
                parameter("spatialRel", "esriSpatialRelIntersects")
                parameter("where", "1=1")
            }
            
            val json = response.bodyAsText()
            val arcGISResponse = Json.decodeFromString<ArcGISResponse>(json)
            arcGISResponse.features?.firstOrNull()?.let { feature ->
                ImpiantoDetail(
                    objectId = feature.attributes["OBJECTID"].toIntSafe() ?: 0,
                    direzioni = feature.attributes["Direzioni"]?.toStringSafe(),
                    ragioneSociale = feature.attributes["Ragione_sociale"]?.toStringSafe(),
                    impianto = feature.attributes["Impianto"]?.toIntSafe()
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMisureCEM(
        where: String = "1=1",
        outFields: String = "*",
        returnGeometry: Boolean = true,
        outSR: Int = 4326,
        resultOffset: Int = 0,
        resultRecordCount: Int = 2000
    ): ArcGISResponse? {
        return try {
            val url = "$BASE_URL/$MISURE_CEM_SERVICE/FeatureServer/0/query"
            val response: HttpResponse = httpClient.get(url) {
                parameter("f", "json")
                parameter("where", where)
                parameter("outFields", outFields)
                parameter("returnGeometry", returnGeometry)
                parameter("outSR", outSR)
                parameter("resultOffset", resultOffset)
                parameter("resultRecordCount", resultRecordCount)
                parameter("spatialRel", "esriSpatialRelIntersects")
            }
            val json = response.bodyAsText()
            Json.decodeFromString<ArcGISResponse>(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCEMAttachments(objectId: Int): AttachmentResponse? {
        return try {
            val url = "$BASE_URL/$MISURE_CEM_SERVICE/FeatureServer/0/queryAttachments"
            val response: HttpResponse = httpClient.get(url) {
                parameter("f", "json")
                parameter("objectIds", objectId)
                parameter("returnMetadata", true)
            }
            val json = response.bodyAsText()
            Json.decodeFromString<AttachmentResponse>(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCEMAttachmentUrl(objectId: Int, attachmentId: Int): String? {
        return try {
            val url = "$BASE_URL/$MISURE_CEM_SERVICE/FeatureServer/0/$objectId/attachments/$attachmentId"
            // Return the direct download URL
            url
        } catch (e: Exception) {
            null
        }
    }
}

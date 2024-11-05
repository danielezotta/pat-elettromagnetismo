package it.danielezotta.patelettromagnetismo.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse(
    val currpage: Int,
    val totalrecords: Int,
    val executetime: Double,
    val totalpages: Int,
    val DV_A_ALBO: List<ApiAlboEntry>
)
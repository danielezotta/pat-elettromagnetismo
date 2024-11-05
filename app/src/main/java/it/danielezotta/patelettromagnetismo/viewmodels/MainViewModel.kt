package it.danielezotta.patelettromagnetismo.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.ContentType.Application.FormUrlEncoded
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import it.danielezotta.patelettromagnetismo.models.ApiAlboEntry
import it.danielezotta.patelettromagnetismo.models.ApiResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

val PERMITS_URL =
    "http://www.territorio.provincia.tn.it/gcopresenter/play.json?task=ajaxCall&_ajaxCall=%3Ftask%3DgetDvResultSet_Q%26__namespace%3Dpns8_Q%26__idDV%3DDV_A_ALBO_Q%26__USERLANGUAGE%3Dit_Q%26__inputParameterName%3D_Q%26__isPopUp%3Dfalse_Q%26_dataRilascioDa%3D_Q%26_dataRilascioA%3D_Q%26_numDetermina%3D_Q%26_oggettoStr%3D_Q%26_area%3D50_Q%26_tema%3D_Q%26_impresaStr%3D_Q%26_comune%3D_Q%26_stato%3D"

class MainViewModel : ViewModel() {

    private var _permits = MutableStateFlow(listOf<ApiAlboEntry>())
    val permits = _permits.asStateFlow()

    private var _page = MutableStateFlow(1)
    val page = _page.asStateFlow()

    private var _loadingState = MutableStateFlow(LoadingState.IDLE)
    val loadingState = _loadingState.asStateFlow()

    fun getPermits() {

        _loadingState.value = LoadingState.LOADING

        viewModelScope.launch {

            val client = HttpClient(CIO) {
                install(HttpTimeout) {
                    requestTimeoutMillis = 120000
                }
                install(ContentNegotiation) {
                    register(
                        ContentType.Text.Any, KotlinxSerializationConverter(
                            Json {
                                prettyPrint = true
                                isLenient = true
                                ignoreUnknownKeys = true
                            }
                        )
                    )
                }
            }

            var response: ApiResponse = client.post(PERMITS_URL + "&page=${ _page.value }") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {

                }))
            }.body()

            _permits.value += response.DV_A_ALBO

            _loadingState.value = LoadingState.LOADED
        }

    }

    fun loadMorePermits() {
        _page.value++
        getPermits()
    }

    enum class LoadingState {
        IDLE,
        LOADING,
        LOADED,
        ERROR
    }

}


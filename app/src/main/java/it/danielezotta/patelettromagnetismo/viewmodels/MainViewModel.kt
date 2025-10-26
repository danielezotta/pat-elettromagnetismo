package it.danielezotta.patelettromagnetismo.viewmodels

import android.app.Application
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.KotlinxSerializationConverter
import it.danielezotta.patelettromagnetismo.dataStore
import it.danielezotta.patelettromagnetismo.models.ApiAlboEntry
import it.danielezotta.patelettromagnetismo.models.ApiResponse
import it.danielezotta.patelettromagnetismo.util.AppConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var _permits = MutableStateFlow(listOf<ApiAlboEntry>())
    val permits = _permits.asStateFlow()

    private var _page = MutableStateFlow(1)
    val page = _page.asStateFlow()

    private var _loadingState = MutableStateFlow(LoadingState.IDLE)
    val loadingState = _loadingState.asStateFlow()

    fun getPermits() {

        _loadingState.value = LoadingState.LOADING

        viewModelScope.launch {

            try {

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

                val response: ApiResponse = client.post(AppConstants.PERMITS_URL + "&page=${_page.value}") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(FormDataContent(Parameters.build {

                    }))
                }.body()

                _permits.value += response.DV_A_ALBO

                if (response.DV_A_ALBO.isNotEmpty()) {
                    viewModelScope.launch {
                        getApplication<Application>().applicationContext.dataStore.edit { it ->
                            it[intPreferencesKey("last_notified_item")] =
                                response.DV_A_ALBO.maxOfOrNull { it.IDATTO.toInt() } ?: 0
                        }
                    }
                }

                _loadingState.value = LoadingState.LOADED

            } catch (e: Exception) {
                _loadingState.value = LoadingState.ERROR
            }
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


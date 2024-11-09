package it.danielezotta.patelettromagnetismo.workers

import android.Manifest.permission
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.work.Worker
import androidx.work.WorkerParameters
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
import it.danielezotta.patelettromagnetismo.models.ApiResponse
import it.danielezotta.patelettromagnetismo.viewmodels.PERMITS_URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Send the notification
        sendNotification()
        return Result.success()
    }

    private fun sendNotification() {

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        CoroutineScope(Dispatchers.Default).launch {

            val channelId = "notification_channel"
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                "Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)

            val lastNotifiedItem = applicationContext.dataStore.data.map { it ->
                it[intPreferencesKey("last_notified_item")] ?: 0
            }.first()

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

            var lastItem = 0

            try {

                var response: ApiResponse = client.post("$PERMITS_URL&page=1") {
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(FormDataContent(Parameters.build {

                    }))
                }.body()

                for (item in response.DV_A_ALBO) {
                    if (item.IDATTO.toInt() > lastItem) {
                        lastItem = item.IDATTO.toInt()
                    }

                    if (item.IDATTO.toInt() < lastNotifiedItem) {
                        val notification = NotificationCompat.Builder(applicationContext, channelId)
                            .setContentTitle(item.INDIRIZZO)
                            .setContentText(item.NOMEIMPRESA)
                            .setSmallIcon(it.danielezotta.patelettromagnetismo.R.drawable.ic_stat_notification_icon)
                            .build()

                        notificationManager.notify(1, notification)
                    }
                }

                applicationContext.dataStore.edit { it ->
                    it[intPreferencesKey("last_notified_item")] = lastItem
                }

                client.close()

            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }
}
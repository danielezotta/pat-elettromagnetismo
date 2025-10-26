package it.danielezotta.patelettromagnetismo.workers

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.work.CoroutineWorker
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
import it.danielezotta.patelettromagnetismo.MainActivity
import it.danielezotta.patelettromagnetismo.dataStore
import it.danielezotta.patelettromagnetismo.models.ApiResponse
import it.danielezotta.patelettromagnetismo.util.AppConstants
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        sendNotification()
        return Result.success()
    }

    private suspend fun sendNotification() {

        if (ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val channelId = AppConstants.NOTIFICATION_CHANNEL_ID
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            AppConstants.NOTIFICATION_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Aggiornamenti nuovi permessi"
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(channel)

        val lastNotifiedItem = applicationContext.dataStore.data.map { it ->
            it[intPreferencesKey("last_notified_item")] ?: 0
        }.first()

        val client = HttpClient(CIO) {
            install(HttpTimeout) {
                requestTimeoutMillis = 180000
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
        val newItems = mutableListOf<Pair<String, String>>()
        val groupKey = "it.danielezotta.patelettromagnetismo.NEW_PERMITS"
        val summaryId = 1000

        try {

            val response: ApiResponse = client.post("${AppConstants.PERMITS_URL}&page=1") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {

                }))
            }.body()

            for (item in response.DV_A_ALBO) {
                if (item.IDATTO.toInt() > lastItem) {
                    lastItem = item.IDATTO.toInt()
                }

                if (item.IDATTO.toInt() > lastNotifiedItem) {
                    // Build a back stack to open MainActivity when tapping the notification
                    val intent = Intent(applicationContext, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    val pendingIntent: PendingIntent = TaskStackBuilder.create(applicationContext).run {
                        addNextIntentWithParentStack(intent)
                        requireNotNull(
                            getPendingIntent(
                                item.IDATTO.toInt(),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                        )
                    }

                    val notification = NotificationCompat.Builder(applicationContext, channelId)
                        .setContentTitle(item.INDIRIZZO)
                        .setContentText(item.NOMEIMPRESA)
                        .setSmallIcon(it.danielezotta.patelettromagnetismo.R.drawable.ic_stat_notification_icon)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setCategory(NotificationCompat.CATEGORY_EVENT)
                        .setGroup(groupKey)
                        .build()

                    // Use a stable ID so duplicates are updated/replaced
                    notificationManager.notify(item.IDATTO.toInt(), notification)
                    newItems.add(item.INDIRIZZO to item.NOMEIMPRESA)
                }
            }

            applicationContext.dataStore.edit { it ->
                it[intPreferencesKey("last_notified_item")] = lastItem
            }

            // Post a summary notification if there are multiple new items
            if (newItems.isNotEmpty()) {
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                val pendingIntent: PendingIntent = TaskStackBuilder.create(applicationContext).run {
                    addNextIntentWithParentStack(intent)
                    requireNotNull(
                        getPendingIntent(
                            summaryId,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                    )
                }

                val summaryNotification = NotificationCompat.Builder(applicationContext, channelId)
                    .setSmallIcon(it.danielezotta.patelettromagnetismo.R.drawable.ic_stat_notification_icon)
                    .setContentTitle("Nuovi permessi pubblicati")
                    .setContentText("${newItems.size} aggiornamenti")
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText(
                            newItems.take(5).joinToString("\n") { (title, text) -> "$title — $text" }
                        )
                    )
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setGroup(groupKey)
                    .setGroupSummary(true)
                    .build()

                notificationManager.notify(summaryId, summaryNotification)
            }

            client.close()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}
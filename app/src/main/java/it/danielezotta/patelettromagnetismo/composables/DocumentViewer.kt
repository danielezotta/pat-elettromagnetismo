package it.danielezotta.patelettromagnetismo.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.net.Uri
import android.content.Intent
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.net.toUri
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.outlined.Error
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.activity.compose.BackHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentViewer(
    documentUrl: String,
    documentTitle: String,
    onBackClick: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var localFile by remember(documentUrl) { mutableStateOf<File?>(null) }
    var loadError by remember(documentUrl) { mutableStateOf<String?>(null) }
    var started by remember(documentUrl) { mutableStateOf(false) }
    var retry by remember(documentUrl) { mutableStateOf(0) }

    LaunchedEffect(documentUrl, retry) {
        if (!started) {
            started = true
            isLoading = true
            loadError = null
            withContext(Dispatchers.IO) {
                val file = downloadFileToCache(context, documentUrl, documentTitle)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    if (file != null) {
                        localFile = file
                    } else {
                        loadError = "Impossibile scaricare il documento"
                    }
                }
            }
        }
    }

        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            BackHandler { onBackClick() }
            if (localFile != null) {
                OdtViewer(
                    context = context,
                    odtFile = localFile!!,
                    title = documentTitle,
                    onBackClick = { localFile = null }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    if (loadError != null && !isLoading) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(imageVector = Icons.Outlined.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Text("Errore durante il download", style = MaterialTheme.typography.titleMedium)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(loadError!!, style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilledTonalButton(onClick = { retry += 1; started = false }) {
                                        Text("Riprova")
                                    }
                                    OutlinedButton(onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, documentUrl.toUri()))
                                    }) {
                                        Text("Apri nel browser")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
}

private fun downloadToCache(context: Context, urlStr: String, title: String): Uri? {
    return try {
        val url = URL(urlStr)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 30000
            readTimeout = 60000
        }
        connection.connect()
        if (connection.responseCode in 200..299) {
            val base = title.ifBlank { "document" }
            val safeBase = base.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val fileName = if (safeBase.contains('.')) safeBase else "$safeBase.odt"
            val file = File(context.cacheDir, fileName)
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        } else null
    } catch (_: Exception) {
        null
    }
}

private fun downloadFileToCache(context: Context, urlStr: String, title: String): File? {
    return try {
        val url = URL(urlStr)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 30000
            readTimeout = 60000
        }
        connection.connect()
        if (connection.responseCode in 200..299) {
            val base = title.ifBlank { "document" }
            val safeBase = base.replace(Regex("[^A-Za-z0-9._-]"), "_")
            val fileName = if (safeBase.contains('.')) safeBase else "$safeBase.odt"
            val file = File(context.cacheDir, fileName)
            connection.inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        } else null
    } catch (_: Exception) {
        null
    }
}

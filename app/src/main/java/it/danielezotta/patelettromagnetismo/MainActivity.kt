package it.danielezotta.patelettromagnetismo

import android.Manifest.permission.POST_NOTIFICATIONS
import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import it.danielezotta.patelettromagnetismo.composables.PermitList
import it.danielezotta.patelettromagnetismo.ui.theme.PATPermessiElettromagnetismoTheme
import it.danielezotta.patelettromagnetismo.viewmodels.MainViewModel
import it.danielezotta.patelettromagnetismo.workers.NotificationWorker
import java.util.concurrent.TimeUnit


val Context.dataStore by preferencesDataStore(name = "preferences")

class MainActivity : ComponentActivity() {

    lateinit var mainViewModel: MainViewModel

    val onDownloadComplete: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId != -1L) {
                    val downloadManager = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                    val uri: Uri? = downloadManager.getUriForDownloadedFile(downloadId)

                    if (uri != null) {
                        val openIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.oasis.opendocument.text")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(openIntent)
                    }
                }
            }
        }
    }

    // Register the permission request callback
    val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission is granted; proceed with the action
        } else {
            // Permission is denied; handle the denial
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestNotificationPermission()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                RECEIVER_EXPORTED
            )
        } else {
            @SuppressLint("UnspecifiedRegisterReceiverFlag")
            registerReceiver(
                onDownloadComplete,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mainViewModel.getPermits()

        scheduleNotificationWork(this, 4)

        enableEdgeToEdge()
        setContent {
            PATPermessiElettromagnetismoTheme {
                Surface (
                    modifier = Modifier.fillMaxSize()
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            TopAppBar(
                                title = {
                                    Row (
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(painter = painterResource(R.drawable.trentino_coa), contentDescription = null, Modifier.height(48.dp).padding(vertical = 4.dp))
                                        Spacer(modifier = Modifier.padding(6.dp))
                                        Text(stringResource(R.string.elettromagnetismo))
                                    }
                                }
                            )
                        }
                    ) { innerPadding ->
                        Box (modifier = Modifier.padding(innerPadding)) {
                            PermitScreen(mainViewModel)
                        }
                    }
                }
            }
        }
    }

    fun scheduleNotificationWork(context: Context, intervalHours: Long) {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(intervalHours, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "notificationWork",
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    // Function to check and request a permission
    fun checkAndRequestPermission(permission: String) {
        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                // Permission is already granted
            }
            shouldShowRequestPermissionRationale(permission) -> {
                // Explain why the permission is needed and then request it
                requestPermissionLauncher.launch(permission)
            }
            else -> {
                // Request the permission directly
                requestPermissionLauncher.launch(permission)
            }
        }
    }

    // Call this function to check a specific permission, e.g., notification permission
    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestPermission(POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun PermitScreen(mainViewModel: MainViewModel) {

    PermitList(mainViewModel)

}

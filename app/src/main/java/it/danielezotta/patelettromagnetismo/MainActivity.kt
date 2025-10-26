package it.danielezotta.patelettromagnetismo

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import it.danielezotta.patelettromagnetismo.composables.DocumentViewer
import it.danielezotta.patelettromagnetismo.composables.PermitList
import it.danielezotta.patelettromagnetismo.ui.theme.PATPermessiElettromagnetismoTheme
import it.danielezotta.patelettromagnetismo.viewmodels.MainViewModel
import it.danielezotta.patelettromagnetismo.workers.NotificationWorker
import it.danielezotta.patelettromagnetismo.util.AppConstants
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    private lateinit var mainViewModel: MainViewModel

    // Register the permission request callback
    private val requestPermissionLauncher = registerForActivityResult(
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

        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        mainViewModel.getPermits()

        scheduleNotificationWork(this, 4)

        enableEdgeToEdge()
        setContent {
            PATPermessiElettromagnetismoTheme {
                Surface (
                    modifier = Modifier.fillMaxSize()
                ) {
                    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Row (
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Image(painter = painterResource(R.drawable.trentino_coa), contentDescription = null, Modifier.height(40.dp).padding(vertical = 4.dp))
                                        Spacer(modifier = Modifier.padding(8.dp))
                                        Text(text = stringResource(R.string.elettromagnetismo))
                                    }
                                },
                                scrollBehavior = scrollBehavior
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

    private fun scheduleNotificationWork(context: Context, intervalHours: Long) {
        val workRequest = PeriodicWorkRequestBuilder<NotificationWorker>(intervalHours, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AppConstants.WORK_NAME_NOTIFICATIONS,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    // Function to check and request a permission
    private fun checkAndRequestPermission(permission: String) {
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
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkAndRequestPermission(POST_NOTIFICATIONS)
        }
    }

}

@Composable
fun PermitScreen(mainViewModel: MainViewModel) {
    var currentDocumentUrl by remember { mutableStateOf<String?>(null) }
    var currentDocumentTitle by remember { mutableStateOf("") }

    if (currentDocumentUrl != null) {
        DocumentViewer(
            documentUrl = currentDocumentUrl!!,
            documentTitle = currentDocumentTitle,
            onBackClick = { currentDocumentUrl = null }
        )
    } else {
        PermitList(
            mainViewModel = mainViewModel,
            onDocumentClick = { url, title ->
                currentDocumentUrl = url
                currentDocumentTitle = title
            }
        )
    }
}

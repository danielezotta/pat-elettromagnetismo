package it.danielezotta.patelettromagnetismo

import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import it.danielezotta.patelettromagnetismo.composables.DocumentViewer
import it.danielezotta.patelettromagnetismo.composables.MapPermitScreen
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

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.BLACK
            )
        )
        setContent {
            PATPermessiElettromagnetismoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
                    var selectedIndex by remember { mutableIntStateOf(0) }
                    var showDocumentViewer by remember { mutableStateOf(false) }
                    var documentUrl by remember { mutableStateOf("") }
                    var documentTitle by remember { mutableStateOf("") }
                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            CenterAlignedTopAppBar(
                                title = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.elettromagnetismo)
                                        )
                                    }
                                },
                                navigationIcon = {
                                    Image(
                                        painter = painterResource(R.drawable.trentino_coa),
                                        contentDescription = "Stemma Trentino",
                                        modifier = Modifier.size(40.dp).padding(start = 8.dp)
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors (
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                scrollBehavior = scrollBehavior
                            )
                        },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ) {
                                val itemColors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                NavigationBarItem(
                                    selected = selectedIndex == 0,
                                    onClick = { selectedIndex = 0 },
                                    icon = { Icon(Icons.Outlined.Map, contentDescription = "Mappa") },
                                    label = { Text("Mappa") },
                                    colors = itemColors
                                )
                                NavigationBarItem(
                                    selected = selectedIndex == 1,
                                    onClick = { selectedIndex = 1 },
                                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Permessi") },
                                    label = { Text("Permessi") },
                                    colors = itemColors
                                )
                            }
                        }
                    ) { innerPadding ->
                        Box(modifier = Modifier.padding(innerPadding)) {
                            if (showDocumentViewer) {
                                DocumentViewer(
                                    documentUrl = documentUrl,
                                    documentTitle = documentTitle,
                                    onBackClick = {
                                        showDocumentViewer = false
                                        documentUrl = ""
                                        documentTitle = ""
                                    }
                                )
                            } else {
                                MapPermitScreen(
                                    mainViewModel = mainViewModel,
                                    selectedIndex = selectedIndex,
                                    onSwitchToMap = { selectedIndex = 0 },
                                    onDocumentClick = { url, title ->
                                        documentUrl = url
                                        documentTitle = title
                                        showDocumentViewer = true
                                    }
                                )
                            }
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


package it.danielezotta.patelettromagnetismo.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import it.danielezotta.patelettromagnetismo.viewmodels.MainViewModel

@Composable
fun MapPermitScreen(
    mainViewModel: MainViewModel,
    selectedIndex: Int,
    onSwitchToMap: () -> Unit = {},
    onDocumentClick: (url: String, title: String) -> Unit = { _, _ -> }
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(if (selectedIndex == 0) 1f else 0f)
        ) {
            OSMElectromagneticMapView(
                arcGISService = mainViewModel.arcGISService,
                pendingMapSearch = mainViewModel.pendingMapSearch,
                onMapSearchConsumed = { mainViewModel.clearMapSearch() },
                modifier = Modifier.fillMaxSize()
            )
        }
        if (selectedIndex == 1) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(1f)
            ) {
                PermitList(
                    mainViewModel = mainViewModel,
                    onShowOnMap = { query ->
                        mainViewModel.searchOnMap(query)
                        onSwitchToMap()
                    },
                    onDocumentClick = onDocumentClick
                )
            }
        }
    }
}

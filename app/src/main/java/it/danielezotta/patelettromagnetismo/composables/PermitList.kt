package it.danielezotta.patelettromagnetismo.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.danielezotta.patelettromagnetismo.viewmodels.MainViewModel
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height

@Composable
fun PermitList(
    mainViewModel: MainViewModel,
    onDocumentClick: (url: String, title: String) -> Unit
) {

    val permits = mainViewModel.permits.collectAsState()
    val loadingState = mainViewModel.loadingState.collectAsState()

    LazyColumn (
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(permits.value) { permit ->
            PermitCardItem(
                apiAlboEntry = permit,
                onDocumentClick = onDocumentClick
            )

            if (permits.value.last() == permit && loadingState.value == MainViewModel.LoadingState.LOADED) {
                mainViewModel.loadMorePermits()
            }
        }

        if (loadingState.value == MainViewModel.LoadingState.LOADING) {
            items(5) {
                PermitCardPlaceholder()
            }
        }

        item {
            AnimatedVisibility(loadingState.value == MainViewModel.LoadingState.ERROR) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        colors = CardDefaults.elevatedCardColors(),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text("Errore durante il caricamento", style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("La richiesta ha superato il tempo massimo di attesa.", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = { mainViewModel.getPermits() }) {
                                Text("Riprova")
                            }
                        }
                    }
                }
            }
        }

    }

}

@Preview
@Composable
fun PermitListPreview() {
//    val permits = listOf(
//        Permit(
//            "Cavizzana (TN)",
//            "20/04/2024",
//            "Vodafone Italia SPA",
//            "TZ82 TOZZAGA",
//            "213456_scheda_riassuntiva.odf"
//        ),
//        Permit(
//            "Folgaria (TN)",
//            "19/04/2024",
//            "TELECOM ITALIA SPA o TIM SPA",
//            "TD19 FOLGARIA 2",
//            "12123_scheda_riassuntiva.odf"
//        )
//    )

//    PermitList(permits)

}
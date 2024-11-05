package it.danielezotta.patelettromagnetismo.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.danielezotta.patelettromagnetismo.viewmodels.MainViewModel

@Composable
fun PermitList(mainViewModel: MainViewModel) {

    val permits = mainViewModel.permits.collectAsState()
    var loadingState = mainViewModel.loadingState.collectAsState()

    LazyColumn (
        modifier = Modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(permits.value) { permit ->
            PermitCardItem(permit)

            if (permits.value.last() == permit) {
                mainViewModel.loadMorePermits()
            }
        }

        item {
            AnimatedVisibility(loadingState.value == MainViewModel.LoadingState.LOADING) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
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
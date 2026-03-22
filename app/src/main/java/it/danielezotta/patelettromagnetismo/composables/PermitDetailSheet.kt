package it.danielezotta.patelettromagnetismo.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.danielezotta.patelettromagnetismo.models.ApiAlboEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermitDetailSheet(
    permit: ApiAlboEntry,
    onDismiss: () -> Unit,
    onDocumentClick: (url: String, title: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Brand colour stripe
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        color = CompanyColor.fromCompany(permit.NOMEIMPRESA).color,
                        shape = MaterialTheme.shapes.small
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = permit.INDIRIZZO,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = permit.COMUNE_INDI,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            SheetDetailRow("Operatore", permit.NOMEIMPRESA)
            SheetDetailRow("Stato", permit.ATTO_STATO)
            SheetDetailRow("Inizio validità", permit.DATAINIZIOVALIDITA)
            SheetDetailRow("Fine validità", permit.DATAFINEVALIDITA.ifEmpty { "-" })
            SheetDetailRow("Data autorizzazione", permit.DATAAUTORIZZAZIONE.ifEmpty { "-" })
            SheetDetailRow("Tipo documento", permit.TIPODOC.ifEmpty { "-" })
            SheetDetailRow("Oggetto", permit.OGGETTODOC.ifEmpty { "-" })
            SheetDetailRow("Documento", permit.DOCUMENTO.ifEmpty { "-" })
            SheetDetailRow("N° autorizzazione", permit.NUMEROAUTORIZZAZIONE.ifEmpty { "-" })

            if (permit.ALLEGATONOMEFILE.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                FilledTonalButton(
                    onClick = {
                        val url = "http://www.territorio.provincia.tn.it/gco/downloadFile.down?userFromRequestParam=portalpa&qpportal=true&codCompany=PROV_TN&idAllegato=${permit.DOCUMENTO}&idAtto=${permit.IDATTO}"
                        onDocumentClick(url, permit.ALLEGATONOMEFILE)
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = permit.ALLEGATONOMEFILE,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(160.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

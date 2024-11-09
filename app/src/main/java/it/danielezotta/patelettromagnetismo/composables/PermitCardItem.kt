package it.danielezotta.patelettromagnetismo.composables

import android.app.DownloadManager
import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.danielezotta.patelettromagnetismo.models.ApiAlboEntry


enum class CompanyColor(val color: Color) {
    TIM(Color(0x800033a1)), // Blue
    VODAFONE(Color(0x80E60000)), // Vodafone Red
    WINDTRE(Color(0x80FF6600)), // Orange
    EOLO(Color(0x8000AEEF)), // Light Blue
    ILIAD(Color(0x80D52B1E)), // Red
    ZEFIRO(Color(0x804ccbda)), // Light Blue)
    GENERIC(Color(0x80808080)); // Default Gray

    companion object {
        fun fromCompany(company: String): CompanyColor {
            return when {
                company.lowercase().contains("tim") -> TIM
                company.lowercase().contains("vodafone") -> VODAFONE
                company.lowercase().contains("windtre") -> WINDTRE
                company.lowercase().contains("eolo") -> EOLO
                company.lowercase().contains("iliad") -> ILIAD
                company.lowercase().contains("zefiro") -> ZEFIRO
                else -> GENERIC
            }
        }
    }
}


@Composable
fun PermitCardItem(apiAlboEntry: ApiAlboEntry) {
    var expanded = remember { mutableStateOf(false) }
    val localContext = LocalContext.current

    Card(
        onClick = {
            expanded.value = !expanded.value
        },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = CompanyColor.fromCompany(apiAlboEntry.NOMEIMPRESA).color
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(apiAlboEntry.INDIRIZZO, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)

            Row {
                AnimatedVisibility(expanded.value) {
                    Text("Comune: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
                Text(apiAlboEntry.COMUNE_INDI, style = MaterialTheme.typography.bodySmall)
            }

            Row {
                AnimatedVisibility(expanded.value) {
                    Text("Operatore: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
                Text(apiAlboEntry.NOMEIMPRESA, style = MaterialTheme.typography.bodySmall)
            }

            Row {
                AnimatedVisibility(expanded.value) {
                    Text("Inizio validità: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
                Text(apiAlboEntry.DATAINIZIOVALIDITA, style = MaterialTheme.typography.bodySmall)
            }

            AnimatedVisibility(
                expanded.value,
                modifier = Modifier.fillMaxWidth()
            ) {

                Column {

                    Row {
                        Text("Documento: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(if (apiAlboEntry.DOCUMENTO == "") "-" else apiAlboEntry.DOCUMENTO, style = MaterialTheme.typography.bodySmall)
                    }

                    Row {
                        Text("Stato: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(if (apiAlboEntry.ATTO_STATO == "") "-" else apiAlboEntry.ATTO_STATO, style = MaterialTheme.typography.bodySmall)
                    }

                    Row {
                        Text("Fine validità: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(if (apiAlboEntry.DATAFINEVALIDITA == "") "-" else apiAlboEntry.DATAFINEVALIDITA, style = MaterialTheme.typography.bodySmall)
                    }

                    Row {
                        Text("Data autorizzazione: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(if (apiAlboEntry.DATAAUTORIZZAZIONE == "") "-" else apiAlboEntry.DATAAUTORIZZAZIONE, style = MaterialTheme.typography.bodySmall)
                    }

                    Row {
                        Text("Oggetto: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(if (apiAlboEntry.OGGETTODOC == "") "-" else apiAlboEntry.OGGETTODOC, style = MaterialTheme.typography.bodySmall)
                    }

                    Row {
                        Text("Tipo documento: ", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        Text(if (apiAlboEntry.TIPODOC == "") "-" else apiAlboEntry.TIPODOC, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {

                        val url = "http://www.territorio.provincia.tn.it/gco/downloadFile.down?userFromRequestParam=portalpa&qpportal=true&codCompany=PROV_TN&idAllegato=${ apiAlboEntry.DOCUMENTO }&idAtto=${ apiAlboEntry.IDATTO }"
                        val request: DownloadManager.Request = DownloadManager.Request(Uri.parse(url))

                        with(request) {
                            setTitle("${ apiAlboEntry.DOCUMENTO }-${ apiAlboEntry.IDATTO }.odt")
                            setMimeType("application/vnd.oasis.opendocument.text")
                            setDescription("Downloading atto...")
                            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            setDestinationInExternalPublicDir(
                                Environment.DIRECTORY_DOWNLOADS,
                                "${ apiAlboEntry.DOCUMENTO }-${ apiAlboEntry.IDATTO }.odt"
                            )
                        }

                        val manager: DownloadManager =
                            localContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                        manager.enqueue(request)

                    }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(apiAlboEntry.ALLEGATONOMEFILE)
                    }

                }

            }
        }
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PermitCardItemPreview() {
    PermitCardItem(
        ApiAlboEntry(
            "371513",
            "",
            "TELECOM ITALIA S.p.A. o TIM SPA SPA",
            "TZ82 TOZZAGA",
            "Elettromagnetismo",
            "14/10/2024",
            "69176095",
            "69176093",
            "0",
            "VALIDO",
            "69347229",
            "CAVIZZANA(TN)",
            "69347229_SCHEDA_RIASSUNTIVA.odt",
            "SCHEDA RIASSUNTIVA SCIA",
            "30/10/2024",
            "pns8GK_0",
            "",
            "0",
            "SCHEDA RIASSUNTIVA.  TELECOM ITALIA S.p.A. o TIM SPA SPA\r\nV. GAETANO NEGRI, 1 - 20123 MILANO (MI)"
        )
    )
}
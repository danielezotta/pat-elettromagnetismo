package it.danielezotta.patelettromagnetismo.composables

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.danielezotta.patelettromagnetismo.models.ApiAlboEntry
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush


enum class CompanyColor(val color: Color) {
    TIM(Color(0x800033a1)), // Blue
    VODAFONE(Color(0x80E60000)), // Vodafone Red
    WINDTRE(Color(0x80FF6600)), // Orange
    EOLO(Color(0x8000AEEF)), // Light Blue
    ILIAD(Color(0x80D52B1E)), // Red
    ZEFIRO(Color(0x804ccbda)), // Light Blue
    GENERIC(Color(0x80808080)); // Default Gray

    companion object {
        fun fromCompany(company: String): CompanyColor {
            return when {
                company.lowercase().contains("tim") -> TIM
                company.lowercase().contains("vodafone") -> VODAFONE
                company.lowercase().contains("wind tre") -> WINDTRE
                company.lowercase().contains("eolo") -> EOLO
                company.lowercase().contains("iliad") -> ILIAD
                company.lowercase().contains("zefiro") -> ZEFIRO
                else -> GENERIC
            }
        }
    }
}

@Composable
fun PermitCardPlaceholder() {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerAnim"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(x = translateAnim.value, y = translateAnim.value)
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        // Top bar placeholder
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        )

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Title placeholder
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )

            // Two lines
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Button placeholder
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(brush)
                    .align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(140.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PermitCardItem(
    apiAlboEntry: ApiAlboEntry,
    onDocumentClick: (url: String, title: String) -> Unit
) {
    val expanded = rememberSaveable { mutableStateOf(false) }

    ElevatedCard(
        onClick = {
            expanded.value = !expanded.value
        },
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large
    ) {
        // Top brand indicator
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(CompanyColor.fromCompany(apiAlboEntry.NOMEIMPRESA).color)
        )
        
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = apiAlboEntry.INDIRIZZO,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(expanded.value) {
                    Row {
                        Text("Comune: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                Text(apiAlboEntry.COMUNE_INDI, style = MaterialTheme.typography.bodyMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(expanded.value) {
                    Row {
                        Text("Operatore: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                Text(apiAlboEntry.NOMEIMPRESA, style = MaterialTheme.typography.bodyMedium)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AnimatedVisibility(expanded.value) {
                    Row {
                        Text("Inizio validità: ", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                Text(apiAlboEntry.DATAINIZIOVALIDITA, style = MaterialTheme.typography.bodyMedium)
            }

            AnimatedVisibility(
                expanded.value,
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    DetailRow(label = "Documento", value = apiAlboEntry.DOCUMENTO.ifEmpty { "-" })
                    DetailRow(label = "Stato", value = apiAlboEntry.ATTO_STATO.ifEmpty { "-" })
                    DetailRow(label = "Fine validità", value = apiAlboEntry.DATAFINEVALIDITA.ifEmpty { "-" })
                    DetailRow(label = "Data autorizzazione", value = apiAlboEntry.DATAAUTORIZZAZIONE.ifEmpty { "-" })
                    DetailRow(label = "Oggetto", value = apiAlboEntry.OGGETTODOC.ifEmpty { "-" })
                    DetailRow(label = "Tipo documento", value = apiAlboEntry.TIPODOC.ifEmpty { "-" })

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    
                    FilledTonalButton(
                        onClick = {
                            val url = "http://www.territorio.provincia.tn.it/gco/downloadFile.down?userFromRequestParam=portalpa&qpportal=true&codCompany=PROV_TN&idAllegato=${apiAlboEntry.DOCUMENTO}&idAtto=${apiAlboEntry.IDATTO}"
                            onDocumentClick(url, apiAlboEntry.ALLEGATONOMEFILE)
                        },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = apiAlboEntry.ALLEGATONOMEFILE,
                            style = MaterialTheme.typography.labelLarge
                        )
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
        apiAlboEntry = ApiAlboEntry(
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
        ),
        onDocumentClick = { _, _ -> }
    )
}
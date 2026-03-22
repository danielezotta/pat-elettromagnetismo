package it.danielezotta.patelettromagnetismo.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Satellite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.compose.SubcomposeAsyncImage
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.overlay.TilesOverlay
import org.osmdroid.tileprovider.MapTileProviderBasic
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer
import org.osmdroid.bonuspack.clustering.StaticCluster
import org.osmdroid.views.overlay.Polygon
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import it.danielezotta.patelettromagnetismo.models.RTVImpianto
import it.danielezotta.patelettromagnetismo.models.StazioneRadiobase
import it.danielezotta.patelettromagnetismo.models.ImpiantoDetail
import it.danielezotta.patelettromagnetismo.models.MisuraCEM
import it.danielezotta.patelettromagnetismo.network.ArcGISService
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.background
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.collectAsState
import org.osmdroid.events.MapListener
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.client.plugins.timeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.contentOrNull
import android.os.Handler
import android.os.Looper
import android.content.Context
import androidx.core.content.res.ResourcesCompat
import coil3.request.crossfade

// Helpers to safely extract values from JsonElement attributes
private fun JsonElement?.toIntSafe(): Int? = this?.jsonPrimitive?.intOrNull ?: this?.jsonPrimitive?.doubleOrNull?.toInt()
private fun JsonElement?.toDoubleSafe(): Double? = this?.jsonPrimitive?.doubleOrNull
private fun JsonElement?.toStringSafe(): String? = this?.jsonPrimitive?.contentOrNull
private fun drawableFromRes(context: Context, resId: Int) = ResourcesCompat.getDrawable(context.resources, resId, context.theme)

private fun createClusterIcon(context: Context, color: Int, count: Int = 0): Bitmap {
    val size = 96
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    
    // Draw outer circle with gradient effect
    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    
    // Draw inner white circle
    val innerPaint = Paint().apply {
        this.color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 8f, innerPaint)
    
    // Draw colored center circle
    val centerPaint = Paint().apply {
        this.color = color
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 16f, centerPaint)
    
    // Add shadow effect
    val shadowPaint = Paint().apply {
        this.color = Color.parseColor("#40000000")
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(size / 2f + 2f, size / 2f + 2f, size / 2f - 16f, shadowPaint)

    if (count > 0) {
        val textPaint = Paint().apply {
            this.color = Color.WHITE
            isAntiAlias = true
            textSize = size * 0.32f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
        }
        val textY = size / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(count.toString(), size / 2f, textY, textPaint)
    }

    return bitmap
}

private class CustomClusterer(context: Context, private val clusterColor: Int) : RadiusMarkerClusterer(context) {
    init {
        mTextPaint.color = Color.TRANSPARENT
    }
    override fun buildClusterMarker(cluster: StaticCluster, mapView: MapView): Marker {
        val marker = super.buildClusterMarker(cluster, mapView)
        val bitmap = createClusterIcon(mapView.context, clusterColor, cluster.size)
        marker.icon = android.graphics.drawable.BitmapDrawable(mapView.context.resources, bitmap)
        return marker
    }
}

private fun drawMapPin(canvas: Canvas, size: Int, pinColor: Int) {
    val cx = size / 2f
    val headR = size * 0.34f
    val headCY = headR + size * 0.06f
    val tipY = size * 0.94f

    // Drop shadow
    val shadow = Paint().apply {
        color = Color.parseColor("#44000000")
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    val shadowPath = android.graphics.Path().apply {
        addCircle(cx + 3f, headCY + 3f, headR, android.graphics.Path.Direction.CW)
        moveTo(cx - headR * 0.55f + 3f, headCY + headR * 0.7f)
        lineTo(cx + headR * 0.55f + 3f, headCY + headR * 0.7f)
        lineTo(cx + 3f, tipY + 4f)
        close()
    }
    canvas.drawPath(shadowPath, shadow)

    // Pin fill
    val fill = Paint().apply {
        color = pinColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    val pinPath = android.graphics.Path().apply {
        addCircle(cx, headCY, headR, android.graphics.Path.Direction.CW)
        moveTo(cx - headR * 0.55f, headCY + headR * 0.7f)
        lineTo(cx + headR * 0.55f, headCY + headR * 0.7f)
        lineTo(cx, tipY)
        close()
    }
    canvas.drawPath(pinPath, fill)

    // White inner circle
    val inner = Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, headCY, headR * 0.65f, inner)
}

private fun createRTVMarkerIcon(context: Context): Bitmap {
    val size = 128
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val headR = size * 0.3f
    val headCY = headR + size * 0.06f
    val accentColor = Color.parseColor("#E64A19") // deep orange

    drawMapPin(canvas, size, accentColor)

    // TV + radio symbol: small TV screen with radio waves
    val tvPaint = Paint().apply {
        color = accentColor
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        strokeCap = Paint.Cap.ROUND
    }
    val fillPaint = Paint().apply {
        color = accentColor
        isAntiAlias = true
        style = Paint.Style.FILL
        alpha = 40
    }
    // TV screen (small rectangle)
    val tvW = headR * 0.8f
    val tvH = headR * 0.5f
    val tvL = cx - tvW/2f
    val tvT = headCY - headR * 0.1f
    val tvR = cx + tvW/2f
    val tvB = tvT + tvH
    val tvRect = android.graphics.RectF(tvL, tvT, tvR, tvB)
    canvas.drawRect(tvRect, fillPaint)
    canvas.drawRect(tvRect, tvPaint)
    // TV stand
    canvas.drawLine(cx, tvB, cx, tvB + headR * 0.15f, tvPaint)
    // Radio waves on right side
    val wavePaint = Paint().apply {
        color = accentColor
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    val waveCx = tvR + headR * 0.15f
    val waveCy = tvT + tvH/2f
    listOf(6f to 2.5f, 10f to 2f, 14f to 1.5f).forEach { (r, w) ->
        wavePaint.strokeWidth = w
        val rect = android.graphics.RectF(waveCx - r, waveCy - r, waveCx + r, waveCy + r)
        canvas.drawArc(rect, 150f, 60f, false, wavePaint)
    }

    return bitmap
}

private fun createSRBMarkerIcon(context: Context): Bitmap {
    val size = 128
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val headR = size * 0.34f
    val headCY = headR + size * 0.06f
    val accentColor = Color.parseColor("#1565C0") // deep blue

    drawMapPin(canvas, size, accentColor)

    // Cell tower symbol: 3 stacked signal arcs (like wifi/cell bars)
    val arcPaint = Paint().apply {
        color = accentColor
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    val dotPaint = Paint().apply {
        color = accentColor
        isAntiAlias = true
        style = Paint.Style.FILL
    }
    // Center dot
    canvas.drawCircle(cx, headCY + headR * 0.30f, 4f, dotPaint)
    // 3 arcs expanding upward from dot
    val dotY = headCY + headR * 0.30f
    listOf(10f to 4f, 18f to 3.5f, 26f to 3f).forEach { (r, w) ->
        arcPaint.strokeWidth = w
        val rect = android.graphics.RectF(cx - r, dotY - r, cx + r, dotY + r)
        canvas.drawArc(rect, 200f, 140f, false, arcPaint)
    }

    return bitmap
}

private fun buildRTVDetails(impianto: RTVImpianto): List<Pair<String, String>> = buildList {
    fun line(label: String, value: String?) { if (!value.isNullOrBlank()) add(label to value) }
    line("Nome Impianto", impianto.nomeImpianto)
    line("Comune", impianto.comune)
    line("Provincia", impianto.provincia)
    line("Indirizzo", impianto.indirizzo)
    line("Tipologia", impianto.tipologia)
    line("Tipo Antenna", impianto.tipoAntennaSorgente)
    line("Frequenza", impianto.frequenza)
    line("Potenza", impianto.potenza)
    if (impianto.idSostegno != null) add("ID Sostegno" to impianto.idSostegno.toString())
    line("Cod. Comune Sostegno", impianto.codiceComuneSostegno)
    add("Object ID" to impianto.objectId.toString())
    if (impianto.geometry != null) add("Coordinate" to "${impianto.geometry.y}, ${impianto.geometry.x}")
    if (impianto.coordinateXSostegno != null && impianto.coordinateYSostegno != null)
        add("Coord. Sostegno" to "${impianto.coordinateYSostegno}, ${impianto.coordinateXSostegno}")
}

private fun buildSRBDetails(stazione: StazioneRadiobase): List<Pair<String, String>> = buildList {
    fun line(label: String, value: String?) { if (!value.isNullOrBlank()) add(label to value) }
    line("Comune", stazione.comune)
    line("Operatore", stazione.operatore)
    line("Tecnologia", stazione.tecnologia)
    line("Frequenza", stazione.frequenza)
    line("Potenza", stazione.potenza)
    add("ID Sostegno" to stazione.idSostegno.toString())
    if (stazione.istat != null) add("Istat" to stazione.istat.toString())
    add("Coord. Sostegno" to "${stazione.ySostegno}, ${stazione.xSostegno}")
    if (stazione.geometry != null) add("Coordinate" to "${stazione.geometry.y}, ${stazione.geometry.x}")
}

private fun buildCEMDetails(misura: MisuraCEM): List<Pair<String, String>> = buildList {
    fun line(label: String, value: String?) { if (!value.isNullOrBlank()) add(label to value) }
    line("Comune", misura.comune)
    line("Luogo", misura.luogo)
    line("Data/Ora", misura.dataOra)
    line("MED V/m", misura.medVm)
    line("Durata", misura.durata)
    line("Quota terreno", misura.quotaTerr)
    line("Grafico", misura.grafico)
    if (misura.anno != null) add("Anno" to misura.anno.toString())
    add("Object ID" to misura.objectId.toString())
    if (misura.geometry != null) add("Coordinate" to "${misura.geometry.y}, ${misura.geometry.x}")
}

private fun createCEMMarkerIcon(context: Context): Bitmap {
    val size = 128
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = size / 2f
    val headR = size * 0.34f
    val headCY = headR + size * 0.06f
    val accentColor = Color.parseColor("#2E7D32") // deep green

    drawMapPin(canvas, size, accentColor)

    // EM wave symbol: two smooth sine-like arcs
    val wavePaint = Paint().apply {
        color = accentColor
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4.5f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    val waveW = headR * 0.90f
    val waveH = headR * 0.30f
    val waveY = headCY
    // Top wave
    val topPath = android.graphics.Path().apply {
        moveTo(cx - waveW, waveY - waveH * 0.5f)
        cubicTo(cx - waveW * 0.5f, waveY - waveH * 2f,
                cx + waveW * 0.5f, waveY + waveH * 2f,
                cx + waveW, waveY - waveH * 0.5f)
    }
    canvas.drawPath(topPath, wavePaint)
    // Bottom wave (offset)
    val botPath = android.graphics.Path().apply {
        moveTo(cx - waveW, waveY + waveH * 0.5f)
        cubicTo(cx - waveW * 0.5f, waveY - waveH * 1.2f,
                cx + waveW * 0.5f, waveY + waveH * 1.2f,
                cx + waveW, waveY + waveH * 0.5f)
    }
    val thinWave = Paint(wavePaint).apply { strokeWidth = 3f; alpha = 160 }
    canvas.drawPath(botPath, thinWave)

    return bitmap
}

@Composable
private fun MapViewContent(
    rtvImpianti: List<RTVImpianto>,
    stazioniRadiobase: List<StazioneRadiobase>,
    misureCEM: List<MisuraCEM>,
    showRTV: Boolean,
    showSRB: Boolean,
    showCEM: Boolean,
    directionOverlayTrigger: Triple<Int, List<ImpiantoDetail>, Set<Int>>?,
    onMapViewCreated: (MapView) -> Unit,
    onMarkerClick: (String, List<Pair<String,String>>, Int?, Boolean) -> Unit
) {
    val context = LocalContext.current
    val mapView = remember {
        // Configure OSMDroid once
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)
        }.also { onMapViewCreated(it) }
    }
    val rtvClusterer = remember {
        CustomClusterer(context, Color.parseColor("#FF5722")).apply {
            setRadius(100)
            setMaxClusteringZoomLevel(12)
        }
    }
    val srbClusterer = remember {
        CustomClusterer(context, Color.parseColor("#2196F3")).apply {
            setRadius(100)
            setMaxClusteringZoomLevel(12)
        }
    }
    val cemClusterer = remember {
        CustomClusterer(context, Color.parseColor("#4CAF50")).apply {
            setRadius(100)
            setMaxClusteringZoomLevel(12)
        }
    }
    val currentZoom = remember { mutableStateOf(6.0) }
    var hasCenteredMap by remember { mutableStateOf(false) }

    // Add overlays once
    LaunchedEffect(Unit) {
        if (!mapView.overlays.contains(rtvClusterer)) {
            mapView.overlays.add(rtvClusterer)
        }
        if (!mapView.overlays.contains(srbClusterer)) {
            mapView.overlays.add(srbClusterer)
        }
        if (!mapView.overlays.contains(cemClusterer)) {
            mapView.overlays.add(cemClusterer)
        }
    }

    // Draw direction cones when SRB operator data arrives
    LaunchedEffect(directionOverlayTrigger) {
        mapView.overlays.removeAll(mapView.overlays.filterIsInstance<Polygon>())
        if (directionOverlayTrigger != null) {
            val (srbObjectId, operators, hiddenIds) = directionOverlayTrigger
            val stazione = stazioniRadiobase.find { it.objectId == srbObjectId }
            if (stazione?.geometry != null) {
                val origin = GeoPoint(stazione.geometry.y, stazione.geometry.x)
                val coneLengthDeg = 0.010

                // Collect all (bearing, opIndex) to detect overlaps and stagger them
                data class ConeEntry(val bearing: Double, val opIndex: Int, val objectId: Int)
                val allCones = mutableListOf<ConeEntry>()
                operators.forEachIndexed { idx, op ->
                    if (op.objectId !in hiddenIds) {
                        val degrees = op.direzioni?.split("/")?.mapNotNull { it.trim().toDoubleOrNull() } ?: emptyList()
                        degrees.forEach { b -> allCones.add(ConeEntry(b, idx, op.objectId)) }
                    }
                }

                // Group by bearing and add random offset for overlapping lines
                val bearingGroups = allCones.groupBy { cone ->
                    // Round bearing to nearest degree to group similar bearings
                    cone.bearing.toInt()
                }
                
                bearingGroups.forEach { (_, cones) ->
                    if (cones.size == 1) {
                        // Single line, no offset needed
                        val cone = cones.first()
                        val op = operators[cone.opIndex]
                        val companyColor = op.ragioneSociale?.let { CompanyColor.fromCompany(it) } ?: CompanyColor.GENERIC
                        val baseColor = companyColor.color.copy(alpha = 1f).toArgb()
                        val r = android.graphics.Color.red(baseColor)
                        val g = android.graphics.Color.green(baseColor)
                        val b = android.graphics.Color.blue(baseColor)
                        
                        val bearingRad = Math.toRadians(cone.bearing)
                        val tipLat = origin.latitude + coneLengthDeg * kotlin.math.cos(bearingRad)
                        val tipLon = origin.longitude + coneLengthDeg * kotlin.math.sin(bearingRad)
                        
                        val line = Polygon(mapView)
                        line.points = listOf(origin, GeoPoint(tipLat, tipLon), GeoPoint(tipLat, tipLon))
                        line.outlinePaint.color = android.graphics.Color.argb(200, r, g, b)
                        line.outlinePaint.strokeWidth = 8.0f
                        line.fillPaint.color = android.graphics.Color.TRANSPARENT
                        mapView.overlays.add(line)
                    } else {
                        // Multiple lines with same bearing, add incremental offsets
                        val strokeWidth = 8.0f
                        val offsetDegrees = strokeWidth * 0.000010 // Convert stroke width to degree offset
                        cones.forEachIndexed { index, cone ->
                            val op = operators[cone.opIndex]
                            val companyColor = op.ragioneSociale?.let { CompanyColor.fromCompany(it) } ?: CompanyColor.GENERIC
                            val baseColor = companyColor.color.copy(alpha = 1f).toArgb()
                            val r = android.graphics.Color.red(baseColor)
                            val g = android.graphics.Color.green(baseColor)
                            val b = android.graphics.Color.blue(baseColor)
                            
                            // Calculate incremental offset: center line, then alternating sides
                            val offsetIndex = index - (cones.size - 1) / 2.0 // Center around 0
                            val offsetDegreesTotal = offsetIndex * offsetDegrees
                            val offsetRad = Math.toRadians(cone.bearing + 90) // perpendicular direction
                            val offsetLat = offsetDegreesTotal * kotlin.math.cos(offsetRad)
                            val offsetLon = offsetDegreesTotal * kotlin.math.sin(offsetRad)
                            
                            val bearingRad = Math.toRadians(cone.bearing)
                            val tipLat = origin.latitude + coneLengthDeg * kotlin.math.cos(bearingRad) + offsetLat
                            val tipLon = origin.longitude + coneLengthDeg * kotlin.math.sin(bearingRad) + offsetLon
                            val offsetOrigin = GeoPoint(origin.latitude + offsetLat, origin.longitude + offsetLon)
                            
                            val line = Polygon(mapView)
                            line.points = listOf(offsetOrigin, GeoPoint(tipLat, tipLon), GeoPoint(tipLat, tipLon))
                            line.outlinePaint.color = android.graphics.Color.argb(200, r, g, b)
                            line.outlinePaint.strokeWidth = strokeWidth
                            line.fillPaint.color = android.graphics.Color.TRANSPARENT
                            mapView.overlays.add(line)
                        }
                    }
                }
                mapView.invalidate()
            }
        } else {
            mapView.invalidate()
        }
    }

    // Debounce rapid changes and only update when zoom is stable
    LaunchedEffect(rtvImpianti, stazioniRadiobase, misureCEM, showRTV, showSRB, showCEM, currentZoom.value) {
        try {
            updateMapOverlays(
                mapView, rtvClusterer, srbClusterer, cemClusterer,
                rtvImpianti, stazioniRadiobase, misureCEM, showRTV, showSRB, showCEM,
                currentZoom.value, context, hasCenteredMap, onMarkerClick
            )
            if (!hasCenteredMap && (rtvImpianti.isNotEmpty() || stazioniRadiobase.isNotEmpty())) {
                hasCenteredMap = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Track zoom changes
    LaunchedEffect(Unit) {
        mapView.addOnFirstLayoutListener { _, _, _, _, _ ->
            currentZoom.value = mapView.zoomLevelDouble
        }
        mapView.addMapListener(object : MapListener {
            override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                currentZoom.value = mapView.zoomLevelDouble
                return true
            }
            override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean = true
        })
    }

    AndroidView(
        factory = { mapView },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun OSMElectromagneticMapView(
    arcGISService: ArcGISService,
    pendingMapSearch: StateFlow<String?> = MutableStateFlow(null),
    onMapSearchConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var rtvImpianti by remember { mutableStateOf<List<RTVImpianto>>(emptyList()) }
    var stazioniRadiobase by remember { mutableStateOf<List<StazioneRadiobase>>(emptyList()) }
    var misureCEM by remember { mutableStateOf<List<MisuraCEM>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var showRTV by remember { mutableStateOf(true) }
    var showSRB by remember { mutableStateOf(true) }
    var showCEM by remember { mutableStateOf(true) }
    var isSatellite by remember { mutableStateOf(false) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var selectedMarker by remember { mutableStateOf<Triple<String, List<Pair<String,String>>, Int?>?>(null) } // title, details, objectId
    var selectedMarkerIsSRB by remember { mutableStateOf<Boolean>(false) }
    var relatedRecords by remember { mutableStateOf<List<ImpiantoDetail>>(emptyList()) }
    var isLoadingDetails by remember { mutableStateOf(false) }
    var directionOverlayTrigger by remember { mutableStateOf<Triple<Int, List<ImpiantoDetail>, Set<Int>>?>(null) }
    var hiddenOperatorIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var cemAttachmentUrl by remember { mutableStateOf<String?>(null) }
    var isLoadingChart by remember { mutableStateOf(false) }
    var showImagePreview by remember { mutableStateOf(false) }
    var previewImageUrl by remember { mutableStateOf<String?>(null) }

    val searchQuery by pendingMapSearch.collectAsState()
    LaunchedEffect(searchQuery) {
        val query = searchQuery ?: return@LaunchedEffect
        val mapView = mapViewRef ?: return@LaunchedEffect
                
        // Add a small delay to ensure map is ready
        delay(100)
        
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1&countrycodes=it"
            
            val response = withContext(Dispatchers.IO) {
                HttpClient(CIO).use { client ->
                    client.get(url) {
                        header("User-Agent", "PATPermessiElettromagnetismo/1.0")
                        timeout {
                            requestTimeoutMillis = 5000
                        }
                    }.bodyAsText()
                }
            }
            
            val json = Json.parseToJsonElement(response).jsonArray
            if (json.isNotEmpty()) {
                val first = json[0].jsonObject
                val lat = first["lat"]?.jsonPrimitive?.content?.toDoubleOrNull()
                val lon = first["lon"]?.jsonPrimitive?.content?.toDoubleOrNull()
                android.util.Log.d("MapSearch", "Parsed coordinates: lat=$lat, lon=$lon")
                
                if (lat != null && lon != null) {
                    withContext(Dispatchers.Main) {
                        android.util.Log.d("MapSearch", "Animating map to: $lat, $lon")
                        mapView.controller.animateTo(GeoPoint(lat, lon))
                        mapView.controller.setZoom(16.0)
                    }
                } else {
                    android.util.Log.w("MapSearch", "Failed to parse coordinates from response")
                }
            } else {
            }
        } catch (e: Exception) {            
            // If geocoding fails, try a simple search by comune only
            val comuneOnly = query.split(",").lastOrNull()?.trim()
            if (comuneOnly != null && comuneOnly != query) {
                android.util.Log.d("MapSearch", "Trying fallback with comune only: $comuneOnly")
                delay(100)
                try {
                    val encoded = java.net.URLEncoder.encode(comuneOnly, "UTF-8")
                    val url = "https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=1&countrycodes=it"
                    android.util.Log.d("MapSearch", "Fallback URL: $url")
                    
                    val response = withContext(Dispatchers.IO) {
                        HttpClient(CIO).use { client ->
                            client.get(url) {
                                header("User-Agent", "PATPermessiElettromagnetismo/1.0")
                                timeout {
                                    requestTimeoutMillis = 5000
                                }
                            }.bodyAsText()
                        }
                    }
                    android.util.Log.d("MapSearch", "Fallback response: $response")
                    
                    val json = Json.parseToJsonElement(response).jsonArray
                    if (json.isNotEmpty()) {
                        val first = json[0].jsonObject
                        val lat = first["lat"]?.jsonPrimitive?.content?.toDoubleOrNull()
                        val lon = first["lon"]?.jsonPrimitive?.content?.toDoubleOrNull()
                        android.util.Log.d("MapSearch", "Fallback coordinates: lat=$lat, lon=$lon")
                        
                        if (lat != null && lon != null) {
                            withContext(Dispatchers.Main) {
                                android.util.Log.d("MapSearch", "Animating map to fallback: $lat, $lon")
                                mapView.controller.animateTo(GeoPoint(lat, lon))
                                mapView.controller.setZoom(14.0)
                            }
                        }
                    }
                } catch (e2: Exception) {
                }
            }
        } finally {
            // Delay consumption to ensure the animation completes
            delay(500)
            android.util.Log.d("MapSearch", "Consuming search query")
            onMapSearchConsumed()
        }
    }

    val esriSatellite = remember {
        object : OnlineTileSourceBase(
            "Esri_WorldImagery",
            0, 19, 256, ".jpg",
            arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/"),
            "Tiles \u00A9 Esri \u2014 Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"
        ) {
            override fun getTileURLString(pMapTileIndex: Long): String {
                val zoom = MapTileIndex.getZoom(pMapTileIndex)
                val x = MapTileIndex.getX(pMapTileIndex)
                val y = MapTileIndex.getY(pMapTileIndex)
                return "$baseUrl$zoom/$y/$x"
            }
        }
    }

    LaunchedEffect(isSatellite, mapViewRef) {
        val mapView = mapViewRef ?: return@LaunchedEffect
        if (isSatellite) {
            mapView.setTileSource(esriSatellite)
            val alreadyAdded = mapView.overlays.any { it is TilesOverlay }
            if (!alreadyAdded) {
                val labelsSource = object : OnlineTileSourceBase(
                    "Esri_WorldBoundariesAndPlaces",
                    0, 19, 256, ".png",
                    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/"),
                    "Tiles \u00A9 Esri \u2014 Source: Esri, i-cubed, USDA, USGS, AEX, GeoEye, Getmapping, Aerogrid, IGN, IGP, UPR-EGP, and the GIS User Community"
                ) {
                    override fun getTileURLString(pMapTileIndex: Long): String {
                        val zoom = MapTileIndex.getZoom(pMapTileIndex)
                        val x = MapTileIndex.getX(pMapTileIndex)
                        val y = MapTileIndex.getY(pMapTileIndex)
                        return "${baseUrl}$zoom/$y/$x"
                    }
                }
                val provider = MapTileProviderBasic(mapView.context, labelsSource)
                val roadsOverlay = TilesOverlay(provider, mapView.context).apply {
                    loadingBackgroundColor = android.graphics.Color.TRANSPARENT
                    loadingLineColor = android.graphics.Color.TRANSPARENT
                }
                mapView.overlays.add(0, roadsOverlay)
            }
        } else {
            mapView.setTileSource(TileSourceFactory.MAPNIK)
            mapView.overlays.removeAll { it is TilesOverlay }
        }
        mapView.invalidate()
    }

    LaunchedEffect(Unit) {
        try {
            isLoading = true
            error = null

            val (rtvResponse, srbResponse, cemResponse) = withContext(Dispatchers.IO) {
                    val rtv = arcGISService.getRTVImpianti(
                    where = "1=1",
                    outFields = "*",
                    returnGeometry = true,
                    outSR = 4326,
                    resultRecordCount = 1000
                )

                val srb = arcGISService.getStazioniRadiobase(
                    where = "1=1",
                    outFields = "*",
                    returnGeometry = true,
                    outSR = 4326,
                    resultRecordCount = 1000
                )

                val cem = arcGISService.getMisureCEM(
                    where = "1=1",
                    outFields = "*",
                    returnGeometry = true,
                    outSR = 4326,
                    resultRecordCount = 2000
                )

                Triple(rtv, srb, cem)
            }

            rtvImpianti = rtvResponse?.features?.mapNotNull { feature ->
                try {
                    val attrs = feature.attributes
                    RTVImpianto(
                        objectId = attrs["OBJECTID"].toIntSafe() ?: return@mapNotNull null,
                        nomeImpianto = attrs["Nome_Impianto"].toStringSafe(),
                        comune = attrs["Comune"].toStringSafe(),
                        provincia = attrs["Provincia"].toStringSafe(),
                        indirizzo = attrs["Indirizzo"].toStringSafe(),
                        tipologia = attrs["Tipologia"].toStringSafe(),
                        frequenza = attrs["Frequenza"].toStringSafe(),
                        potenza = attrs["Potenza"].toStringSafe(),
                        tipoAntennaSorgente = attrs["Tipo_Antenna_Sorgente"].toStringSafe(),
                        coordinateXSostegno = attrs["Coordinate_X_Sostegno"].toDoubleSafe(),
                        coordinateYSostegno = attrs["Coordinate_Y_Sostegno"].toDoubleSafe(),
                        codiceComuneSostegno = attrs["Codice_Comune_Sostegno"].toStringSafe(),
                        idSostegno = attrs["ID_Sostegno"].toIntSafe(),
                        geometry = feature.geometry
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            stazioniRadiobase = srbResponse?.features?.mapNotNull { feature ->
                try {
                    val attrs = feature.attributes
                    StazioneRadiobase(
                        objectId = attrs["OBJECTID"].toIntSafe() ?: return@mapNotNull null,
                        idSostegno = attrs["ID_Sostegno"].toIntSafe() ?: attrs["ID_Sostegno"].toDoubleSafe()?.toInt() ?: return@mapNotNull null,
                        xSostegno = attrs["X_Sostegno"].toDoubleSafe() ?: return@mapNotNull null,
                        ySostegno = attrs["Y_Sostegno"].toDoubleSafe() ?: return@mapNotNull null,
                        comune = attrs["Comune"].toStringSafe(),
                        istat = attrs["Istat"].toIntSafe(),
                        operatore = attrs["Operatore"].toStringSafe(),
                        tecnologia = attrs["Tecnologia"].toStringSafe(),
                        frequenza = attrs["Frequenza"].toStringSafe(),
                        potenza = attrs["Potenza"].toStringSafe(),
                        geometry = feature.geometry
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()
            
            misureCEM = cemResponse?.features?.mapNotNull { feature ->
                try {
                    val attrs = feature.attributes
                    MisuraCEM(
                        objectId = attrs["OBJECTID"].toIntSafe() ?: return@mapNotNull null,
                        dataOra = attrs["Data_ora"].toStringSafe(),
                        medVm = attrs["MED_Vm"].toStringSafe(),
                        durata = attrs["Durata"].toStringSafe(),
                        comune = attrs["Comune"].toStringSafe(),
                        quotaTerr = attrs["Quota_terr"].toStringSafe(),
                        luogo = attrs["Luogo"].toStringSafe(),
                        grafico = attrs["Grafico"].toStringSafe(),
                        anno = attrs["Anno"].toIntSafe(),
                        geometry = feature.geometry
                    )
                } catch (e: Exception) {
                    null
                }
            } ?: emptyList()

            isLoading = false
        } catch (e: Exception) {
            error = e.message
            isLoading = false
        }
    }

    // Fetch CEM attachment when CEM marker is selected
    val selectedMarkerTitle = selectedMarker?.first
    val selectedMarkerDetails = selectedMarker?.second
    val isCEMMarker = selectedMarkerTitle?.contains("Misura CEM") == true || 
                      selectedMarkerDetails?.any { it.first == "Grafico" } == true
    
    LaunchedEffect(isCEMMarker, selectedMarkerDetails) {
        if (isCEMMarker && selectedMarkerDetails != null) {
            // Find the CEM object ID from details
            val objectIdField = selectedMarkerDetails.find { it.first == "Object ID" }?.second
            if (objectIdField != null) {
                try {
                    isLoadingChart = true
                    cemAttachmentUrl = null
                    
                    // First get attachment info
                    val attachmentResponse = arcGISService.getCEMAttachments(objectIdField.toInt())
                    val attachmentId = attachmentResponse?.attachmentGroups
                        ?.firstOrNull()?.attachmentInfos
                        ?.firstOrNull()?.id
                    
                    if (attachmentId != null) {
                        // Then get the actual attachment URL
                        val attachmentUrl = arcGISService.getCEMAttachmentUrl(objectIdField.toInt(), attachmentId)
                        cemAttachmentUrl = attachmentUrl
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CEMAttachment", "Error loading CEM attachment", e)
                } finally {
                    isLoadingChart = false
                }
            }
        } else {
            cemAttachmentUrl = null
        }
    }

    // Fetch related records when SRB marker is selected
    val selectedObjectId = selectedMarker?.third
    LaunchedEffect(selectedObjectId) {
        if (selectedObjectId != null) {
            try {
                isLoadingDetails = true
                relatedRecords = emptyList()
                val relatedResponse = arcGISService.getRelatedRecords(
                    objectId = selectedObjectId,
                    relationshipId = 0,
                    outFields = "Ragione_sociale,Impianto,OBJECTID"
                )
                val details = mutableListOf<ImpiantoDetail>()
                relatedResponse?.relatedRecordGroups?.firstOrNull()?.relatedRecords?.forEach { record ->
                    val impiantoId = record.attributes["OBJECTID"]?.toIntSafe()
                    if (impiantoId != null) {
                        val ragioneSociale = record.attributes["Ragione_sociale"]?.toStringSafe()
                        val impianto = record.attributes["Impianto"]?.toIntSafe()
                        val impiantoDetail = arcGISService.getImpiantoDetails(impiantoId)
                        details.add(impiantoDetail ?: ImpiantoDetail(
                            objectId = impiantoId,
                            direzioni = null,
                            ragioneSociale = ragioneSociale,
                            impianto = impianto
                        ))
                    }
                }
                relatedRecords = details
                hiddenOperatorIds = emptySet()
                directionOverlayTrigger = Triple(selectedObjectId, details, emptySet<Int>())
            } catch (e: Exception) {
            } finally {
                isLoadingDetails = false
            }
        } else {
            relatedRecords = emptyList()
            hiddenOperatorIds = emptySet()
            directionOverlayTrigger = null
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(text = "Error: $error", modifier = Modifier.align(Alignment.Center))
            }
        } else {
            // Map area
            Box(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
            ) {
                MapViewContent(
                    rtvImpianti = rtvImpianti,
                    stazioniRadiobase = stazioniRadiobase,
                    misureCEM = misureCEM,
                    showRTV = showRTV,
                    showSRB = showSRB,
                    showCEM = showCEM,
                    directionOverlayTrigger = directionOverlayTrigger?.let { Triple(it.first, it.second, hiddenOperatorIds) },
                    onMapViewCreated = { mapViewRef = it },
                    onMarkerClick = { title, details, objectId, isSRB ->
                        val isCEMMarker = title.contains("Misura CEM") || details.any { it.first == "Grafico" }
                        val newObjectId = if (isSRB || isCEMMarker) objectId else null // Keep for SRB and CEM, null for RTV
                        selectedMarker = Triple(title, details, newObjectId)
                        selectedMarkerIsSRB = isSRB
                        if (!isSRB) relatedRecords = emptyList()
                    }
                )

                // Satellite toggle button (top-end)
                SmallFloatingActionButton(
                    onClick = { isSatellite = !isSatellite },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    containerColor = if (isSatellite)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        imageVector = if (isSatellite) Icons.Default.Map else Icons.Default.Satellite,
                        contentDescription = if (isSatellite) "Vista mappa" else "Vista satellite",
                        tint = if (isSatellite)
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Layer toggle bar (top of map)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    shape = MaterialTheme.shapes.medium,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = showRTV, onCheckedChange = { showRTV = it },
                            modifier = Modifier.size(20.dp))
                        Text(text = "RTV", style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 4.dp, end = 12.dp))
                        Checkbox(checked = showSRB, onCheckedChange = { showSRB = it },
                            modifier = Modifier.size(20.dp))
                        Text(text = "SRB", style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 4.dp, end = 12.dp))
                        Checkbox(checked = showCEM, onCheckedChange = { showCEM = it },
                            modifier = Modifier.size(20.dp))
                        Text(text = "CEM", style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(start = 4.dp))
                    }
                }

                // Zoom controls (bottom-end of map)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                ) {
                    SmallFloatingActionButton(
                        onClick = { mapViewRef?.controller?.zoomIn() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) { Icon(Icons.Default.Add, contentDescription = "Zoom In") }
                    Spacer(modifier = Modifier.height(8.dp))
                    SmallFloatingActionButton(
                        onClick = { mapViewRef?.controller?.zoomOut() },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) { Icon(Icons.Default.Remove, contentDescription = "Zoom Out") }
                }
            }

            // Inline details panel
            AnimatedVisibility(
                visible = selectedMarker != null,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                selectedMarker?.let { (title, details, objectId) ->
                    MarkerDetailsPanel(
                        title = title,
                        details = details,
                        relatedRecords = relatedRecords,
                        isLoadingDetails = isLoadingDetails,
                        isSRB = selectedMarkerIsSRB,
                        hiddenOperatorIds = hiddenOperatorIds,
                        cemAttachmentUrl = cemAttachmentUrl,
                        isLoadingChart = isLoadingChart,
                        onToggleOperator = { operatorId ->
                            hiddenOperatorIds = if (operatorId in hiddenOperatorIds) {
                                hiddenOperatorIds - operatorId
                            } else {
                                hiddenOperatorIds + operatorId
                            }
                        },
                        onDismiss = {
                            selectedMarker = null
                            selectedMarkerIsSRB = false
                            cemAttachmentUrl = null
                            directionOverlayTrigger = null
                        },
                        onImagePreview = { url ->
                            previewImageUrl = url
                            showImagePreview = true
                        }
                    )
                }
            }
        }
    }

    // Image Preview Modal
    if (showImagePreview && previewImageUrl != null) {
        ImagePreviewDialog(
            imageUrl = previewImageUrl!!,
            onDismiss = { 
                showImagePreview = false
                previewImageUrl = null
            }
        )
    }
}

@Composable
private fun MarkerDetailsPanel(
    title: String,
    details: List<Pair<String, String>>,
    relatedRecords: List<ImpiantoDetail>,
    isLoadingDetails: Boolean,
    isSRB: Boolean,
    hiddenOperatorIds: Set<Int> = emptySet(),
    cemAttachmentUrl: String? = null,
    isLoadingChart: Boolean = false,
    showImagePreview: Boolean = false,
    previewImageUrl: String? = null,
    onToggleOperator: (Int) -> Unit = {},
    onDismiss: () -> Unit,
    onImagePreview: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.45f),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Handle bar + title row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            HorizontalDivider()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Station/marker details - one row per field
                details.forEach { (label, value) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            softWrap = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }

                if (isSRB) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isLoadingDetails) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Caricamento operatori...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (relatedRecords.isNotEmpty()) {
                        Text(
                            text = "Operatori (${relatedRecords.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        relatedRecords.forEach { record ->
                            val cardColor = record.ragioneSociale
                                ?.let { CompanyColor.fromCompany(it).color }
                                ?: CompanyColor.GENERIC.color
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                shape = MaterialTheme.shapes.small,
                                tonalElevation = 2.dp
                            ) {
                                Row {
                                    // Colored left border
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(4.dp)
                                            .background(cardColor)
                                    )
                                    Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 7.dp)) {
                                    if (record.ragioneSociale != null) {
                                        Text(
                                            text = record.ragioneSociale,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )
                                    }
                                    if (record.direzioni != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val degrees = record.direzioni
                                            .split("/")
                                            .mapNotNull { it.trim().toIntOrNull() }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 1.dp)
                                        ) {
                                            Text(
                                                text = "Dir: ",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            degrees.forEach { deg ->
                                                Text(
                                                    text = "${directionArrow(deg)} ${deg}°",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.padding(end = 6.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (record.impianto != null) {
                                        Text(
                                            text = "Impianto: ${record.impianto}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    }   // end Column
                                    // Eye toggle (only for SRB operators with directions)
                                    if (record.direzioni != null) {
                                        val isVisible = record.objectId !in hiddenOperatorIds
                                        IconButton(
                                            onClick = { onToggleOperator(record.objectId) },
                                            modifier = Modifier.size(32.dp).align(Alignment.CenterVertically)
                                        ) {
                                            Icon(
                                                imageVector = if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (isVisible) "Nascondi" else "Mostra",
                                                modifier = Modifier.size(18.dp),
                                                tint = if (isVisible) cardColor else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                        }
                                    }
                                }
                            }
                        }   // end forEach
                    }   // end else if
                }   // end if isSRB

                // CEM Chart display
                if (!isSRB && title.contains("Misura CEM") || details.any { it.first == "Grafico" }) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Grafico Misura CEM",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isLoadingChart) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Caricamento grafico...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (cemAttachmentUrl != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            shape = MaterialTheme.shapes.small,
                            tonalElevation = 2.dp
                        ) {
                            SubcomposeAsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(cemAttachmentUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Grafico misura CEM",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onTap = {
                                                cemAttachmentUrl?.let { url ->
                                                    onImagePreview(url)
                                                }
                                            }
                                        )
                                    },
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                                loading = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    }
                                },
                                error = {
                                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Error",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = "Errore caricamento grafico",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    } else {
                        // No attachment available
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            tonalElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VisibilityOff,
                                    contentDescription = "No attachment",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Nessun grafico disponibile per questa misura",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }   // end inner Column
        }   // end outer Column
    }   // end Surface
}   // end MarkerDetailsPanel

private fun directionArrow(degrees: Int): String {
    val normalized = ((degrees % 360) + 360) % 360
    return when {
        normalized < 23  -> "⬆️"
        normalized < 68  -> "↗️"
        normalized < 113 -> "➡️"
        normalized < 158 -> "↘️"
        normalized < 203 -> "⬇️"
        normalized < 248 -> "↙️"
        normalized < 293 -> "⬅️"
        normalized < 338 -> "↖️"
        else             -> "⬆️"
    }
}

// Simple debounce helper
@Composable
fun rememberDebounce(delayMs: Long): (()->Unit)->Unit {
    val handler = remember { Handler(Looper.getMainLooper()) }
    val job = remember { mutableStateOf<Job?>(null) }
    return { action ->
        job.value?.cancel()
        job.value = CoroutineScope(Dispatchers.Main).launch {
            delay(delayMs)
            action()
        }
    }
}

// Helper to cull markers by zoom level to keep UI responsive
private fun shouldShowMarker(zoom: Double): Boolean = true

// Extracted heavy work to a top-level suspend function
private suspend fun updateMapOverlays(
    mapView: MapView,
    rtvClusterer: RadiusMarkerClusterer?,
    srbClusterer: RadiusMarkerClusterer?,
    cemClusterer: RadiusMarkerClusterer?,
    rtvImpianti: List<RTVImpianto>,
    stazioniRadiobase: List<StazioneRadiobase>,
    misureCEM: List<MisuraCEM>,
    showRTV: Boolean,
    showSRB: Boolean,
    showCEM: Boolean,
    zoom: Double,
    context: Context,
    hasCenteredMap: Boolean,
    onMarkerClick: (String, List<Pair<String,String>>, Int?, Boolean) -> Unit
) {
    // Build marker data off the main thread (no MapView/clusterer access)
    data class MarkerData(
        val position: GeoPoint,
        val title: String,
        val details: List<Pair<String, String>>,
        val objectId: Int?,
        val isSRB: Boolean
    )

    val rtvData = withContext(Dispatchers.Default) {
        if (showRTV && shouldShowMarker(zoom)) {
            rtvImpianti.mapNotNull { impianto ->
                impianto.geometry?.let { geometry ->
                    MarkerData(
                        position = GeoPoint(geometry.y, geometry.x),
                        title = impianto.nomeImpianto ?: impianto.comune ?: "RTV Impianto",
                        details = buildRTVDetails(impianto),
                        objectId = impianto.objectId,
                        isSRB = false
                    )
                }
            }
        } else emptyList()
    }

    val srbData = withContext(Dispatchers.Default) {
        if (showSRB && shouldShowMarker(zoom)) {
            stazioniRadiobase.mapNotNull { stazione ->
                stazione.geometry?.let { geometry ->
                    MarkerData(
                        position = GeoPoint(geometry.y, geometry.x),
                        title = stazione.comune ?: "Stazione Radiobase",
                        details = buildSRBDetails(stazione),
                        objectId = stazione.objectId,
                        isSRB = true
                    )
                }
            }
        } else emptyList()
    }

    val cemData = withContext(Dispatchers.Default) {
        if (showCEM && shouldShowMarker(zoom)) {
            misureCEM.mapNotNull { misura ->
                misura.geometry?.let { geometry ->
                    MarkerData(
                        position = GeoPoint(geometry.y, geometry.x),
                        title = misura.luogo ?: misura.comune ?: "Misura CEM",
                        details = buildCEMDetails(misura),
                        objectId = misura.objectId,
                        isSRB = false
                    )
                }
            }
        } else emptyList()
    }

    // All MapView and clusterer mutations must happen on the main thread
    withContext(Dispatchers.Main) {
        val rtvMarkerIcon = BitmapDrawable(context.resources, createRTVMarkerIcon(context))
        val srbMarkerIcon = BitmapDrawable(context.resources, createSRBMarkerIcon(context))
        val cemMarkerIcon = BitmapDrawable(context.resources, createCEMMarkerIcon(context))

        // Clear existing markers and clusterers
        mapView.overlays.removeAll(mapView.overlays.filterIsInstance<Marker>())
        rtvClusterer?.items?.clear()
        srbClusterer?.items?.clear()
        cemClusterer?.items?.clear()

        val points = mutableListOf<GeoPoint>()

        rtvData.forEach { data ->
            val marker = Marker(mapView)
            marker.position = data.position
            marker.title = data.title
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = rtvMarkerIcon
            marker.setOnMarkerClickListener { _, _ ->
                mapView.controller.animateTo(marker.position)
                mapView.controller.setZoom(18.0)
                onMarkerClick(marker.title, data.details, data.objectId, false)
                true
            }
            points.add(data.position)
            rtvClusterer?.add(marker)
        }

        srbData.forEach { data ->
            val marker = Marker(mapView)
            marker.position = data.position
            marker.title = data.title
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = srbMarkerIcon
            marker.setOnMarkerClickListener { _, _ ->
                mapView.controller.animateTo(marker.position)
                mapView.controller.setZoom(18.0)
                onMarkerClick(marker.title, data.details, data.objectId, true)
                true
            }
            points.add(data.position)
            srbClusterer?.add(marker)
        }

        cemData.forEach { data ->
            val marker = Marker(mapView)
            marker.position = data.position
            marker.title = data.title
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = cemMarkerIcon
            marker.setOnMarkerClickListener { _, _ ->
                mapView.controller.animateTo(marker.position)
                mapView.controller.setZoom(18.0)
                onMarkerClick(marker.title, data.details, data.objectId, false)
                true
            }
            points.add(data.position)
            cemClusterer?.add(marker)
        }

        rtvClusterer?.invalidate()
        srbClusterer?.invalidate()
        cemClusterer?.invalidate()
        mapView.invalidate()

        if (points.isNotEmpty() && !hasCenteredMap) {
            val boundingBox = BoundingBox.fromGeoPoints(points)
            mapView.zoomToBoundingBox(boundingBox, true, 100)
        } else if (points.isEmpty() && !hasCenteredMap) {
            mapView.controller.setCenter(GeoPoint(41.9028, 12.4964))
            mapView.controller.setZoom(6.0)
        }
    }
}

@Composable
private fun ImagePreviewDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, rotation ->
                    val oldScale = scale
                    val newScale = (scale * zoom).coerceIn(0.5f, 5f)
                    
                    // Calculate offset to keep the zoom centered on the gesture centroid
                    val newOffsetX = (offsetX + centroid.x) * (newScale / oldScale) - centroid.x + pan.x * newScale
                    val newOffsetY = (offsetY + centroid.y) * (newScale / oldScale) - centroid.y + pan.y * newScale
                    
                    scale = newScale
                    offsetX = newOffsetX
                    offsetY = newOffsetY
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    if (scale > 1f) {
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Close button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                    shape = MaterialTheme.shapes.small
                )
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }

        // Image with zoom and pan
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Preview grafico CEM",
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                ),
            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
            loading = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
            },
            error = {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Error",
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Errore caricamento immagine",
                            style = MaterialTheme.typography.bodyMedium,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
        )
    }
}

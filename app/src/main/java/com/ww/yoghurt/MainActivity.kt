package com.ww.yoghurt

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// Global Version Control
const val APP_VERSION = "1.2.0"

val appChangelog = mapOf(
    "v1.2.0" to listOf("Rewrote edit engine to eliminate ClassCastException crashes.", "Added Multi-Variate Trend Chart mapping Acidity, Density, Texture, and Overall.", "Batch names are now editable.", "Version Code bumped to 4."),
    "v1.1.2" to listOf("Restored proper architectural formatting."),
    "v1.1.1" to listOf("Fixed Radar Chart label placement.", "Resolved Edit Save ClassCastException crash.", "Fixed GitHub 302 Redirect APK parsing error.", "Added active API version checking.", "Moved Changelog to overlay dialog."),
    "v1.1.0" to listOf("Updated Gemini Model to v3 Flash.", "Added Historical Auto-Complete.", "Added Sensory Radar Charts & Trendlines.", "Added Batch Edit & Fork features."),
    "v1.0.0" to listOf("Initial Release: Core Engine.", "Dynamic NoSQL Schema Builder.", "AI Diagnostics integration.", "Data mapping & CSV Export.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val GreekBlue = Color(0xFF1E40AF)
            val LightBlueBg = Color(0xFFE0F2FE)
            val CreamWhite = Color(0xFFFDFBF7)
            val DarkBlueText = Color(0xFF1E3A8A)
            val CardOuterTint = Color(0xFFBAE6FD)

            val lightScheme = lightColorScheme(
                primary = GreekBlue,
                background = LightBlueBg,
                surface = CreamWhite,
                surfaceVariant = CardOuterTint,
                onPrimary = Color.White,
                onBackground = DarkBlueText,
                onSurface = DarkBlueText,
                onSurfaceVariant = Color(0xFF0F172A)
            )

            val darkScheme = darkColorScheme(
                primary = Color(0xFF60A5FA),
                background = Color(0xFF0F172A),
                surface = Color(0xFF1E293B),
                surfaceVariant = Color(0xFF334155),
                onPrimary = Color(0xFF0F172A),
                onBackground = Color(0xFFF8FAFC),
                onSurface = Color(0xFFF8FAFC),
                onSurfaceVariant = Color(0xFFCBD5E1)
            )

            val WwColorScheme = if (isSystemInDarkTheme()) darkScheme else lightScheme

            val GoogleSans = FontFamily(Font(R.font.googlesansflex))
            val defaultTypography = Typography()
            val WwTypography = Typography(
                displayLarge = defaultTypography.displayLarge.copy(fontFamily = GoogleSans),
                headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = GoogleSans),
                headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = GoogleSans),
                titleLarge = defaultTypography.titleLarge.copy(fontFamily = GoogleSans),
                titleMedium = defaultTypography.titleMedium.copy(fontFamily = GoogleSans),
                titleSmall = defaultTypography.titleSmall.copy(fontFamily = GoogleSans),
                bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = GoogleSans),
                bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = GoogleSans),
                bodySmall = defaultTypography.bodySmall.copy(fontFamily = GoogleSans),
                labelSmall = defaultTypography.labelSmall.copy(fontFamily = GoogleSans)
            )

            MaterialTheme(colorScheme = WwColorScheme, typography = WwTypography) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen()
                }
            }
        }
    }
}

val GeminiIcon: ImageVector
    get() = ImageVector.Builder(name = "Gemini", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 0f)
            curveTo(12f, 0f, 14.5f, 9.5f, 24f, 12f)
            curveTo(24f, 12f, 14.5f, 14.5f, 12f, 24f)
            curveTo(12f, 24f, 9.5f, 14.5f, 0f, 12f)
            curveTo(0f, 12f, 9.5f, 9.5f, 12f, 0f)
            close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(20.5f, 0f)
            curveTo(20.5f, 0f, 21.5f, 3.5f, 24f, 4.5f)
            curveTo(24f, 4.5f, 21.5f, 5.5f, 20.5f, 9f)
            curveTo(20.5f, 9f, 19.5f, 5.5f, 17f, 4.5f)
            curveTo(17f, 4.5f, 19.5f, 3.5f, 20.5f, 0f)
            close()
        }
    }.build()

val ForkIcon: ImageVector
    get() = ImageVector.Builder(name = "Fork", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(16f, 3f); lineTo(16f, 7f); lineTo(18f, 7f); lineTo(18f, 10f)
            curveTo(18f, 11.66f, 16.66f, 13f, 15f, 13f); lineTo(13f, 13f); lineTo(13f, 21f)
            lineTo(11f, 21f); lineTo(11f, 13f); lineTo(9f, 13f)
            curveTo(7.34f, 13f, 6f, 11.66f, 6f, 10f); lineTo(6f, 7f); lineTo(8f, 7f)
            lineTo(8f, 3f); lineTo(10f, 3f); lineTo(10f, 7f); lineTo(14f, 7f)
            lineTo(14f, 3f); lineTo(16f, 3f); close()
        }
    }.build()

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    val tabs = listOf(
        TabItem("Dashboard", Icons.Default.Menu),
        TabItem("Brew", Icons.Default.AddCircle),
        TabItem("Harvest", Icons.Default.CheckCircle),
        TabItem("Analysis", GeminiIcon),
        TabItem("System", Icons.Default.Settings)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val db = Firebase.firestore
    val view = LocalView.current

    var brewSchema by remember { mutableStateOf(defaultBrewSchema) }
    var harvestSchema by remember { mutableStateOf(defaultHarvestSchema) }
    var schemaLoaded by remember { mutableStateOf(false) }

    var masterBatches by remember { mutableStateOf<List<YogurtBatch>>(emptyList()) }
    var batchesLoading by remember { mutableStateOf(true) }

    var forkedBatchName by remember { mutableStateOf("") }
    val forkedValues = remember { mutableStateMapOf<String, String>() }

    val historicalData = remember(masterBatches) {
        val map = mutableMapOf<String, MutableSet<String>>()
        masterBatches.forEach { batch ->
            batch.rawData.forEach { (key, value) ->
                if (value is String && value.isNotBlank()) {
                    map.getOrPut(key) { mutableSetOf() }.add(value)
                }
            }
        }
        map
    }

    LaunchedEffect(Unit) {
        db.collection("config").document("schema").addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                val bFields = snapshot.get("brewFields") as? List<Map<String, String>>
                val hFields = snapshot.get("harvestFields") as? List<Map<String, String>>
                if (bFields != null) brewSchema = bFields.map { CustomField(it["name"] ?: "", it["type"] ?: "Text") }
                if (hFields != null) harvestSchema = hFields.map { CustomField(it["name"] ?: "", it["type"] ?: "Text") }
            }
            schemaLoaded = true
        }
    }

    DisposableEffect(Unit) {
        val listener = db.collection("brews").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    batchesLoading = false
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    masterBatches = snapshot.documents.map { doc ->
                        val data = doc.data ?: emptyMap()
                        val name = doc.getString("batchName") ?: "Unknown Batch"
                        val timestamp = doc.getLong("timestamp") ?: 0L
                        val date = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault()).format(Date(timestamp))
                        val status = doc.getString("status") ?: "unknown"
                        YogurtBatch(doc.id, name, date, status, data)
                    }
                    batchesLoading = false
                }
            }
        onDispose { listener.remove() }
    }

    if (!schemaLoaded) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.primary,
            edgePadding = 8.dp
        ) {
            tabs.forEachIndexed { index, tabItem ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    icon = { Icon(tabItem.icon, contentDescription = tabItem.title) }
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                when (page) {
                    0 -> DashboardScreen(
                        batches = masterBatches,
                        isLoading = batchesLoading,
                        onForkBatch = { batch ->
                            forkedBatchName = "Copy of ${batch.batchName}"
                            forkedValues.clear()
                            brewSchema.forEach { field ->
                                batch.rawData[field.name]?.let { forkedValues[field.name] = it.toString() }
                            }
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                    1 -> YogurtBrewScreen(brewSchema, historicalData, forkedBatchName, forkedValues) {
                        forkedBatchName = ""
                        forkedValues.clear()
                    }
                    2 -> HarvestScreen(harvestSchema, historicalData)
                    3 -> AnalysisScreen(masterBatches)
                    4 -> SystemSettingsScreen(brewSchema, harvestSchema, coroutineScope)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    options: Set<String>,
    isNumber: Boolean,
    isLargeText: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredOptions = options.filter { it.contains(value, ignoreCase = true) && it != value }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredOptions.isNotEmpty(),
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            label = { Text(label, fontWeight = FontWeight.Normal) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(if (isLargeText) 120.dp else 60.dp),
            keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number)
            else KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = if (isLargeText) 5 else 1,
            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal)
        )
        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, fontWeight = FontWeight.Normal) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SensoryRadarChart(rawData: Map<String, Any>) {
    val metrics = listOf("Acidity", "Density", "Texture", "Overall")
    val scores = metrics.map { key ->
        val actualKey = rawData.keys.firstOrNull { it.contains(key, ignoreCase = true) } ?: key
        (rawData[actualKey] as? Number)?.toFloat() ?: 0f
    }

    if (scores.count { it > 0f } < 3) return

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
    ) {
        Text("Sensory Profile", color = primaryColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2
                val maxScore = 7f

                for (step in 1..7 step 2) {
                    val stepRadius = radius * (step / maxScore)
                    val webPath = Path()
                    for (i in 0 until 4) {
                        val angle = Math.toRadians((i * 90 - 90).toDouble())
                        val x = center.x + stepRadius * cos(angle).toFloat()
                        val y = center.y + stepRadius * sin(angle).toFloat()
                        if (i == 0) webPath.moveTo(x, y) else webPath.lineTo(x, y)
                    }
                    webPath.close()
                    drawPath(path = webPath, color = surfaceVariant.copy(alpha = 0.2f), style = Stroke(width = 1.dp.toPx()))
                }

                for (i in 0 until 4) {
                    val angle = Math.toRadians((i * 90 - 90).toDouble())
                    val x = center.x + radius * cos(angle).toFloat()
                    val y = center.y + radius * sin(angle).toFloat()
                    drawLine(color = surfaceVariant.copy(alpha = 0.2f), start = center, end = Offset(x, y), strokeWidth = 1.dp.toPx())
                }

                val dataPath = Path()
                scores.forEachIndexed { i, score ->
                    val angle = Math.toRadians((i * 90 - 90).toDouble())
                    val scoreRadius = radius * (score / maxScore)
                    val x = center.x + scoreRadius * cos(angle).toFloat()
                    val y = center.y + scoreRadius * sin(angle).toFloat()
                    if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
                    drawCircle(color = primaryColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
                dataPath.close()
                drawPath(path = dataPath, color = primaryColor.copy(alpha = 0.4f))
                drawPath(path = dataPath, color = primaryColor, style = Stroke(width = 2.dp.toPx()))
            }

            Text("Acidity", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = surfaceVariant, modifier = Modifier.align(Alignment.TopCenter))
            Text("Density", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = surfaceVariant, modifier = Modifier.align(Alignment.CenterEnd))
            Text("Texture", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = surfaceVariant, modifier = Modifier.align(Alignment.BottomCenter))
            Text("Overall", fontSize = MaterialTheme.typography.labelMedium.fontSize, fontWeight = FontWeight.Bold, color = surfaceVariant, modifier = Modifier.align(Alignment.CenterStart))
        }
    }
}

// Multi-Variate Trend Chart mapping all sensory metrics
@Composable
fun MultiTrendChart(batches: List<YogurtBatch>) {
    val validBatches = batches.filter { it.status == "completed" }.reversed()
    if (validBatches.size < 2) return

    val metrics = listOf(
        Pair("Overall", MaterialTheme.colorScheme.primary),
        Pair("Acidity", Color(0xFFEF4444)), // Red
        Pair("Density", Color(0xFF10B981)), // Green
        Pair("Texture", Color(0xFFF59E0B))  // Amber
    )

    Card(
        modifier = Modifier.fillMaxWidth().height(260.dp).padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sensory Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))

            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val canvasWidth = size.width; val canvasHeight = size.height
                val maxScore = 7f; val minScore = 1f

                // Background grid lines
                val gridSteps = 3
                for (i in 0..gridSteps) {
                    val y = canvasHeight * (i.toFloat() / gridSteps)
                    drawLine(color = Color.Gray.copy(alpha = 0.2f), start = Offset(0f, y), end = Offset(canvasWidth, y), strokeWidth = 1.dp.toPx())
                }

                val xStep = canvasWidth / (validBatches.size - 1).coerceAtLeast(1)

                // Plot each metric line
                metrics.forEach { (metricName, color) ->
                    val path = Path()
                    var hasStarted = false

                    validBatches.forEachIndexed { index, batch ->
                        val key = batch.rawData.keys.firstOrNull { it.contains(metricName, ignoreCase = true) } ?: metricName
                        val score = (batch.rawData[key] as? Number)?.toFloat()

                        if (score != null) {
                            val x = index * xStep
                            val normalizedY = 1f - ((score - minScore) / (maxScore - minScore))
                            val y = normalizedY * canvasHeight

                            if (!hasStarted) {
                                path.moveTo(x, y)
                                hasStarted = true
                            } else {
                                path.lineTo(x, y)
                            }
                            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
                        }
                    }
                    if (hasStarted) {
                        drawPath(path = path, color = color.copy(alpha = 0.8f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                    }
                }
            }

            // Legend
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                metrics.forEach { (name, color) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(color))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(name, fontSize = MaterialTheme.typography.labelSmall.fontSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(batches: List<YogurtBatch>, isLoading: Boolean, onForkBatch: (YogurtBatch) -> Unit) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val view = LocalView.current

    var selectedBatchForDetails by remember { mutableStateOf<YogurtBatch?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    // Explicitly track the batch name for editing
    var editBatchName by remember { mutableStateOf("") }
    val editValues = remember { mutableStateMapOf<String, String>() }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ww_logo),
                    contentDescription = "WW Yoghurt Logo",
                    modifier = Modifier.size(144.dp).clip(RoundedCornerShape(16.dp))
                )
            }
            FeedbackIconButton(
                onClick = { exportDataToCsv(context, db) },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Icon(Icons.Default.Share, contentDescription = "Export to CSV", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
        } else if (batches.isEmpty()) {
            Text(
                "No batches found. Swipe right to start brewing!",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp)
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    MultiTrendChart(batches)
                }

                itemsIndexed(batches) { index, batch ->
                    val chronologicalNumber = batches.size - index
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                selectedBatchForDetails = batch
                            },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "#$chronologicalNumber",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(36.dp)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(batch.batchName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text(batch.dateStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (batch.status == "completed") {
                                val ratingKey = batch.rawData.keys.firstOrNull { it.contains("Rating", ignoreCase = true) || it.contains("overall", ignoreCase = true) } ?: "Overall Rating"
                                val rating = (batch.rawData[ratingKey] as? Number)?.toInt()
                                if (rating != null) {
                                    val scoreColor = if (rating >= 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    Text("$rating/7", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, color = scoreColor)
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Done")
                                }
                            } else {
                                Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                    Text("Brewing", color = Color.White, fontWeight = FontWeight.Normal, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Made with ❤️ for Woody",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    selectedBatchForDetails?.let { batch ->
        AlertDialog(
            onDismissRequest = {
                selectedBatchForDetails = null
                isEditing = false
            },
            title = {
                Text(if (isEditing) "Edit Batch" else batch.batchName, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (!isEditing) {
                        Text("Date: ${batch.dateStr}", fontWeight = FontWeight.Normal)
                        if (batch.status == "completed") {
                            SensoryRadarChart(batch.rawData)
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Data Log", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        val ignoreKeys = listOf("batchName", "timestamp", "status")
                        batch.rawData.forEach { (key, value) ->
                            if (!ignoreKeys.contains(key)) {
                                Text("$key: $value", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal)
                            }
                        }
                    } else {
                        // Editable Batch Name
                        OutlinedTextField(
                            value = editBatchName,
                            onValueChange = { editBatchName = it },
                            label = { Text("Batch Name", fontWeight = FontWeight.Normal) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        val ignoreKeys = listOf("batchName", "timestamp", "status")
                        batch.rawData.forEach { (key, value) ->
                            if (!ignoreKeys.contains(key)) {
                                OutlinedTextField(
                                    value = editValues[key] ?: "",
                                    onValueChange = { editValues[key] = it },
                                    label = { Text(key, fontWeight = FontWeight.Normal) },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    keyboardOptions = if (value is Number) KeyboardOptions(keyboardType = KeyboardType.Number)
                                    else KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isEditing) {
                    FeedbackButton(
                        onClick = {
                            val mapToUpdate = mutableMapOf<String, Any>()
                            mapToUpdate["batchName"] = editBatchName

                            // Safely cast based ONLY on original data type to prevent crash
                            editValues.forEach { (k, v) ->
                                val originalVal = batch.rawData[k]
                                if (originalVal is Number) {
                                    mapToUpdate[k] = v.toFloatOrNull() ?: 0f
                                } else {
                                    mapToUpdate[k] = v
                                }
                            }

                            db.collection("brews").document(batch.id).update(mapToUpdate).addOnSuccessListener {
                                Toast.makeText(context, "Batch Updated", Toast.LENGTH_SHORT).show()
                                selectedBatchForDetails = null
                                isEditing = false
                            }
                        }
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                } else {
                    FeedbackButton(
                        onClick = { selectedBatchForDetails = null }
                    ) {
                        Text("Close", fontWeight = FontWeight.Normal)
                    }
                }
            },
            dismissButton = {
                Row {
                    if (!isEditing) {
                        FeedbackIconButton(
                            onClick = {
                                editBatchName = batch.batchName
                                editValues.clear()
                                val ignoreKeys = listOf("batchName", "timestamp", "status")
                                batch.rawData.forEach { (k, v) ->
                                    if (k !in ignoreKeys) editValues[k] = v.toString()
                                }
                                isEditing = true
                            }
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                        }

                        FeedbackIconButton(
                            onClick = {
                                onForkBatch(batch)
                                selectedBatchForDetails = null
                            }
                        ) {
                            Icon(ForkIcon, contentDescription = "Fork Batch", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        FeedbackIconButton(
                            onClick = { isEditing = false }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    FeedbackIconButton(
                        onClick = { showDeleteConfirm = true }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }

    if (showDeleteConfirm && selectedBatchForDetails != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Batch?", fontWeight = FontWeight.Bold) },
            text = { Text("This action cannot be undone.", fontWeight = FontWeight.Normal) },
            confirmButton = {
                FeedbackButton(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        db.collection("brews").document(selectedBatchForDetails!!.id).delete()
                        showDeleteConfirm = false
                        selectedBatchForDetails = null
                    }
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", fontWeight = FontWeight.Normal)
                }
            }
        )
    }
}

fun exportDataToCsv(context: Context, db: com.google.firebase.firestore.FirebaseFirestore) {
    Toast.makeText(context, "Compiling CSV Database...", Toast.LENGTH_SHORT).show()
    db.collection("brews").orderBy("timestamp", Query.Direction.ASCENDING).get()
        .addOnSuccessListener { snapshot ->
            if (snapshot.isEmpty) {
                Toast.makeText(context, "No data to export.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }
            try {
                val docs = snapshot.documents
                val dynamicKeys = mutableSetOf<String>()
                val ignoreKeys = setOf("batchName", "timestamp", "status")

                docs.forEach { doc ->
                    doc.data?.keys?.filter { it !in ignoreKeys }?.let { dynamicKeys.addAll(it) }
                }

                val sortedKeys = dynamicKeys.sorted()
                val csv = StringBuilder().append("Date,Batch Name,Status")
                sortedKeys.forEach { csv.append(",\"$it\"") }
                csv.append("\n")

                docs.forEach { doc ->
                    val data = doc.data ?: emptyMap()
                    val name = (doc.getString("batchName") ?: "Unknown").replace("\"", "\"\"")
                    val ts = doc.getLong("timestamp") ?: 0L
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(ts))
                    val status = doc.getString("status") ?: "unknown"

                    csv.append("\"$dateStr\",\"$name\",\"$status\"")
                    sortedKeys.forEach { key ->
                        val value = data[key]?.toString() ?: ""
                        csv.append(",\"${value.replace("\"", "\"\"")}\"")
                    }
                    csv.append("\n")
                }

                val exportFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "WW_Yoghurt_Data.csv")
                java.io.FileWriter(exportFile).use { it.write(csv.toString()) }

                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", exportFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Export to Google Drive"))
            } catch (e: Exception) {
                Toast.makeText(context, "Export Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to fetch Database.", Toast.LENGTH_SHORT).show()
        }
}

@Composable
fun YogurtBrewScreen(
    schema: List<CustomField>,
    historicalData: Map<String, Set<String>>,
    forkedBatchName: String,
    forkedValues: Map<String, String>,
    onClearFork: () -> Unit
) {
    var batchName by remember { mutableStateOf("") }
    val dynamicTextValues = remember { mutableStateMapOf<String, String>() }
    var isSaving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val db = Firebase.firestore
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(forkedBatchName) {
        if (forkedBatchName.isNotBlank()) {
            batchName = forkedBatchName
            dynamicTextValues.clear()
            forkedValues.forEach { (k, v) -> dynamicTextValues[k] = v }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Phase 1: The Brew", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Log ingredients and environment.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = batchName,
                    onValueChange = { batchName = it; onClearFork() },
                    label = { Text("Batch Name (e.g., Batch #1)", fontWeight = FontWeight.Normal) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal),
                    enabled = !isSaving
                )
                Spacer(modifier = Modifier.height(8.dp))

                schema.forEach { field ->
                    AutoCompleteTextField(
                        value = dynamicTextValues[field.name] ?: "",
                        onValueChange = { dynamicTextValues[field.name] = it; onClearFork() },
                        label = field.name,
                        options = historicalData[field.name] ?: emptySet(),
                        isNumber = field.type == "Number"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Spacer(modifier = Modifier.height(24.dp))

                FeedbackButton(
                    onClick = {
                        if (batchName.isNotBlank()) {
                            isSaving = true
                            coroutineScope.launch {
                                var weatherData = "Unknown"
                                try {
                                    val response = withContext(Dispatchers.IO) {
                                        URL("https://api.open-meteo.com/v1/forecast?latitude=37.98&longitude=23.72&current_weather=true").readText()
                                    }
                                    val temp = JSONObject(response).getJSONObject("current_weather").getDouble("temperature")
                                    weatherData = "$temp °C"
                                } catch (e: Exception) {}

                                val brewData = hashMapOf<String, Any>(
                                    "batchName" to batchName,
                                    "timestamp" to System.currentTimeMillis(),
                                    "status" to "brewing",
                                    "Ambient Temp (Athens)" to weatherData
                                )
                                schema.forEach { field ->
                                    val stringVal = dynamicTextValues[field.name] ?: ""
                                    if (field.type == "Number") {
                                        brewData[field.name] = stringVal.toFloatOrNull() ?: 0f
                                    } else {
                                        brewData[field.name] = stringVal
                                    }
                                }
                                db.collection("brews").add(brewData).addOnSuccessListener {
                                    Toast.makeText(context, "Brew saved!", Toast.LENGTH_SHORT).show()
                                    batchName = ""
                                    dynamicTextValues.clear()
                                    onClearFork()
                                    isSaving = false
                                }.addOnFailureListener { e ->
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    isSaving = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "Batch Name is required.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Brew Phase", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HarvestScreen(schema: List<CustomField>, historicalData: Map<String, Set<String>>) {
    val dynamicSliderValues = remember { mutableStateMapOf<String, Float>() }
    val dynamicTextValues = remember { mutableStateMapOf<String, String>() }
    var pendingBrews by remember { mutableStateOf<List<PendingBrew>>(emptyList()) }
    var selectedBrew by remember { mutableStateOf<PendingBrew?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val db = Firebase.firestore
    val view = LocalView.current

    LaunchedEffect(Unit) {
        db.collection("brews").whereEqualTo("status", "brewing").get().addOnSuccessListener { snapshot ->
            val brews = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("batchName") ?: "Unknown"
                val ts = doc.getLong("timestamp") ?: 0L
                PendingBrew(doc.id, "$name (${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(ts))})")
            }
            pendingBrews = brews
            if (brews.isNotEmpty()) selectedBrew = brews.first()
            isLoading = false
        }.addOnFailureListener { isLoading = false }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Phase 2: The Harvest", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Tasting and evaluation.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (pendingBrews.isEmpty()) {
                    Text("No active brews waiting.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Normal)
                } else {
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedBrew?.displayLabel ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Target Batch", fontWeight = FontWeight.Normal) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                    view.playSoundEffect(SoundEffectConstants.CLICK)
                                    dropdownExpanded = true
                                },
                            enabled = false,
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal),
                            colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            pendingBrews.forEach { brew ->
                                DropdownMenuItem(
                                    text = { Text(brew.displayLabel, fontWeight = FontWeight.Normal) },
                                    onClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                        view.playSoundEffect(SoundEffectConstants.CLICK)
                                        selectedBrew = brew
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                schema.forEach { field ->
                    if (field.type == "Slider") {
                        val sliderVal = dynamicSliderValues[field.name] ?: 4f
                        Text("${field.name}: ${sliderVal.toInt()}", fontWeight = FontWeight.Bold)
                        Slider(
                            value = sliderVal,
                            onValueChange = { dynamicSliderValues[field.name] = it },
                            valueRange = 1f..7f,
                            steps = 5
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        AutoCompleteTextField(
                            value = dynamicTextValues[field.name] ?: "",
                            onValueChange = { dynamicTextValues[field.name] = it },
                            label = field.name,
                            options = historicalData[field.name] ?: emptySet(),
                            isNumber = field.type == "Number",
                            isLargeText = field.name == "Notes"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))

                FeedbackButton(
                    onClick = {
                        selectedBrew?.let { brew ->
                            isSaving = true
                            val data = mutableMapOf<String, Any>("status" to "completed")
                            schema.forEach { field ->
                                if (field.type == "Slider") {
                                    data[field.name] = (dynamicSliderValues[field.name] ?: 4f).toInt()
                                } else if (field.type == "Number") {
                                    data[field.name] = (dynamicTextValues[field.name] ?: "").toFloatOrNull() ?: 0f
                                } else {
                                    data[field.name] = dynamicTextValues[field.name] ?: ""
                                }
                            }
                            db.collection("brews").document(brew.id).update(data).addOnSuccessListener {
                                Toast.makeText(context, "Harvest saved!", Toast.LENGTH_SHORT).show()
                                dynamicSliderValues.clear()
                                dynamicTextValues.clear()
                                pendingBrews = pendingBrews.filter { it.id != brew.id }
                                selectedBrew = pendingBrews.firstOrNull()
                                isSaving = false
                            }.addOnFailureListener { isSaving = false }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving && selectedBrew != null
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Harvest", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AnalysisScreen(batches: List<YogurtBatch>) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val sharedPrefs = context.getSharedPreferences("WWPrefs", Context.MODE_PRIVATE)
    val apiKey = sharedPrefs.getString("GEMINI_API_KEY", "") ?: ""

    var prompt by remember { mutableStateOf("") }
    var savedChats by remember { mutableStateOf<List<SavedChat>>(emptyList()) }
    var expandedChatIds by remember { mutableStateOf(setOf<String>()) }
    var isThinking by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val listener = db.collection("chats").orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    savedChats = snapshot.documents.map { doc ->
                        SavedChat(
                            id = doc.id,
                            prompt = doc.getString("prompt") ?: "",
                            response = doc.getString("response") ?: "",
                            timestamp = doc.getLong("timestamp") ?: 0L
                        )
                    }
                }
            }
        onDispose { listener.remove() }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding().padding(16.dp)) {
        Text("AI Diagnostic Engine", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Chat with your data.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        if (apiKey.isBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text("API Key Missing. Please configure it in the System tab.", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), reverseLayout = false) {
                items(savedChats) { chat ->
                    val isExpanded = expandedChatIds.contains(chat.id)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            expandedChatIds = if (isExpanded) expandedChatIds - chat.id else expandedChatIds + chat.id
                        },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(GeminiIcon, contentDescription = "AI", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(chat.prompt, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            if (isExpanded) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(chat.response, fontWeight = FontWeight.Normal, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }

                if (isThinking) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(24.dp).align(Alignment.CenterHorizontally), strokeWidth = 2.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("E.g., Why was my last batch so tart?", fontWeight = FontWeight.Normal) },
                    modifier = Modifier.weight(1f),
                    enabled = !isThinking,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FeedbackIconButton(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            val userText = prompt
                            prompt = ""
                            isThinking = true

                            coroutineScope.launch {
                                try {
                                    val contextData = StringBuilder("You are WW Yoghurt AI, an expert in fermentation. Here is the user's historical batch data (in JSON/Map format):\n\n")
                                    batches.forEach { batch -> contextData.append("Batch: ${batch.batchName}, Status: ${batch.status}, Data: ${batch.rawData}\n") }
                                    contextData.append("\nBased strictly on the data above and your expertise, answer the user's prompt: $userText. Do not use markdown bolding in your response.")

                                    val generativeModel = GenerativeModel(modelName = "gemini-3-flash-preview", apiKey = apiKey)
                                    val response = generativeModel.generateContent(contextData.toString())

                                    val chatDoc = hashMapOf(
                                        "prompt" to userText,
                                        "response" to (response.text?.replace("**", "") ?: "No response generated."),
                                        "timestamp" to System.currentTimeMillis()
                                    )
                                    db.collection("chats").add(chatDoc)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "AI Error: ${e.message}", Toast.LENGTH_LONG).show()
                                } finally { isThinking = false }
                            }
                        }
                    },
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp)),
                    enabled = !isThinking && prompt.isNotBlank()
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun SystemSettingsScreen(currentBrewSchema: List<CustomField>, currentHarvestSchema: List<CustomField>, coroutineScope: CoroutineScope) {
    val context = LocalContext.current
    val db = Firebase.firestore
    val sharedPrefs = context.getSharedPreferences("WWPrefs", Context.MODE_PRIVATE)

    var apiKeyInput by remember { mutableStateOf(sharedPrefs.getString("GEMINI_API_KEY", "") ?: "") }
    var isSaving by remember { mutableStateOf(false) }

    val editBrewSchema = remember(currentBrewSchema) { mutableStateListOf(*currentBrewSchema.toTypedArray()) }
    val editHarvestSchema = remember(currentHarvestSchema) { mutableStateListOf(*currentHarvestSchema.toTypedArray()) }
    var showAddFieldDialog by remember { mutableStateOf(false) }
    var showChangelogDialog by remember { mutableStateOf(false) }
    var fieldToAddPhase by remember { mutableStateOf("Brew") }
    var newFieldName by remember { mutableStateOf("") }
    var newFieldType by remember { mutableStateOf("Text") }

    Column(
        modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("System Engine", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Modify global variables & integrations.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI Integration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Enter your Gemini API Key. This is stored securely on this device.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("Gemini API Key", fontWeight = FontWeight.Normal) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                FeedbackButton(
                    onClick = {
                        sharedPrefs.edit().putString("GEMINI_API_KEY", apiKeyInput).apply()
                        Toast.makeText(context, "API Key Saved Locally", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save API Key", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Schema Editor", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Brew Phase Fields", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                editBrewSchema.forEachIndexed { index, field ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${field.name} (${field.type})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal, modifier = Modifier.weight(1f))
                        Row {
                            FeedbackIconButton(onClick = { if (index > 0) { val temp = editBrewSchema[index]; editBrewSchema[index] = editBrewSchema[index - 1]; editBrewSchema[index - 1] = temp } }) { Icon(Icons.Default.KeyboardArrowUp, "Up", tint = MaterialTheme.colorScheme.primary) }
                            FeedbackIconButton(onClick = { if (index < editBrewSchema.size - 1) { val temp = editBrewSchema[index]; editBrewSchema[index] = editBrewSchema[index + 1]; editBrewSchema[index + 1] = temp } }) { Icon(Icons.Default.KeyboardArrowDown, "Down", tint = MaterialTheme.colorScheme.primary) }
                            FeedbackIconButton(onClick = { editBrewSchema.remove(field) }) { Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
                TextButton(onClick = { fieldToAddPhase = "Brew"; showAddFieldDialog = true }) { Text("+ Add Brew Field", fontWeight = FontWeight.Bold) }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Text("Harvest Phase Fields", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                editHarvestSchema.forEachIndexed { index, field ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${field.name} (${field.type})", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal, modifier = Modifier.weight(1f))
                        Row {
                            FeedbackIconButton(onClick = { if (index > 0) { val temp = editHarvestSchema[index]; editHarvestSchema[index] = editHarvestSchema[index - 1]; editHarvestSchema[index - 1] = temp } }) { Icon(Icons.Default.KeyboardArrowUp, "Up", tint = MaterialTheme.colorScheme.primary) }
                            FeedbackIconButton(onClick = { if (index < editHarvestSchema.size - 1) { val temp = editHarvestSchema[index]; editHarvestSchema[index] = editHarvestSchema[index + 1]; editHarvestSchema[index + 1] = temp } }) { Icon(Icons.Default.KeyboardArrowDown, "Down", tint = MaterialTheme.colorScheme.primary) }
                            FeedbackIconButton(onClick = { editHarvestSchema.remove(field) }) { Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
                TextButton(onClick = { fieldToAddPhase = "Harvest"; showAddFieldDialog = true }) { Text("+ Add Harvest Field", fontWeight = FontWeight.Bold) }

                Spacer(modifier = Modifier.height(16.dp))

                FeedbackButton(
                    onClick = {
                        isSaving = true
                        val configData = hashMapOf("brewFields" to editBrewSchema.map { mapOf("name" to it.name, "type" to it.type) }, "harvestFields" to editHarvestSchema.map { mapOf("name" to it.name, "type" to it.type) })
                        db.collection("config").document("schema").set(configData).addOnSuccessListener {
                            Toast.makeText(context, "Schema Updated Globally", Toast.LENGTH_SHORT).show()
                            isSaving = false
                        }.addOnFailureListener { isSaving = false }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save & Apply Schema", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { showChangelogDialog = true },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Text("View Full Changelog", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { checkForUpdates(context, coroutineScope) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Download Latest Update", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("Version v$APP_VERSION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false },
            title = { Text("App Changelog", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    appChangelog.forEach { (version, changes) ->
                        item {
                            Text(version, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                            changes.forEach { change ->
                                Text("• $change", fontWeight = FontWeight.Normal, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp))
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showChangelogDialog = false }) {
                    Text("Close", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showAddFieldDialog) {
        AlertDialog(
            onDismissRequest = { showAddFieldDialog = false },
            title = { Text("Add Field to $fieldToAddPhase", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFieldName,
                        onValueChange = { newFieldName = it },
                        label = { Text("Parameter Name", fontWeight = FontWeight.Normal) },
                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Type:", fontWeight = FontWeight.Bold)
                    Row {
                        listOf("Text", "Number", "Slider").forEach { type ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = newFieldType == type, onClick = { newFieldType = type })
                                Text(type, modifier = Modifier.padding(end = 8.dp), fontWeight = FontWeight.Normal)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                FeedbackButton(onClick = {
                    if (newFieldName.isNotBlank()) {
                        val newField = CustomField(newFieldName, newFieldType)
                        if (fieldToAddPhase == "Brew") {
                            editBrewSchema.add(newField)
                        } else {
                            editHarvestSchema.add(newField)
                        }
                        newFieldName = ""
                        showAddFieldDialog = false
                    }
                }) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFieldDialog = false }) {
                    Text("Cancel", fontWeight = FontWeight.Normal)
                }
            }
        )
    }
}

fun checkForUpdates(context: Context, coroutineScope: CoroutineScope) {
    Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()

    coroutineScope.launch {
        try {
            val response = withContext(Dispatchers.IO) {
                URL("https://api.github.com/repos/exolon/ww-yoghurt/releases/latest").readText()
            }
            val json = JSONObject(response)
            val latestTag = json.getString("tag_name").replace("v", "")

            if (latestTag > APP_VERSION) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Downloading v$latestTag...", Toast.LENGTH_LONG).show()
                    val apkName = "app-release.apk"
                    val githubReleaseUrl = "https://github.com/exolon/ww-yoghurt/releases/latest/download/$apkName"
                    val request = DownloadManager.Request(Uri.parse(githubReleaseUrl))
                        .setTitle("WW Yoghurt Update")
                        .setDescription("Downloading v$latestTag...")
                        .setMimeType("application/vnd.android.package-archive")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkName)

                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), apkName)
                    if (file.exists()) file.delete()

                    val downloadId = downloadManager.enqueue(request)

                    val onComplete = object : BroadcastReceiver() {
                        override fun onReceive(ctxt: Context, intent: Intent) {
                            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                            if (id == downloadId) {
                                val downloadedFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), apkName)
                                val uri = FileProvider.getUriForFile(ctxt, "${ctxt.packageName}.provider", downloadedFile)
                                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                    setDataAndType(uri, "application/vnd.android.package-archive")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                }
                                ctxt.startActivity(installIntent)
                                ctxt.unregisterReceiver(this)
                            }
                        }
                    }
                    val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        context.registerReceiver(onComplete, filter, Context.RECEIVER_EXPORTED)
                    } else {
                        context.registerReceiver(onComplete, filter)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "You are already on the latest version (v$APP_VERSION).", Toast.LENGTH_LONG).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to check for updates: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
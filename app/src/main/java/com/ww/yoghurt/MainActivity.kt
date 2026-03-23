package com.ww.yoghurt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()

        setContent {
            val GreekBlue = Color(0xFF1E40AF)
            val LightBlueBg = Color(0xFFE0F2FE)
            val CreamWhite = Color(0xFFFDFBF7)
            val DarkBlueText = Color(0xFF1E3A8A)
            val CardOuterTint = Color(0xFFBAE6FD)

            val lightScheme = lightColorScheme(
                primary = GreekBlue, background = LightBlueBg, surface = CreamWhite,
                surfaceVariant = CardOuterTint, onPrimary = Color.White, onBackground = DarkBlueText,
                onSurface = DarkBlueText, onSurfaceVariant = Color(0xFF0F172A)
            )

            val darkScheme = darkColorScheme(
                primary = Color(0xFF60A5FA), background = Color(0xFF0F172A), surface = Color(0xFF1E293B),
                surfaceVariant = Color(0xFF334155), onPrimary = Color(0xFF0F172A), onBackground = Color(0xFFF8FAFC),
                onSurface = Color(0xFFF8FAFC), onSurfaceVariant = Color(0xFFCBD5E1)
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

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Yoghurt Fermentation"
            val descriptionText = "Alerts when a batch is ready to be moved to the fridge."
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("YOGHURT_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MainScreen() {
    val tabs = listOf(
        TabItem("Dashboard", Icons.Default.Menu), TabItem("Brew", Icons.Default.AddCircle),
        TabItem("Harvest", Icons.Default.CheckCircle), TabItem("Analysis", GeminiIcon),
        TabItem("System", Icons.Default.Settings)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()
    val view = LocalView.current

    var brewSchema by remember { mutableStateOf(defaultBrewSchema) }
    var harvestSchema by remember { mutableStateOf(defaultHarvestSchema) }
    var schemaLoaded by remember { mutableStateOf(false) }

    var masterBatches by remember { mutableStateOf<List<YogurtBatch>>(emptyList()) }
    var batchesLoading by remember { mutableStateOf(true) }

    var forkedBatchName by remember { mutableStateOf("") }
    var forkedFromId by remember { mutableStateOf<String?>(null) }
    val forkedValues = remember { mutableStateMapOf<String, String>() }

    val historicalData = remember(masterBatches) {
        val map = mutableMapOf<String, MutableSet<String>>()
        masterBatches.forEach { batch ->
            batch.rawData.forEach { (key, value) ->
                if (value is String && value.isNotBlank()) map.getOrPut(key) { mutableSetOf() }.add(value)
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
                if (e != null) { batchesLoading = false; return@addSnapshotListener }
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

    if (!schemaLoaded) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }; return }

    Column(modifier = Modifier.fillMaxSize().systemBarsPadding()) {
        // CHANGED: TabRow instead of ScrollableTabRow for evenly spaced icons
        TabRow(selectedTabIndex = pagerState.currentPage, containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.primary) {
            tabs.forEachIndexed { index, tabItem ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM); view.playSoundEffect(SoundEffectConstants.CLICK); coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    icon = { Icon(tabItem.icon, contentDescription = tabItem.title) }
                )
            }
        }

        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Card(modifier = Modifier.fillMaxSize().padding(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)), elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)) {
                when (page) {
                    0 -> DashboardScreen(
                        batches = masterBatches,
                        isLoading = batchesLoading,
                        brewSchema = brewSchema,
                        harvestSchema = harvestSchema,
                        onForkBatch = { batch ->
                            forkedBatchName = "Copy of ${batch.batchName}"
                            forkedFromId = batch.id
                            forkedValues.clear()
                            brewSchema.forEach { field -> batch.rawData[field.name]?.let { forkedValues[field.name] = it.toString() } }
                            coroutineScope.launch { pagerState.animateScrollToPage(1) }
                        }
                    )
                    1 -> YogurtBrewScreen(brewSchema, historicalData, forkedBatchName, forkedFromId, forkedValues) {
                        forkedBatchName = ""; forkedFromId = null; forkedValues.clear()
                    }
                    2 -> HarvestScreen(harvestSchema, historicalData)
                    3 -> AnalysisScreen(masterBatches)
                    4 -> SystemSettingsScreen(brewSchema, harvestSchema, coroutineScope)
                }
            }
        }
    }
}
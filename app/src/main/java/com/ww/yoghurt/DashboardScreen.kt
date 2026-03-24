package com.ww.yoghurt

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

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

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Text("Sensory Profile", color = primaryColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.width / 2

                for (step in 1..7 step 2) {
                    val stepRadius = radius * (step / 7f)
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
                    drawLine(color = surfaceVariant.copy(alpha = 0.2f), start = center, end = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat()), strokeWidth = 1.dp.toPx())
                }

                val dataPath = Path()
                scores.forEachIndexed { i, score ->
                    val angle = Math.toRadians((i * 90 - 90).toDouble())
                    val scoreRadius = radius * (score / 7f)
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

@Composable
fun MultiTrendChart(batches: List<YogurtBatch>) {
    val validBatches = batches.filter { it.status == "completed" }.reversed()
    if (validBatches.size < 2) return

    val metrics = listOf(Pair("Overall", MaterialTheme.colorScheme.primary), Pair("Acidity", Color(0xFFEF4444)), Pair("Density", Color(0xFF10B981)), Pair("Texture", Color(0xFFF59E0B)))

    Card(modifier = Modifier.fillMaxWidth().height(260.dp).padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sensory Trends", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val canvasWidth = size.width; val canvasHeight = size.height
                for (i in 0..3) {
                    val y = canvasHeight * (i.toFloat() / 3)
                    drawLine(color = Color.Gray.copy(alpha = 0.2f), start = Offset(0f, y), end = Offset(canvasWidth, y), strokeWidth = 1.dp.toPx())
                }
                val xStep = canvasWidth / (validBatches.size - 1).coerceAtLeast(1)
                metrics.forEach { (metricName, color) ->
                    val path = Path()
                    var hasStarted = false
                    validBatches.forEachIndexed { index, batch ->
                        val key = batch.rawData.keys.firstOrNull { it.contains(metricName, ignoreCase = true) } ?: metricName
                        val score = (batch.rawData[key] as? Number)?.toFloat()
                        if (score != null) {
                            val x = index * xStep
                            val y = (1f - ((score - 1f) / 6f)) * canvasHeight
                            if (!hasStarted) { path.moveTo(x, y); hasStarted = true } else path.lineTo(x, y)
                            drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
                        }
                    }
                    if (hasStarted) drawPath(path = path, color = color.copy(alpha = 0.8f), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
                }
            }
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
fun CorrelationScatterPlot(batches: List<YogurtBatch>, brewSchema: List<CustomField>) {
    val numericField = brewSchema.firstOrNull { it.type == "Number" }?.name ?: return
    val validBatches = batches.filter { it.status == "completed" }
    val ratingKey = validBatches.firstOrNull()?.rawData?.keys?.firstOrNull { it.contains("Rating", ignoreCase = true) || it.contains("overall", ignoreCase = true) } ?: "Overall Rating"
    val dataPoints = validBatches.mapNotNull { batch ->
        val xVal = (batch.rawData[numericField] as? Number)?.toFloat()
        val yVal = (batch.rawData[ratingKey] as? Number)?.toFloat()
        if (xVal != null && yVal != null) Offset(xVal, yVal) else null
    }
    if (dataPoints.size < 3) return

    val minX = dataPoints.minOf { it.x }.coerceAtMost(dataPoints.maxOf { it.x } - 1f)
    val maxX = dataPoints.maxOf { it.x }.coerceAtLeast(minX + 1f)

    Card(modifier = Modifier.fillMaxWidth().height(260.dp).padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("$numericField vs Rating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                for (i in 0..3) {
                    val y = canvasHeight * (i.toFloat() / 3)
                    drawLine(color = Color.Gray.copy(alpha = 0.2f), start = Offset(0f, y), end = Offset(canvasWidth, y), strokeWidth = 1.dp.toPx())
                }
                dataPoints.forEach { point ->
                    val normalizedX = ((point.x - minX) / (maxX - minX)) * canvasWidth
                    val normalizedY = (1f - ((point.y - 1f) / 6f)) * canvasHeight
                    drawCircle(color = Color(0xFF3B82F6).copy(alpha = 0.7f), radius = 6.dp.toPx(), center = Offset(normalizedX, normalizedY))
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(String.format(Locale.US, "%.1f", minX), fontSize = MaterialTheme.typography.labelSmall.fontSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(String.format(Locale.US, "%.1f", maxX), fontSize = MaterialTheme.typography.labelSmall.fontSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun BrewingCountdownBadge(batch: YogurtBatch) {
    var timeLeft by remember { mutableStateOf("") }

    LaunchedEffect(batch) {
        var hours = 0f
        batch.rawData.forEach { (k, v) ->
            if (k != "timestamp" && (k.contains("time", true) || k.contains("hours", true) || k.endsWith("(h)"))) {
                hours = when (v) {
                    is Number -> v.toFloat()
                    is String -> v.toFloatOrNull() ?: 0f
                    else -> 0f
                }
            }
        }

        val ts = when (val t = batch.rawData["timestamp"]) {
            is Number -> t.toLong()
            is String -> t.toLongOrNull() ?: 0L
            else -> 0L
        }

        val targetMillis = ts + (hours * 3600000L).toLong()

        while (true) {
            val remaining = targetMillis - System.currentTimeMillis()
            if (remaining > 0 && hours > 0f) {
                val h = remaining / 3600000
                val m = (remaining % 3600000) / 60000
                timeLeft = String.format(Locale.US, "%02d:%02d", h, m)
            } else if (hours > 0f) {
                timeLeft = "Ready"
                break
            } else {
                timeLeft = "Brewing"
                break
            }
            delay(60000)
        }
    }
    Badge(containerColor = MaterialTheme.colorScheme.primary) { Text(timeLeft, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) }
}

data class TreeNode(val batch: YogurtBatch, val depth: Int, val isLastChild: Boolean)

fun buildTree(batches: List<YogurtBatch>): List<TreeNode> {
    val childrenMap = batches.groupBy { it.rawData["forkedFrom"] as? String }
    val result = mutableListOf<TreeNode>()
    val visited = mutableSetOf<String>()
    val batchIds = batches.map { it.id }.toSet()

    val roots = batches.filter {
        val parentId = it.rawData["forkedFrom"] as? String
        parentId == null || parentId !in batchIds
    }.sortedByDescending { (it.rawData["timestamp"] as? Number)?.toLong() ?: 0L }

    fun traverse(node: YogurtBatch, depth: Int, isLast: Boolean) {
        if (node.id in visited) return
        visited.add(node.id)

        result.add(TreeNode(node, depth, isLast))
        val children = childrenMap[node.id]?.sortedByDescending { (it.rawData["timestamp"] as? Number)?.toLong() ?: 0L } ?: emptyList()
        val unvisitedChildren = children.filter { it.id !in visited }

        unvisitedChildren.forEachIndexed { index, child ->
            traverse(child, depth + 1, index == unvisitedChildren.size - 1)
        }
    }
    roots.forEachIndexed { index, root -> traverse(root, 0, index == roots.size - 1) }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(batches: List<YogurtBatch>, isLoading: Boolean, brewSchema: List<CustomField>, harvestSchema: List<CustomField>, onForkBatch: (YogurtBatch) -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val locallyHiddenBatches = remember { mutableStateListOf<String>() }

    var selectedBatchForDetails by remember { mutableStateOf<YogurtBatch?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var editBatchName by remember { mutableStateOf("") }
    val editValues = remember { mutableStateMapOf<String, String>() }
    var sortByTree by remember { mutableStateOf(false) }

    var aiInsight by remember { mutableStateOf("") }

    val batchNumbers = remember(batches) {
        batches.mapIndexed { index, batch -> batch.id to (batches.size - index) }.toMap()
    }

    val visibleTreeNodes = remember(batches, locallyHiddenBatches.size) {
        buildTree(batches).filter { it.batch.id !in locallyHiddenBatches }
    }
    val visibleTimelineBatches = remember(batches, locallyHiddenBatches.size) {
        batches.filter { it.id !in locallyHiddenBatches }
    }

    LaunchedEffect(Unit) {
        db.collection("config").document("ai_insight").addSnapshotListener { snap, _ ->
            if (snap != null && snap.exists()) {
                aiInsight = snap.getString("text") ?: ""
            }
        }
    }

    // NEW: Scroll State Engine for the Collapsing Header
    val listState = rememberLazyListState()
    val isScrolled by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 20 } }

    val logoSize by animateDpAsState(targetValue = if (isScrolled) 56.dp else 144.dp, label = "logoSize")
    val headerHeight by animateDpAsState(targetValue = if (isScrolled) 72.dp else 176.dp, label = "headerHeight")

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val centerOffsetX = (screenWidth - 32.dp) / 2 - logoSize / 2
    val logoOffsetX by animateDpAsState(targetValue = if (isScrolled) 0.dp else centerOffsetX, label = "logoOffsetX")
    val logoOffsetY by animateDpAsState(targetValue = if (isScrolled) 8.dp else 16.dp, label = "logoOffsetY")

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            Text("Made with ❤️ for Woody", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), textAlign = TextAlign.Center)
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)) {

            // NEW: Animated Collapsing Header
            Box(modifier = Modifier.fillMaxWidth().height(headerHeight), contentAlignment = Alignment.TopStart) {
                Image(
                    painter = painterResource(id = R.drawable.ww_logo),
                    contentDescription = "WW Yoghurt Logo",
                    modifier = Modifier
                        .offset(x = logoOffsetX, y = logoOffsetY)
                        .size(logoSize)
                        .clip(RoundedCornerShape(if (isScrolled) 8.dp else 16.dp))
                )
                FeedbackIconButton(
                    onClick = { exportDataToCsv(context, db) },
                    modifier = Modifier.align(Alignment.TopEnd).offset(y = logoOffsetY)
                ) {
                    Icon(DownloadCsvIcon, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (isLoading) { CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
            } else if (batches.isEmpty()) { Text("No batches found.", modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
            } else {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    if (aiInsight.isNotBlank()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(GeminiIcon, contentDescription = "AI Insight", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Copilot Insight:\n$aiInsight", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        MultiTrendChart(batches)
                        CorrelationScatterPlot(batches, brewSchema)
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                            IconToggleButton(checked = !sortByTree, onCheckedChange = { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM); sortByTree = false }) { Icon(TimelineIcon, contentDescription = "Timeline", tint = if (!sortByTree) MaterialTheme.colorScheme.primary else Color.Gray) }
                            Spacer(modifier = Modifier.width(8.dp))
                            IconToggleButton(checked = sortByTree, onCheckedChange = { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM); sortByTree = true }) { Icon(TreeLineageIcon, contentDescription = "Lineage Tree", tint = if (sortByTree) MaterialTheme.colorScheme.primary else Color.Gray) }
                        }
                    }

                    if (sortByTree) {
                        items(visibleTreeNodes, key = { it.batch.id }) { node ->
                            Row(modifier = Modifier
                                .fillMaxWidth()
                                .drawBehind {
                                    if (node.depth > 0) {
                                        val depthOffset = ((node.depth - 1) * 16).dp
                                        val lineX = depthOffset.toPx() + 8.dp.toPx()
                                        val midY = size.height / 2f
                                        val lineColor = Color.Gray.copy(alpha = 0.4f)

                                        drawLine(color = lineColor, start = Offset(lineX, 0f), end = Offset(lineX, midY), strokeWidth = 4f)
                                        if (!node.isLastChild) {
                                            drawLine(color = lineColor, start = Offset(lineX, midY), end = Offset(lineX, size.height), strokeWidth = 4f)
                                        }
                                        drawLine(color = lineColor, start = Offset(lineX, midY), end = Offset(lineX + 16.dp.toPx(), midY), strokeWidth = 4f)
                                    }
                                }
                            ) {
                                if (node.depth > 0) { Spacer(modifier = Modifier.width((node.depth * 16).dp)) }

                                val dismissState = rememberSwipeToDismissBoxState(
                                    confirmValueChange = { value ->
                                        if (value != SwipeToDismissBoxValue.Settled) {
                                            locallyHiddenBatches.add(node.batch.id)
                                            coroutineScope.launch {
                                                val result = snackbarHostState.showSnackbar("Batch deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
                                                if (result == SnackbarResult.ActionPerformed) { locallyHiddenBatches.remove(node.batch.id) } else { db.collection("brews").document(node.batch.id).delete() }
                                            }
                                            true
                                        } else false
                                    }
                                )

                                SwipeToDismissBox(
                                    state = dismissState,
                                    backgroundContent = { Box(Modifier.fillMaxSize().padding(vertical = 4.dp).background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp)), contentAlignment = Alignment.CenterEnd) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.padding(end = 16.dp)) } }
                                ) {
                                    Card(
                                        modifier = Modifier.weight(1f).padding(vertical = 4.dp).clickable { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM); selectedBatchForDetails = node.batch },
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(node.batch.batchName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                                                    val parentId = node.batch.rawData["forkedFrom"] as? String
                                                    if (parentId != null) {
                                                        val parentNum = batchNumbers[parentId]
                                                        if (parentNum != null) {
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Icon(ForkIcon, contentDescription = "Forked", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                            Spacer(modifier = Modifier.width(2.dp))
                                                            Text("#$parentNum", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                        }
                                                    }

                                                    if (node.batch.rawData["woodyApproved"] == true) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(Icons.Default.Favorite, contentDescription = "Woody Approved", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                                Text(node.batch.dateStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            if (node.batch.status == "completed") {
                                                val ratingKey = node.batch.rawData.keys.firstOrNull { it.contains("Rating", ignoreCase = true) || it.contains("overall", ignoreCase = true) } ?: "Overall Rating"
                                                val rating = (node.batch.rawData[ratingKey] as? Number)?.toInt()
                                                if (rating != null) {
                                                    val scoreColor = if (rating >= 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    Text("$rating/7", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, color = scoreColor)
                                                } else { Icon(Icons.Default.Add, contentDescription = "Done") }
                                            } else {
                                                BrewingCountdownBadge(node.batch)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        itemsIndexed(visibleTimelineBatches, key = { _, batch -> batch.id }) { index, batch ->
                            val chronologicalNumber = visibleTimelineBatches.size - index

                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value != SwipeToDismissBoxValue.Settled) {
                                        locallyHiddenBatches.add(batch.id)
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar("Batch deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
                                            if (result == SnackbarResult.ActionPerformed) { locallyHiddenBatches.remove(batch.id) } else { db.collection("brews").document(batch.id).delete() }
                                        }
                                        true
                                    } else false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = { Box(Modifier.fillMaxSize().padding(vertical = 4.dp).background(MaterialTheme.colorScheme.error, RoundedCornerShape(12.dp)), contentAlignment = Alignment.CenterEnd) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.padding(end = 16.dp)) } }
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM); selectedBatchForDetails = batch },
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text("#$chronologicalNumber", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                                        Box(modifier = Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(batch.batchName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                                                val parentId = batch.rawData["forkedFrom"] as? String
                                                if (parentId != null) {
                                                    val parentNum = batchNumbers[parentId]
                                                    if (parentNum != null) {
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Icon(ForkIcon, contentDescription = "Forked", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text("#$parentNum", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                                    }
                                                }

                                                if (batch.rawData["woodyApproved"] == true) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Icon(Icons.Default.Favorite, contentDescription = "Woody Approved", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                                                }
                                            }
                                            Text(batch.dateStr, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if (batch.status == "completed") {
                                            val ratingKey = batch.rawData.keys.firstOrNull { it.contains("Rating", ignoreCase = true) || it.contains("overall", ignoreCase = true) } ?: "Overall Rating"
                                            val rating = (batch.rawData[ratingKey] as? Number)?.toInt()
                                            if (rating != null) {
                                                val scoreColor = if (rating >= 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                Text("$rating/7", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, color = scoreColor)
                                            } else { Icon(Icons.Default.Add, contentDescription = "Done") }
                                        } else {
                                            BrewingCountdownBadge(batch)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    selectedBatchForDetails?.let { batch ->
        AlertDialog(
            onDismissRequest = { selectedBatchForDetails = null; isEditing = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isEditing) "Edit Batch" else batch.batchName, fontWeight = FontWeight.Bold)

                    if (!isEditing && batch.status == "completed") {
                        Spacer(modifier = Modifier.width(8.dp))
                        val isApproved = batch.rawData["woodyApproved"] == true
                        IconToggleButton(
                            checked = isApproved,
                            onCheckedChange = { checked ->
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                view.playSoundEffect(SoundEffectConstants.CLICK)
                                db.collection("brews").document(batch.id).set(mapOf("woodyApproved" to checked), SetOptions.merge())

                                val updatedData = batch.rawData.toMutableMap().apply { put("woodyApproved", checked) }
                                selectedBatchForDetails = batch.copy(rawData = updatedData)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Woody Approved",
                                tint = if (isApproved) Color(0xFFF59E0B) else Color.Gray.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (!isEditing) {
                        Text("Date: ${batch.dateStr}", fontWeight = FontWeight.Normal)

                        val parentId = batch.rawData["forkedFrom"] as? String
                        if (parentId != null) {
                            val parentNum = batchNumbers[parentId]
                            if (parentNum != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Icon(ForkIcon, contentDescription = "Forked", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Forked from #$parentNum", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        if (batch.rawData["woodyApproved"] == true) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth().background(Color(0xFFFEF3C7), RoundedCornerShape(8.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                Icon(Icons.Default.Favorite, contentDescription = "Woody Approved", tint = Color(0xFFD97706), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Woody's Choice", color = Color(0xFFD97706), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }

                        if (batch.status == "completed") SensoryRadarChart(batch.rawData)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Data Log", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        val ignoreKeys = listOf("batchName", "timestamp", "status", "forkedFrom", "woodyApproved")
                        batch.rawData.forEach { (key, value) ->
                            if (!ignoreKeys.contains(key)) Text("$key: $value", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal)
                        }
                    } else {
                        OutlinedTextField(
                            value = editBatchName, onValueChange = { editBatchName = it }, label = { Text("Batch Name", fontWeight = FontWeight.Normal) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        val ignoreKeys = listOf("batchName", "timestamp", "status", "forkedFrom", "woodyApproved")
                        batch.rawData.forEach { (key, value) ->
                            if (!ignoreKeys.contains(key)) {
                                OutlinedTextField(
                                    value = editValues[key] ?: "", onValueChange = { editValues[key] = it }, label = { Text(key, fontWeight = FontWeight.Normal) },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    keyboardOptions = if (value is Number) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
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
                            val mapToUpdate = hashMapOf<String, Any>()
                            mapToUpdate["batchName"] = editBatchName
                            for ((k, v) in editValues) {
                                val originalVal = batch.rawData[k]
                                mapToUpdate[k] = when (originalVal) {
                                    is Double -> v.toDoubleOrNull() ?: 0.0
                                    is Float -> v.toFloatOrNull() ?: 0f
                                    is Long -> v.toLongOrNull() ?: 0L
                                    is Int -> v.toIntOrNull() ?: 0
                                    is Number -> v.toFloatOrNull() ?: 0f
                                    else -> v
                                }
                            }
                            try {
                                db.collection("brews").document(batch.id).set(mapToUpdate, SetOptions.merge())
                                    .addOnSuccessListener { Toast.makeText(context, "Batch Updated", Toast.LENGTH_SHORT).show(); selectedBatchForDetails = null; isEditing = false }
                            } catch (e: Exception) { Toast.makeText(context, "Crash Prevented: ${e.message}", Toast.LENGTH_LONG).show() }
                        }
                    ) { Text("Save", fontWeight = FontWeight.Bold) }
                } else {
                    FeedbackButton(onClick = { selectedBatchForDetails = null }) { Text("Close", fontWeight = FontWeight.Normal) }
                }
            },
            dismissButton = {
                Row {
                    if (!isEditing) {
                        FeedbackIconButton(onClick = {
                            editBatchName = batch.batchName; editValues.clear()
                            val ignoreKeys = listOf("batchName", "timestamp", "status", "forkedFrom", "woodyApproved")
                            batch.rawData.forEach { (k, v) -> if (k !in ignoreKeys) editValues[k] = v.toString() }
                            isEditing = true
                        }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary) }
                        FeedbackIconButton(onClick = { onForkBatch(batch); selectedBatchForDetails = null }) { Icon(ForkIcon, contentDescription = "Fork Batch", tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        FeedbackIconButton(onClick = { isEditing = false }) { Icon(Icons.Default.Close, contentDescription = "Cancel Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
        )
    }
}
package com.ww.yoghurt

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

// --- Helper Functions and Chart Composables (Keep SensoryRadarChart, MultiTrendChart, CorrelationScatterPlot exactly as they are) ---
// (Omitted for brevity, but leave them at the top of your DashboardScreen.kt file!)

// Data structure to hold depth for the Tree view
data class TreeNode(val batch: YogurtBatch, val depth: Int)

// The DFS Algorithm to flatten the tree
fun buildTree(batches: List<YogurtBatch>): List<TreeNode> {
    val childrenMap = batches.groupBy { it.rawData["forkedFrom"] as? String }
    val result = mutableListOf<TreeNode>()
    val batchIds = batches.map { it.id }.toSet()

    // Find all roots (batches that either have no parent, or their parent was deleted)
    val roots = batches.filter {
        val parentId = it.rawData["forkedFrom"] as? String
        parentId == null || parentId !in batchIds
    }.sortedByDescending { it.rawData["timestamp"] as? Long ?: 0L }

    fun traverse(node: YogurtBatch, depth: Int) {
        result.add(TreeNode(node, depth))
        // Recursively find children and increase indentation depth
        val children = childrenMap[node.id]?.sortedByDescending { it.rawData["timestamp"] as? Long ?: 0L } ?: emptyList()
        children.forEach { child ->
            traverse(child, depth + 1)
        }
    }

    roots.forEach { traverse(it, 0) }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(batches: List<YogurtBatch>, isLoading: Boolean, brewSchema: List<CustomField>, harvestSchema: List<CustomField>, onForkBatch: (YogurtBatch) -> Unit) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val view = LocalView.current

    var selectedBatchForDetails by remember { mutableStateOf<YogurtBatch?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editBatchName by remember { mutableStateOf("") }
    val editValues = remember { mutableStateMapOf<String, String>() }

    // Toggle State for sorting
    var sortByTree by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Image(painter = painterResource(id = R.drawable.ww_logo), contentDescription = "WW Yoghurt Logo", modifier = Modifier.size(144.dp).clip(RoundedCornerShape(16.dp)))
            }
            FeedbackIconButton(onClick = { exportDataToCsv(context, db) }, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Share, contentDescription = "Export", tint = MaterialTheme.colorScheme.primary)
            }
        }

        if (isLoading) { CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
        } else if (batches.isEmpty()) { Text("No batches found.", modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {

                // Keep the charts at the top
                /* item { Spacer(modifier = Modifier.height(24.dp)); MultiTrendChart(batches); CorrelationScatterPlot(batches, brewSchema) } */

                // The Toggle UI
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.End) {
                        FilterChip(
                            selected = !sortByTree,
                            onClick = { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM); sortByTree = false },
                            label = { Text("Timeline") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = sortByTree,
                            onClick = { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM); sortByTree = true },
                            label = { Text("Lineage Tree") }
                        )
                    }
                }

                if (sortByTree) {
                    val treeNodes = buildTree(batches)
                    items(treeNodes) { node ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                            // The Canvas Indentation Engine
                            if (node.depth > 0) {
                                Spacer(modifier = Modifier.width((node.depth * 16).dp))
                                Canvas(modifier = Modifier.width(16.dp).height(48.dp)) {
                                    val lineColor = Color.Gray.copy(alpha = 0.4f)
                                    // Vertical line from above
                                    drawLine(color = lineColor, start = Offset(8.dp.toPx(), -20.dp.toPx()), end = Offset(8.dp.toPx(), 24.dp.toPx()), strokeWidth = 4f)
                                    // Horizontal arm touching the card
                                    drawLine(color = lineColor, start = Offset(8.dp.toPx(), 24.dp.toPx()), end = Offset(16.dp.toPx(), 24.dp.toPx()), strokeWidth = 4f)
                                }
                            }

                            // The actual Card
                            Card(
                                modifier = Modifier.weight(1f).clickable { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM); selectedBatchForDetails = node.batch },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(node.batch.batchName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
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
                                        Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("Brewing", color = Color.White, fontWeight = FontWeight.Normal, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Standard Chronological View
                    itemsIndexed(batches) { index, batch ->
                        val chronologicalNumber = batches.size - index
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { view.performHapticFeedback(HapticFeedbackConstants.CONFIRM); selectedBatchForDetails = batch },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("#$chronologicalNumber", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 12.dp))
                                Box(modifier = Modifier.width(1.dp).height(36.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))
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
                                    } else { Icon(Icons.Default.Add, contentDescription = "Done") }
                                } else {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("Brewing", color = Color.White, fontWeight = FontWeight.Normal, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("Made with ❤️ for Woody", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    selectedBatchForDetails?.let { batch ->
        AlertDialog(
            onDismissRequest = { selectedBatchForDetails = null; isEditing = false },
            title = { Text(if (isEditing) "Edit Batch" else batch.batchName, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    if (!isEditing) {
                        Text("Date: ${batch.dateStr}", fontWeight = FontWeight.Normal)

                        // NEW: Show Lineage Data if it exists
                        val parentId = batch.rawData["forkedFrom"] as? String
                        if (parentId != null) {
                            val parentBatch = batches.find { it.id == parentId }
                            if (parentBatch != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Icon(ForkIcon, contentDescription = "Forked", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Forked from ${parentBatch.batchName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }

                        /* if (batch.status == "completed") SensoryRadarChart(batch.rawData) */ // Make sure your charts are here!
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        Text("Data Log", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        val ignoreKeys = listOf("batchName", "timestamp", "status", "forkedFrom")
                        batch.rawData.forEach { (key, value) ->
                            if (!ignoreKeys.contains(key)) Text("$key: $value", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal)
                        }
                    } else {
                        OutlinedTextField(
                            value = editBatchName, onValueChange = { editBatchName = it }, label = { Text("Batch Name", fontWeight = FontWeight.Normal) },
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        val ignoreKeys = listOf("batchName", "timestamp", "status", "forkedFrom")
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
                            val ignoreKeys = listOf("batchName", "timestamp", "status", "forkedFrom")
                            batch.rawData.forEach { (k, v) -> if (k !in ignoreKeys) editValues[k] = v.toString() }
                            isEditing = true
                        }) { Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary) }
                        FeedbackIconButton(onClick = { onForkBatch(batch); selectedBatchForDetails = null }) { Icon(ForkIcon, contentDescription = "Fork Batch", tint = MaterialTheme.colorScheme.primary) }
                    } else {
                        FeedbackIconButton(onClick = { isEditing = false }) { Icon(Icons.Default.Close, contentDescription = "Cancel Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    FeedbackIconButton(onClick = { showDeleteConfirm = true }) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
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
                FeedbackButton(colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error), onClick = {
                    db.collection("brews").document(selectedBatchForDetails!!.id).delete()
                    showDeleteConfirm = false; selectedBatchForDetails = null
                }) { Text("Delete", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", fontWeight = FontWeight.Normal) } }
        )
    }
}
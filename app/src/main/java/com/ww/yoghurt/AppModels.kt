package com.ww.yoghurt

import androidx.compose.ui.graphics.vector.ImageVector

data class CustomField(val name: String, val type: String)

data class YogurtBatch(
    val id: String,
    val batchName: String,
    val dateStr: String,
    val status: String,
    val rawData: Map<String, Any>
)

data class PendingBrew(val id: String, val displayLabel: String)

data class ChatMessage(val role: String, val text: String)

// NEW: Data model for the Firestore Chat Ledger
data class SavedChat(
    val id: String,
    val prompt: String,
    val response: String,
    val timestamp: Long
)

data class TabItem(val title: String, val icon: ImageVector)

val defaultBrewSchema = listOf(
    CustomField("Milk Brand", "Text"),
    CustomField("Starter Culture", "Text"),
    CustomField("Protein Added (g)", "Number"),
    CustomField("Heating Temp (C)", "Number"),
    CustomField("Fermentation Time (h)", "Number")
)

val defaultHarvestSchema = listOf(
    CustomField("Acidity", "Slider"),
    CustomField("Density", "Slider"),
    CustomField("Texture", "Slider"),
    CustomField("Overall Rating", "Slider"),
    CustomField("Notes", "Text")
)
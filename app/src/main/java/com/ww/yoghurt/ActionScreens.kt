package com.ww.yoghurt

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoCompleteTextField(
    value: String, onValueChange: (String) -> Unit, label: String, options: Set<String>, isNumber: Boolean, isLargeText: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    val filteredOptions = options.filter { it.contains(value, ignoreCase = true) && it != value }

    ExposedDropdownMenuBox(expanded = expanded && filteredOptions.isNotEmpty(), onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = value, onValueChange = { onValueChange(it); expanded = true }, label = { Text(label, fontWeight = FontWeight.Normal) },
            modifier = Modifier.menuAnchor().fillMaxWidth().height(if (isLargeText) 120.dp else 60.dp),
            keyboardOptions = if (isNumber) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            maxLines = if (isLargeText) 5 else 1, textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal)
        )
        if (filteredOptions.isNotEmpty()) {
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                filteredOptions.forEach { option ->
                    DropdownMenuItem(text = { Text(option, fontWeight = FontWeight.Normal) }, onClick = { onValueChange(option); expanded = false })
                }
            }
        }
    }
}

@Composable
fun YogurtBrewScreen(
    schema: List<CustomField>,
    historicalData: Map<String, Set<String>>,
    forkedBatchName: String,
    forkedFromId: String?,
    forkedValues: Map<String, String>,
    onClearFork: () -> Unit
) {
    var batchName by remember { mutableStateOf("") }
    val dynamicTextValues = remember { mutableStateMapOf<String, String>() }
    var isSaving by remember { mutableStateOf(false) }

    // Notification Toggle State
    var enableNotification by remember { mutableStateOf(true) }

    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    // Permission Launcher for Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (!isGranted) {
            enableNotification = false
            Toast.makeText(context, "Notifications disabled in settings.", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(forkedBatchName) {
        if (forkedBatchName.isNotBlank()) {
            batchName = forkedBatchName
            dynamicTextValues.clear()
            forkedValues.forEach { (k, v) -> dynamicTextValues[k] = v }
        }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Phase 1: The Brew", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Log ingredients and environment.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = batchName, onValueChange = { batchName = it; onClearFork() }, label = { Text("Batch Name (e.g., Batch #1)", fontWeight = FontWeight.Normal) },
                    modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal), enabled = !isSaving
                )
                Spacer(modifier = Modifier.height(8.dp))
                schema.forEach { field ->
                    AutoCompleteTextField(
                        value = dynamicTextValues[field.name] ?: "", onValueChange = { dynamicTextValues[field.name] = it; onClearFork() },
                        label = field.name, options = historicalData[field.name] ?: emptySet(), isNumber = field.type == "Number"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Notification Toggle UI
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Set alarm for Harvest Time", fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = enableNotification,
                        onCheckedChange = { isChecked ->
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            enableNotification = isChecked
                            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
                FeedbackButton(
                    onClick = {
                        if (batchName.isNotBlank()) {
                            isSaving = true
                            coroutineScope.launch {
                                var weatherData = "Unknown"
                                try {
                                    val response = withContext(Dispatchers.IO) { URL("https://api.open-meteo.com/v1/forecast?latitude=37.98&longitude=23.72&current_weather=true").readText() }
                                    val temp = JSONObject(response).getJSONObject("current_weather").getDouble("temperature")
                                    weatherData = "$temp °C"
                                } catch (e: Exception) {}

                                val brewData = hashMapOf<String, Any>(
                                    "batchName" to batchName,
                                    "timestamp" to System.currentTimeMillis(),
                                    "status" to "brewing",
                                    "Ambient Temp (Athens)" to weatherData
                                )

                                if (forkedFromId != null) brewData["forkedFrom"] = forkedFromId

                                var hoursToWait = 0f
                                schema.forEach { field ->
                                    val stringVal = dynamicTextValues[field.name] ?: ""
                                    if (field.type == "Number") {
                                        val num = stringVal.toFloatOrNull() ?: 0f
                                        brewData[field.name] = num
                                        // Detect if this field represents Fermentation Time
                                        if (field.name.contains("time", ignoreCase = true) || field.name.contains("hours", ignoreCase = true) || field.name.endsWith("(h)")) {
                                            hoursToWait = num
                                        }
                                    } else {
                                        brewData[field.name] = stringVal
                                    }
                                }

                                // Schedule the Push Notification
                                if (enableNotification && hoursToWait > 0f) {
                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                                    val intent = Intent(context, YogurtAlarmReceiver::class.java).apply {
                                        putExtra("BATCH_NAME", batchName)
                                    }
                                    val pendingIntent = PendingIntent.getBroadcast(
                                        context,
                                        batchName.hashCode(),
                                        intent,
                                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                    )

                                    val timeInMillis = System.currentTimeMillis() + (hoursToWait * 3600000L).toLong()

                                    try {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            if (alarmManager.canScheduleExactAlarms()) {
                                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                                            } else {
                                                alarmManager.set(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                                            }
                                        } else {
                                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
                                        }
                                    } catch (e: SecurityException) {
                                        withContext(Dispatchers.Main) { Toast.makeText(context, "Alarm permission denied by OS.", Toast.LENGTH_SHORT).show() }
                                    }
                                }

                                db.collection("brews").add(brewData).addOnSuccessListener {
                                    Toast.makeText(context, "Brew saved!", Toast.LENGTH_SHORT).show()
                                    batchName = ""; dynamicTextValues.clear(); onClearFork(); isSaving = false
                                }.addOnFailureListener { e -> Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show(); isSaving = false }
                            }
                        } else { Toast.makeText(context, "Batch Name is required.", Toast.LENGTH_SHORT).show() }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !isSaving
                ) { if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Save Brew Phase", fontWeight = FontWeight.Bold) }
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
    val db = FirebaseFirestore.getInstance()
    val view = LocalView.current

    LaunchedEffect(Unit) {
        db.collection("brews").whereEqualTo("status", "brewing").get().addOnSuccessListener { snapshot ->
            val brews = snapshot.documents.mapNotNull { doc ->
                val name = doc.getString("batchName") ?: "Unknown"
                val ts = doc.getLong("timestamp") ?: 0L
                PendingBrew(doc.id, "$name (${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(ts))})")
            }
            pendingBrews = brews; if (brews.isNotEmpty()) selectedBrew = brews.first()
            isLoading = false
        }.addOnFailureListener { isLoading = false }
    }

    Column(modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Phase 2: The Harvest", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Tasting and evaluation.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (isLoading) { CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (pendingBrews.isEmpty()) { Text("No active brews waiting.", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Normal)
                } else {
                    ExposedDropdownMenuBox(expanded = dropdownExpanded, onExpandedChange = { dropdownExpanded = it }) {
                        OutlinedTextField(
                            value = selectedBrew?.displayLabel ?: "", onValueChange = {}, readOnly = true, label = { Text("Select Target Batch", fontWeight = FontWeight.Normal) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth(),
                            textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal), colors = OutlinedTextFieldDefaults.colors(disabledTextColor = MaterialTheme.colorScheme.onSurface)
                        )
                        ExposedDropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                            pendingBrews.forEach { brew -> DropdownMenuItem(text = { Text(brew.displayLabel, fontWeight = FontWeight.Normal) }, onClick = { selectedBrew = brew; dropdownExpanded = false }) }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                schema.forEach { field ->
                    if (field.type == "Slider") {
                        val sliderVal = dynamicSliderValues[field.name] ?: 4f
                        Text("${field.name}: ${sliderVal.toInt()}", fontWeight = FontWeight.Bold)
                        Slider(value = sliderVal, onValueChange = { dynamicSliderValues[field.name] = it }, valueRange = 1f..7f, steps = 5)
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        AutoCompleteTextField(
                            value = dynamicTextValues[field.name] ?: "", onValueChange = { dynamicTextValues[field.name] = it },
                            label = field.name, options = historicalData[field.name] ?: emptySet(), isNumber = field.type == "Number", isLargeText = field.name == "Notes"
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
                                if (field.type == "Slider") data[field.name] = (dynamicSliderValues[field.name] ?: 4f).toInt()
                                else if (field.type == "Number") data[field.name] = (dynamicTextValues[field.name] ?: "").toFloatOrNull() ?: 0f
                                else data[field.name] = dynamicTextValues[field.name] ?: ""
                            }
                            db.collection("brews").document(brew.id).update(data).addOnSuccessListener {
                                Toast.makeText(context, "Harvest saved!", Toast.LENGTH_SHORT).show()
                                dynamicSliderValues.clear(); dynamicTextValues.clear(); pendingBrews = pendingBrews.filter { it.id != brew.id }
                                selectedBrew = pendingBrews.firstOrNull(); isSaving = false
                            }.addOnFailureListener { isSaving = false }
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !isSaving && selectedBrew != null
                ) { if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Save Harvest", fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun AnalysisScreen(batches: List<YogurtBatch>) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
    val sharedPrefs = context.getSharedPreferences("WWPrefs", Context.MODE_PRIVATE)
    val apiKey = sharedPrefs.getString("GEMINI_API_KEY", "") ?: ""
    val view = LocalView.current

    var prompt by remember { mutableStateOf("") }
    var savedChats by remember { mutableStateOf<List<SavedChat>>(emptyList()) }
    var expandedChatIds by remember { mutableStateOf(setOf<String>()) }
    var isThinking by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val listener = db.collection("chats").orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    savedChats = snapshot.documents.map { doc ->
                        SavedChat(doc.id, doc.getString("prompt") ?: "", doc.getString("response") ?: "", doc.getLong("timestamp") ?: 0L)
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
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text("API Key Missing. Please configure it in the System tab.", modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onErrorContainer)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), reverseLayout = false) {
                items(savedChats) { chat ->
                    val isExpanded = expandedChatIds.contains(chat.id)
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                            view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            view.playSoundEffect(SoundEffectConstants.CLICK)
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
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(24.dp).align(Alignment.CenterHorizontally), strokeWidth = 2.dp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = prompt, onValueChange = { prompt = it }, placeholder = { Text("E.g., Why was my last batch so tart?", fontWeight = FontWeight.Normal) },
                    modifier = Modifier.weight(1f), enabled = !isThinking, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface, unfocusedContainerColor = MaterialTheme.colorScheme.surface)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FeedbackIconButton(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            val userText = prompt; prompt = ""; isThinking = true
                            coroutineScope.launch {
                                try {
                                    val contextData = StringBuilder("You are WW Yoghurt AI, an expert in fermentation. Here is the user's historical batch data (in JSON/Map format):\n\n")
                                    batches.forEach { batch -> contextData.append("Batch: ${batch.batchName}, Status: ${batch.status}, Data: ${batch.rawData}\n") }
                                    contextData.append("\nBased strictly on the data above and your expertise, answer the user's prompt: $userText. Do not use markdown bolding in your response.")
                                    val generativeModel = GenerativeModel(modelName = "gemini-3-flash-preview", apiKey = apiKey)
                                    val response = generativeModel.generateContent(contextData.toString())
                                    val chatDoc = hashMapOf("prompt" to userText, "response" to (response.text?.replace("**", "") ?: "No response generated."), "timestamp" to System.currentTimeMillis())
                                    db.collection("chats").add(chatDoc)
                                } catch (e: Exception) { Toast.makeText(context, "AI Error: ${e.message}", Toast.LENGTH_LONG).show() } finally { isThinking = false }
                            }
                        }
                    }, modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(12.dp)), enabled = !isThinking && prompt.isNotBlank()
                ) { Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White) }
            }
        }
    }
}

@Composable
fun SystemSettingsScreen(currentBrewSchema: List<CustomField>, currentHarvestSchema: List<CustomField>, coroutineScope: CoroutineScope) {
    val context = LocalContext.current
    val db = FirebaseFirestore.getInstance()
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

    Column(modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("System Engine", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Modify global variables & integrations.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("AI Integration", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Enter your Gemini API Key. This is stored securely on this device.", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKeyInput, onValueChange = { apiKeyInput = it }, label = { Text("Gemini API Key", fontWeight = FontWeight.Normal) },
                    modifier = Modifier.fillMaxWidth(), textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal), singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                FeedbackButton(onClick = { sharedPrefs.edit().putString("GEMINI_API_KEY", apiKeyInput).apply(); Toast.makeText(context, "API Key Saved Locally", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth()) { Text("Save API Key", fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Schema Editor", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Brew Phase Fields", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                editBrewSchema.forEachIndexed { index, field ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
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
                        db.collection("config").document("schema").set(configData).addOnSuccessListener { Toast.makeText(context, "Schema Updated Globally", Toast.LENGTH_SHORT).show(); isSaving = false }.addOnFailureListener { isSaving = false }
                    }, modifier = Modifier.fillMaxWidth(), enabled = !isSaving
                ) { if (isSaving) CircularProgressIndicator(modifier = Modifier.size(24.dp)) else Text("Save & Apply Schema", fontWeight = FontWeight.Bold) }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = { showChangelogDialog = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) { Text("View Full Changelog", fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { checkForUpdates(context, coroutineScope) }, modifier = Modifier.fillMaxWidth()) { Text("Download Latest Update", fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Version v$APP_VERSION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Normal, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (showChangelogDialog) {
        AlertDialog(
            onDismissRequest = { showChangelogDialog = false }, title = { Text("App Changelog", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn {
                    appChangelog.forEach { (version, changes) ->
                        item {
                            Text(version, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                            changes.forEach { change -> Text("• $change", fontWeight = FontWeight.Normal, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 8.dp, bottom = 2.dp)) }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }
            }, confirmButton = { Button(onClick = { showChangelogDialog = false }) { Text("Close", fontWeight = FontWeight.Bold) } }
        )
    }

    if (showAddFieldDialog) {
        AlertDialog(
            onDismissRequest = { showAddFieldDialog = false }, title = { Text("Add Field to $fieldToAddPhase", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(value = newFieldName, onValueChange = { newFieldName = it }, label = { Text("Parameter Name", fontWeight = FontWeight.Normal) }, textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Normal))
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
            confirmButton = { FeedbackButton(onClick = { if (newFieldName.isNotBlank()) { val newField = CustomField(newFieldName, newFieldType); if (fieldToAddPhase == "Brew") editBrewSchema.add(newField) else editHarvestSchema.add(newField); newFieldName = ""; showAddFieldDialog = false } }) { Text("Add", fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { showAddFieldDialog = false }) { Text("Cancel", fontWeight = FontWeight.Normal) } }
        )
    }
}
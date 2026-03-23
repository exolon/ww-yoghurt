package com.ww.yoghurt

import android.app.AlarmManager
import android.app.DownloadManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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

const val APP_VERSION = "1.3.0"

val appChangelog = mapOf(
    "v1.3.0" to listOf("Added Push Notifications for fermentation tracking.", "Added toggle to silence alarms per-batch.", "Version Code bumped to 5."),
    "v1.2.3" to listOf("Fixed Firestore restricted character save error using SetOptions.merge().", "Added Correlation Scatter Plot to Dashboard.", "Version Code bumped."),
    "v1.2.0" to listOf("Rewrote edit engine to eliminate ClassCastException crashes.", "Added Multi-Variate Trend Chart.", "Batch names are now editable.", "Modularized architecture into 6 distinct files.", "Version Code bumped to 4."),
    "v1.1.1" to listOf("Fixed Radar Chart label placement.", "Fixed GitHub 302 Redirect APK parsing error.", "Added Semantic Version checking.", "Moved Changelog to dialog."),
    "v1.1.0" to listOf("Updated Gemini Model to v3 Flash.", "Added Historical Auto-Complete.", "Added Sensory Radar Charts.", "Added Batch Edit & Fork features.")
)

val GeminiIcon: ImageVector
    get() = ImageVector.Builder(name = "Gemini", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(12f, 0f); curveTo(12f, 0f, 14.5f, 9.5f, 24f, 12f)
            curveTo(24f, 12f, 14.5f, 14.5f, 12f, 24f); curveTo(12f, 24f, 9.5f, 14.5f, 0f, 12f)
            curveTo(0f, 12f, 9.5f, 9.5f, 12f, 0f); close()
        }
        path(fill = SolidColor(Color.Black)) {
            moveTo(20.5f, 0f); curveTo(20.5f, 0f, 21.5f, 3.5f, 24f, 4.5f)
            curveTo(24f, 4.5f, 21.5f, 5.5f, 20.5f, 9f); curveTo(20.5f, 9f, 19.5f, 5.5f, 17f, 4.5f)
            curveTo(17f, 4.5f, 19.5f, 3.5f, 20.5f, 0f); close()
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

// Background Broadcast Receiver for the Push Notification
class YogurtAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val batchName = intent.getStringExtra("BATCH_NAME") ?: "Your batch"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "YOGHURT_CHANNEL")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Fermentation Complete!")
            .setContentText("$batchName is ready to be transferred to the fridge.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

fun exportDataToCsv(context: Context, db: FirebaseFirestore) {
    Toast.makeText(context, "Compiling CSV...", Toast.LENGTH_SHORT).show()
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
                docs.forEach { doc -> doc.data?.keys?.filter { it !in ignoreKeys }?.let { dynamicKeys.addAll(it) } }
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
                exportFile.writeText(csv.toString())
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", exportFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply { type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                context.startActivity(Intent.createChooser(shareIntent, "Export Database"))
            } catch (e: Exception) { Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show() }
        }.addOnFailureListener { Toast.makeText(context, "Failed to fetch Data.", Toast.LENGTH_SHORT).show() }
}

fun checkForUpdates(context: Context, coroutineScope: CoroutineScope) {
    Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
    coroutineScope.launch {
        try {
            val response = withContext(Dispatchers.IO) { URL("https://api.github.com/repos/exolon/ww-yoghurt/releases/latest").readText() }
            val json = JSONObject(response)
            val latestTag = json.getString("tag_name")
            val lParts = latestTag.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
            val cParts = APP_VERSION.replace(Regex("[^0-9.]"), "").split(".").map { it.toIntOrNull() ?: 0 }
            var isNewer = false
            for (i in 0 until maxOf(lParts.size, cParts.size)) {
                val lVal = lParts.getOrElse(i) { 0 }; val cVal = cParts.getOrElse(i) { 0 }
                if (lVal > cVal) { isNewer = true; break }
                if (lVal < cVal) break
            }
            if (isNewer) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Downloading $latestTag...", Toast.LENGTH_LONG).show()
                    val githubReleaseUrl = json.getJSONArray("assets").getJSONObject(0).getString("browser_download_url")
                    val request = DownloadManager.Request(Uri.parse(githubReleaseUrl))
                        .setTitle("WW Yoghurt Update").setDescription("Downloading $latestTag...")
                        .setMimeType("application/vnd.android.package-archive")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val downloadId = downloadManager.enqueue(request)
                    val onComplete = object : BroadcastReceiver() {
                        override fun onReceive(ctxt: Context, intent: Intent) {
                            if (intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) == downloadId) {
                                downloadManager.getUriForDownloadedFile(downloadId)?.let { uri ->
                                    ctxt.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION })
                                } ?: Toast.makeText(ctxt, "Update failed.", Toast.LENGTH_LONG).show()
                                ctxt.unregisterReceiver(this)
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
                    else context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
                }
            } else withContext(Dispatchers.Main) { Toast.makeText(context, "You are on the latest version.", Toast.LENGTH_SHORT).show() }
        } catch (e: Exception) { withContext(Dispatchers.Main) { Toast.makeText(context, "Check failed.", Toast.LENGTH_SHORT).show() } }
    }
}
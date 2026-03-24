package com.ww.yoghurt

import android.app.DownloadManager
import android.app.NotificationManager
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

const val APP_VERSION = "1.5.0"

val appChangelog = mapOf(
    "v1.5.0" to listOf("Auto-navigate to Dashboard after saving a Brew.", "Updater string logic improvements."),
    "v1.4.1" to listOf("Moved Woody's Choice toggle to Dashboard dialog.", "Fixed Android 13+ Notification Permissions."),
    "v1.4.0" to listOf("Added Proactive AI Copilot Insight.", "Added Lineage Tree continuous rendering.", "Added Swipe-to-Delete with Undo."),
    "v1.3.0" to listOf("Added Push Notifications for fermentation tracking.", "Version Code bumped to 5.")
)

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

val DownloadCsvIcon: ImageVector
    get() = ImageVector.Builder(name = "DownloadCsv", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(19f, 9f); lineTo(15f, 9f); lineTo(15f, 3f); lineTo(9f, 3f); lineTo(9f, 9f); lineTo(5f, 9f); lineTo(12f, 16f); lineTo(19f, 9f); close()
            moveTo(5f, 18f); lineTo(19f, 18f); lineTo(19f, 20f); lineTo(5f, 20f); close()
        }
    }.build()

val TimelineIcon: ImageVector
    get() = ImageVector.Builder(name = "Timeline", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(3f, 4f); lineTo(7f, 4f); lineTo(7f, 8f); lineTo(3f, 8f); close()
            moveTo(9f, 5f); lineTo(21f, 5f); lineTo(21f, 7f); lineTo(9f, 7f); close()
            moveTo(3f, 10f); lineTo(7f, 10f); lineTo(7f, 14f); lineTo(3f, 14f); close()
            moveTo(9f, 11f); lineTo(21f, 11f); lineTo(21f, 13f); lineTo(9f, 13f); close()
            moveTo(3f, 16f); lineTo(7f, 16f); lineTo(7f, 20f); lineTo(3f, 20f); close()
            moveTo(9f, 17f); lineTo(21f, 17f); lineTo(21f, 19f); lineTo(9f, 19f); close()
        }
    }.build()

val TreeLineageIcon: ImageVector
    get() = ImageVector.Builder(name = "TreeLineage", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f).apply {
        path(fill = SolidColor(Color.Black)) {
            moveTo(17f, 3f); lineTo(22f, 3f); lineTo(22f, 8f); lineTo(17f, 8f); close()
            moveTo(2f, 9f); lineTo(7f, 9f); lineTo(7f, 14f); lineTo(2f, 14f); close()
            moveTo(17f, 15f); lineTo(22f, 15f); lineTo(22f, 20f); lineTo(17f, 20f); close()
            moveTo(9f, 11f); lineTo(14f, 11f); lineTo(14f, 5f); lineTo(17f, 5f); lineTo(17f, 6f); lineTo(15f, 6f); lineTo(15f, 12f); lineTo(9f, 12f); close()
            moveTo(15f, 12f); lineTo(15f, 18f); lineTo(17f, 18f); lineTo(17f, 17f); lineTo(14f, 17f); lineTo(14f, 11f); close()
        }
    }.build()

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
                val ignoreKeys = setOf("batchName", "timestamp", "status", "forkedFrom", "woodyApproved")
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
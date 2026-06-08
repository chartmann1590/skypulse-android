package com.charles.skypulse.app.domain.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Locale
import java.util.concurrent.TimeUnit

class DiagnosticsGatherer(private val context: Context) {

    suspend fun gatherDiagnostics(includeModels: Boolean): String = withContext(Dispatchers.IO) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appVersionName = packageInfo.versionName ?: "Unknown"
        val appVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo)

        val filesDir = context.filesDir
        val totalSpaceBytes = filesDir.totalSpace
        val freeSpaceBytes = filesDir.freeSpace
        val totalSpaceGb = totalSpaceBytes / (1024.0 * 1024.0 * 1024.0)
        val freeSpaceGb = freeSpaceBytes / (1024.0 * 1024.0 * 1024.0)

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val totalRamBytes = memoryInfo.totalMem
        val freeRamBytes = memoryInfo.availMem
        val totalRamGb = totalRamBytes / (1024.0 * 1024.0 * 1024.0)
        val freeRamGb = freeRamBytes / (1024.0 * 1024.0 * 1024.0)

        val sb = StringBuilder()
        sb.append("### System Diagnostics\n")
        sb.append("- **Device Brand:** ").append(Build.BRAND).append("\n")
        sb.append("- **Device Model:** ").append(Build.MODEL).append("\n")
        sb.append("- **Manufacturer:** ").append(Build.MANUFACTURER).append("\n")
        sb.append("- **Android Version:** ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n")
        sb.append("- **App Version:** ").append(appVersionName).append(" (Code ").append(appVersionCode).append(")\n")
        sb.append("- **System Locale:** ").append(Locale.getDefault().toString()).append("\n")
        sb.append("- **Disk Storage:** ").append(String.format(Locale.US, "%.2f GB free / %.2f GB total", freeSpaceGb, totalSpaceGb)).append("\n")
        sb.append("- **System Memory:** ").append(String.format(Locale.US, "%.2f GB free / %.2f GB total RAM", freeRamGb, totalRamGb)).append("\n")

        if (includeModels) {
            sb.append("\n### Configured Models\n")
            val models = getOllamaModels()
            if (models.isEmpty()) {
                sb.append("_No local or remote Ollama models detected (service unreachable)._\n")
            } else {
                models.forEach { (name, location) ->
                    sb.append("- **Model:** `").append(name).append("` (Type: ").append(location).append(")\n")
                }
            }
        }
        sb.toString()
    }

    private fun getOllamaModels(): List<Pair<String, String>> {
        val client = OkHttpClient.Builder()
            .connectTimeout(1, TimeUnit.SECONDS)
            .readTimeout(1, TimeUnit.SECONDS)
            .build()

        // Try local emulator loopback, localhost, and LifeCaptureOS network host
        val hosts = listOf("10.0.2.2", "127.0.0.1", "10.0.0.74")
        for (host in hosts) {
            try {
                val request = Request.Builder()
                    .url("http://$host:11434/api/tags")
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val bodyString = response.body?.string() ?: ""
                        val json = Json.parseToJsonElement(bodyString).jsonObject
                        val modelsArray = json["models"]?.jsonArray
                        if (modelsArray != null) {
                            return modelsArray.map { modelElement ->
                                val name = modelElement.jsonObject["name"]?.jsonPrimitive?.content ?: "Unknown"
                                val isOnDevice = host == "127.0.0.1"
                                name to (if (isOnDevice) "On-Device" else "Remote (Ollama Host: $host)")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Continue to next host
            }
        }
        return emptyList()
    }
}

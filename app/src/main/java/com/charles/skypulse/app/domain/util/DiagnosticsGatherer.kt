package com.charles.skypulse.app.domain.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class DiagnosticsGatherer(private val context: Context) {

    suspend fun gatherDiagnostics(): String = withContext(Dispatchers.IO) {
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
        sb.toString()
    }
}

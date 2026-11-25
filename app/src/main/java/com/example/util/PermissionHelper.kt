package com.example.util

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.ContextCompat
import com.example.service.NexoraAccessibilityService

data class PermissionStatusSnapshot(
    val microphoneGranted: Boolean = false,
    val notificationsGranted: Boolean = true,
    val overlayGranted: Boolean = false,
    val accessibilityEnabled: Boolean = false
)

data class DeviceGamingTelemetry(
    val availableRamMb: Long,
    val totalRamMb: Long,
    val batteryPercent: Int,
    val batteryTempC: Float,
    val isLowMemory: Boolean
)

object PermissionHelper {

    fun checkPermissions(context: Context): PermissionStatusSnapshot {
        val mic = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val notif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val overlay = Settings.canDrawOverlays(context)
        val accessibility = isAccessibilityServiceEnabled(context) ||
            NexoraAccessibilityService.instance != null

        return PermissionStatusSnapshot(
            microphoneGranted = mic,
            notificationsGranted = notif,
            overlayGranted = overlay,
            accessibilityEnabled = accessibility
        )
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponent = "${context.packageName}/${NexoraAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabledServices)
        while (splitter.hasNext()) {
            val component = splitter.next()
            if (component.equals(expectedComponent, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun decodeUriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun readDeviceGamingTelemetry(context: Context): DeviceGamingTelemetry {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)

        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320) ?: 320
        val pct = if (level >= 0 && scale > 0) (level * 100) / scale else 100

        return DeviceGamingTelemetry(
            availableRamMb = memInfo.availMem / (1024L * 1024L),
            totalRamMb = memInfo.totalMem / (1024L * 1024L),
            batteryPercent = pct,
            batteryTempC = tempTenths / 10f,
            isLowMemory = memInfo.lowMemory
        )
    }
}

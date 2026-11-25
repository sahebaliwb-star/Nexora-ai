package com.example.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.example.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "ScreenCaptureService"
private const val CHANNEL_ID = "nexora_screen_capture_channel"
private const val NOTIFICATION_ID = 2002

class ScreenCaptureService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, Activity.RESULT_CANCELED)
            ?: Activity.RESULT_CANCELED
        val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(EXTRA_RESULT_DATA)
        }

        if (resultCode != Activity.RESULT_OK || resultData == null) {
            _captureError.value = "স্ক্রিন ক্যাপচার অনুমতি পাওয়া যায়নি।"
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundWithType()
        captureSingleFrame(resultCode, resultData)
        return START_NOT_STICKY
    }

    private fun startForegroundWithType() {
        createNotificationChannel()
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.capture_notification_title))
            .setContentText(getString(R.string.capture_notification_text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun captureSingleFrame(resultCode: Int, data: Intent) {
        try {
            val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val projection = manager.getMediaProjection(resultCode, data)
            if (projection == null) {
                _captureError.value = "MediaProjection তৈরি করা যায়নি।"
                stopSelf()
                return
            }
            mediaProjection = projection
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    cleanupProjection()
                }
            }, mainHandler)

            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            val width = (metrics.widthPixels / 2).coerceAtLeast(360)
            val height = (metrics.heightPixels / 2).coerceAtLeast(640)
            val density = metrics.densityDpi

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            imageReader = reader

            virtualDisplay = projection.createVirtualDisplay(
                "NexoraScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                mainHandler
            )

            // Wait briefly for first frame to render, capture once, then immediately tear down projection
            mainHandler.postDelayed({
                try {
                    val image = reader.acquireLatestImage()
                    if (image != null) {
                        val plane = image.planes[0]
                        val buffer = plane.buffer
                        val pixelStride = plane.pixelStride
                        val rowStride = plane.rowStride
                        val rowPadding = rowStride - pixelStride * width
                        val paddedBitmap = Bitmap.createBitmap(
                            width + rowPadding / pixelStride,
                            height,
                            Bitmap.Config.ARGB_8888
                        )
                        paddedBitmap.copyPixelsFromBuffer(buffer)
                        val cropped = Bitmap.createBitmap(paddedBitmap, 0, 0, width, height)
                        paddedBitmap.recycle()
                        image.close()
                        _capturedBitmap.value = cropped
                        _captureError.value = null
                    } else {
                        _captureError.value = "স্ক্রিন ফ্রেম পাওয়া যায়নি। আবার চেষ্টা করুন।"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Frame capture failed", e)
                    _captureError.value = "স্ক্রিন ক্যাপচার ত্রুটি: ${e.localizedMessage ?: "Failed"}"
                } finally {
                    cleanupProjection()
                    stopSelf()
                }
            }, 450L)
        } catch (e: Exception) {
            Log.e(TAG, "MediaProjection setup failed", e)
            _captureError.value = "স্ক্রিন ক্যাপচার শুরু করা যায়নি: ${e.localizedMessage ?: "Error"}"
            cleanupProjection()
            stopSelf()
        }
    }

    private fun cleanupProjection() {
        try {
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Cleanup warning", e)
        } finally {
            virtualDisplay = null
            imageReader = null
            mediaProjection = null
        }
    }

    override fun onDestroy() {
        cleanupProjection()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.capture_notification_title),
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"

        private val _capturedBitmap = MutableStateFlow<Bitmap?>(null)
        val capturedBitmap: StateFlow<Bitmap?> = _capturedBitmap.asStateFlow()

        private val _captureError = MutableStateFlow<String?>(null)
        val captureError: StateFlow<String?> = _captureError.asStateFlow()

        fun setPickedBitmap(bitmap: Bitmap?) {
            _capturedBitmap.value = bitmap
            _captureError.value = null
        }

        fun clearCaptureError() {
            _captureError.value = null
        }

        fun startCapture(context: Context, resultCode: Int, data: Intent) {
            val intent = Intent(context, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

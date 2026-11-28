package com.example.carrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.NotificationCompat
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecorderService : Service() {
    companion object {
        const val CHANNEL_ID = "CAR_RECORDER"
        var targetFolder: File? = null
        var maxSizeBytes: Long = 10L * 1024 * 1024 * 1024
        var segmentSecond = 60
        var isRecording = false
        var currentVideoFile: File? = null
    }

    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: androidx.camera.video.Recording? = null
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var collisionSensor: CollisionSensor
    private lateinit var gpsSpeedHelper: GpsSpeedHelper

    private val chargeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            ctx ?: return
            if (!AppPref.getAutoChargeRecord(ctx)) return
            val action = intent?.action ?: return
            when(action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    if (batteryMonitor.canRecord() && !isRecording) {
                        startRecord()
                        collisionSensor.start()
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    if (isRecording) stopRecord()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        batteryMonitor = BatteryMonitor(this)
        gpsSpeedHelper = GpsSpeedHelper(this)
        gpsSpeedHelper.start()

        collisionSensor = CollisionSensor(this) {
            currentVideoFile?.let { f ->
                val crashSpeed = "%.0f".format(gpsSpeedHelper.currentSpeedKmH)
                val lockFile = File("${f.absolutePath}_crash${crashSpeed}.lock")
                lockFile.createNewFile()
            }
        }

        createNotificationChannel()
        startForeground(1, buildNotification())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(chargeReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!batteryMonitor.canRecord()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!isRecording) {
            startRecord()
            collisionSensor.start()
        }
        return START_STICKY
    }

    private fun startRecord() {
        isRecording = true
        RecorderService.isRecording = true

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.higherQualityOrLowerThan(Quality.HD)))
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        val fmt = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())
        val speedStr = "%.0f".format(gpsSpeedHelper.currentSpeedKmH)
        val fileName = "car_${fmt.format(Date())}_spd${speedStr}.mp4"
        targetFolder ?: return
        currentVideoFile = File(targetFolder, fileName)

        val outputOpt = MediaStoreOutputOptions.Builder(
            contentResolver,
            androidx.camera.video.FileOutputOptions.Builder(currentVideoFile!!).build()
        ).build()

        recording = videoCapture!!.output
            .prepareRecording(this, outputOpt)
            .withAudioEnabled()
            .start { event ->
                if (event is VideoRecordEvent.Finalize) {
                    FileManagerHelper.doCleanup(targetFolder!!, maxSizeBytes)
                    if (isRecording && batteryMonitor.canRecord()) {
                        startRecord()
                    } else {
                        stopRecord()
                    }
                }
            }
    }

    private fun stopRecord() {
        isRecording = false
        collisionSensor.stop()
        recording?.stop()
        recording = null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(chargeReceiver)
        gpsSpeedHelper.stop()
        stopRecord()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("行车记录仪录制中")
            .setContentText("GPS车速采集中")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "记录仪服务", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

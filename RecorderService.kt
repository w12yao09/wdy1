package com.example.carrecorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
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
        var maxSizeBytes: Long = 10L * 1024 * 1024 * 1024 // 默认10GB
        var segmentSecond = 60 // 默认1分钟，UI可改为60/120/180
        var isRecording = false
        var currentVideoFile: File? = null
    }
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: androidx.camera.video.Recording? = null
    private lateinit var batteryMonitor: BatteryMonitor
    private lateinit var collisionSensor: CollisionSensor

    // 充电广播接收器
    private val chargeReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            ctx ?: return
            if (!AppPref.getAutoRecOnCharge(ctx)) return
            val action = intent?.action ?: return
            when (action) {
                Intent.ACTION_POWER_CONNECTED -> {
                    if (batteryMonitor.canRecord() && !isRecording) {
                        startRecord()
                        collisionSensor.start()
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    if (isRecording) {
                        stopRecord()
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        batteryMonitor = BatteryMonitor(this)
        collisionSensor = CollisionSensor(this) {
            // 碰撞：生成lock文件锁定当前视频
            currentVideoFile?.let { f ->
                File(f.absolutePath + ".lock").createNewFile()
            }
        }
        createNotificationChannel()
        startForeground(1, buildNotification())
        // 注册充电监听
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
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.higherQualityOrLowerThan(Quality.HD)))
            .build()
        videoCapture = VideoCapture.Builder(recorder).build()
        val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val name = "car_${fmt.format(Date())}.mp4"
        targetFolder ?: return
        currentVideoFile = File(targetFolder, name)
        val output = MediaStoreOutputOptions.Builder(contentResolver, MediaStoreOutputOptions.FILE_CONTENT_URI)
            .setDestinationOptions(androidx.camera.video.FileOutputOptions.Builder(currentVideoFile!!).build())
            .build()
        recording = videoCapture!!.output.prepareRecording(this, output)
            .withAudioEnabled(true)
            .start { event ->
                if (event is VideoRecordEvent.Finalize) {
                    // 一段录完，执行容量清理
                    FileManagerHelper.doCleanup(targetFolder!!, maxSizeBytes)
                    // 自动开启下一段
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
        stopRecord()
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("行车记录仪录制中")
            .setContentText("后台录制，电量≥30%持续工作")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "记录仪", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null
}

private fun startRecord() {
    isRecording = true
    // 新增
    RecorderService.isRecording = true
    val recorder = Recorder.Builder()
        .setQualitySelector(QualitySelector.from(Quality.HD, FallbackStrategy.higherQualityOrLowerThan(Quality.HD)))
        .build()
    videoCapture = VideoCapture.Builder(recorder).build()
    val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    val name = "car_${fmt.format(Date())}.mp4"
    targetFolder ?: return
    currentVideoFile = File(targetFolder, name)
    val output = MediaStoreOutputOptions.Builder(contentResolver, MediaStoreOutputOptions.FILE_CONTENT_URI)
        .setDestinationOptions(androidx.camera.video.FileOutputOptions.Builder(currentVideoFile!!).build())
        .build()
    recording = videoCapture!!.output.prepareRecording(this, output)
        .withAudioEnabled(true)
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

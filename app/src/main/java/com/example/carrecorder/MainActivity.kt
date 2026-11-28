package com.example.carrecorder

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Rational
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import com.example.carrecorder.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cbAutoCharge.isChecked = AppPref.getAutoChargeRecord(this)
        binding.cbAutoCharge.setOnCheckedChangeListener { _, isChecked ->
            AppPref.setAutoChargeRecord(this, isChecked)
        }

        binding.rgDuration.setOnCheckedChangeListener { _, id ->
            RecorderService.segmentSecond = when(id) {
                binding.r1.id -> 60
                binding.r2.id -> 120
                binding.r3.id -> 180
                else -> 60
            }
        }

        binding.btnStart.setOnClickListener {
            val folderName = binding.etFolderName.text.toString().trim()
            val maxGbText = binding.etMaxGb.text.toString().trim()
            if (folderName.isEmpty() || maxGbText.isEmpty()) return@setOnClickListener
            val maxGb = maxGbText.toLongOrNull() ?: return@setOnClickListener
            val targetDir = FileManagerHelper.createFolder(Environment.getExternalStorageDirectory(), folderName)
            RecorderService.targetFolder = targetDir
            RecorderService.maxSizeBytes = maxGb * 1024 * 1024 * 1024
            val intent = Intent(this, RecorderService::class.java)
            startService(intent)
            startCameraPreview()
        }

        binding.btnStop.setOnClickListener {
            stopService(Intent(this, RecorderService::class.java))
            cameraProvider?.unbindAll()
        }

        binding.btnLockCurrent.setOnClickListener {
            RecorderService.currentVideoFile?.let { f ->
                val crashSpeed = "%.0f".format(GpsSpeedHelper(this).currentSpeedKmH)
                File("${f.absolutePath}_crash${crashSpeed}.lock").createNewFile()
            }
        }
    }

    private fun startCameraPreview() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            cameraProvider = future.get()
            preview = Preview.Builder().build().apply {
                setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, selector, preview)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, mainExecutor)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!RecorderService.isRecording) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16,9))
                .build()
            enterPictureInPictureMode(params)
        }
    }
}

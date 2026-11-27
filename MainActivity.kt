package com.example.carrecorder

import android.app.PictureInPictureParams
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Rational
import android.provider.Settings
import android.util.Rational
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.util.Consumer
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

        binding.cbAutoCharge.isChecked = AppPref.getAutoRecOnCharge(this)
        binding.cbAutoCharge.setOnCheckedChangeListener { _, isChecked ->
            AppPref.setAutoRecOnCharge(this, isChecked)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
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
            val rootDir = Environment.getExternalStorageDirectory()
            val targetDir = FileManagerHelper.createFolder(rootDir, folderName)
            RecorderService.targetFolder = targetDir
            RecorderService.maxSizeBytes = maxGb * 1024 * 1024 * 1024
            val srv = Intent(this, RecorderService::class.java)
            startService(srv)
            startCameraPreview()
        }
        binding.btnStop.setOnClickListener {
            stopService(Intent(this, RecorderService::class.java))
            cameraProvider?.unbindAll()
        }
        binding.btnLockCurrent.setOnClickListener {
            RecorderService.currentVideoFile?.let { f ->
                File(f.absolutePath + ".lock").createNewFile()
            }
        }
    }

    private fun startCameraPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(this, cameraSelector, preview)
            } catch(exc: Exception) {
                exc.printStackTrace()
            }
        }, android.os.HandlerExecutor(mainLooper))
    }

    // 按下Home，进入画中画
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (!RecorderService.isRecording) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pipBuilder = PictureInPictureParams.Builder()
            pipBuilder.setAspectRatio(Rational(16,9))
            enterPictureInPictureMode(pipBuilder.build())
        }
    }
}

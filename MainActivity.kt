package com.example.carrecorder

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.carrecorder.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 加载保存的充电自动录制开关状态
        binding.cbAutoCharge.isChecked = AppPref.getAutoRecOnCharge(this)
        binding.cbAutoCharge.setOnCheckedChangeListener { _, isChecked ->
            AppPref.setAutoRecOnCharge(this, isChecked)
        }

        // 申请全部文件权限
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
        }
        binding.btnStop.setOnClickListener {
            stopService(Intent(this, RecorderService::class.java))
        }
        binding.btnLockCurrent.setOnClickListener {
            RecorderService.currentVideoFile?.let { f ->
                File(f.absolutePath + ".lock").createNewFile()
            }
        }
    }
}

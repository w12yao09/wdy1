package com.example.carrecorder

import java.io.File
import java.util.Arrays

object FileManagerHelper {
    fun createFolder(root: File, folderName: String): File {
        val dir = File(root, folderName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun doCleanup(folder: File, maxTotalBytes: Long) {
        val allFiles = folder.listFiles() ?: return
        val videoList = allFiles.filter { it.name.endsWith(".mp4") }.sortedBy { it.lastModified() }
        var totalSize = allFiles.sumOf { it.length() }

        for (video in videoList) {
            if (totalSize <= maxTotalBytes) break
            val lockFile = File(video.absolutePath + ".lock")
            val lockFile2 = File(video.absolutePath + "_crash")
            if (lockFile.exists() || allFiles.any { it.name.startsWith(video.name) && it.name.contains("_crash") }) {
                continue
            }
            totalSize -= video.length()
            video.delete()
        }
    }
}

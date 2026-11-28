package com.example.carrecorder

import java.io.File
import java.nio.file.Files
import java.util.*

object FileManagerHelper {
    // 扫描目标文件夹，统计总大小，超上限删除最早未锁定视频
    fun doCleanup(targetFolder: File, maxSizeBytes: Long) {
        if (!targetFolder.exists()) return
        val allMp4 = targetFolder.listFiles { f -> f.name.endsWith(".mp4") } ?: return
        val videoList = allMp4.map { videoFile ->
            val lockFile = File(videoFile.absolutePath + ".lock")
            VideoItem(
                file = videoFile,
                lockFile = lockFile,
                isLocked = lockFile.exists(),
                createTime = videoFile.lastModified()
            )
        }
        var totalSize = videoList.sumOf { it.file.length() }
        // 只清理未锁定的，按时间升序（最早的先删）
        val toDelete = videoList.filter { !it.isLocked }.sortedBy { it.createTime }
        for (item in toDelete) {
            if (totalSize <= maxSizeBytes) break
            val delSize = item.file.length()
            item.file.delete()
            if(item.lockFile.exists()) item.lockFile.delete()
            totalSize -= delSize
        }
    }

    fun createFolder(parent: File, folderName: String): File {
        val dir = File(parent, folderName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}

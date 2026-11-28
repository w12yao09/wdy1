package com.example.carrecorder
import java.io.File

data class VideoItem(
    val file: File,
    val lockFile: File,
    val isLocked: Boolean,
    val createTime: Long
)

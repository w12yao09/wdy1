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

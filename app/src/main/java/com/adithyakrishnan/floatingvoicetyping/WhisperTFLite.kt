package com.adithyakrishnan.floatingvoicetyping

import android.content.Context
import android.util.Log
import org.tensorflow.lite.support.audio.TensorAudio
import org.tensorflow.lite.task.audio.classifier.AudioClassifier
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer

class WhisperTFLite(context: Context, modelName: String = "whisper-tiny-en-int8.tflite") {

    private val classifier: AudioClassifier
    private val tensor: TensorAudio
    private val sampleRate: Int
    private val audioFormat: TensorAudio.TensorAudioFormat

    companion object {
        private const val TAG = "WhisperTFLite"
        const val TINY_EN = "whisper-tiny-en-int8.tflite"
        const val SMALL_EN = "whisper-small-en-int8.tflite"
    }

    init {
        // Get model file from assets
        val modelFile = getModelFile(context, modelName)

        val options = AudioClassifier.AudioClassifierOptions.builder()
            .setMaxResults(1)
            .setScoreThreshold(0.3f)
            .build()

        // Create classifier using file path
        classifier = AudioClassifier.createFromFileAndOptions(
            context,
            modelFile.absolutePath,  // Use absolute path
            options
        )

        // Create tensor with proper format
        audioFormat = classifier.requiredTensorAudioFormat
        tensor = classifier.createInputTensorAudio()
        sampleRate = audioFormat.sampleRate

        Log.d(TAG, "Initialized Whisper: $modelName")
        Log.d(TAG, "Sample rate: $sampleRate, Channels: ${audioFormat.channels}")
    }

    fun transcribe(audioBuffer: ShortArray): String {
        try {
            // Convert to float array normalized between -1.0 and 1.0
            val floatBuffer = FloatArray(audioBuffer.size) {
                audioBuffer[it] / 32768.0f
            }

            // Load into tensor with proper format
            tensor.load(floatBuffer, audioFormat)

            // Run inference
            val output = classifier.classify(tensor)

            // Return best transcription
            return if (output.isNotEmpty() && output[0].categories.isNotEmpty()) {
                output[0].categories[0].label
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Transcription failed", e)
            return ""
        }
    }

    fun getSampleRate(): Int = sampleRate

    fun getAudioFormat(): TensorAudio.TensorAudioFormat = audioFormat

    private fun getModelFile(context: Context, modelName: String): File {
        val cacheFile = File(context.cacheDir, modelName)

        // Return if already exists
        if (cacheFile.exists()) return cacheFile

        // Copy from assets
        context.assets.open(modelName).use { inputStream ->
            FileOutputStream(cacheFile).use { outputStream ->
                val buffer = ByteArray(1024 * 1024)  // 1MB buffer for faster copy
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
        }
        Log.d(TAG, "Copied model to cache: ${cacheFile.absolutePath}")
        return cacheFile
    }
}
package com.adithyakrishnan.floatingvoicetyping

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.NetworkType
import androidx.work.Constraints
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.FileOutputStream

class DownloadWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val modelUrl = inputData.getString("model_url") ?: return Result.failure()
        val filename = inputData.getString("filename") ?: return Result.failure()

        return try {
            val modelFile = ModelRepository.getModelPath(applicationContext, filename)
            modelFile.parentFile?.mkdirs()

            val request = Request.Builder().url(modelUrl).build()
            val response = OkHttpClient().newCall(request).execute()

            response.body?.byteStream()?.use { input ->
                FileOutputStream(modelFile).use { output ->
                    input.copyTo(output)
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        fun enqueueDownload(context: Context, model: ModelInfo) {
            val data = Data.Builder()
                .putString("model_url", model.url)
                .putString("filename", model.filename)
                .build()

            val request = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(data)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
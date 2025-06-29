// ModelRepository.kt
import android.content.Context
import java.io.File
object ModelRepository {
    val models = listOf(
        ModelInfo(
            name = "Tiny (English)",
            sizeMB = 75,
            url = "https://huggingface.co/DocWolle/whisper_tflite_models/resolve/main/whisper-tiny-en-int8.tflite",
            filename = "tiny_en.tflite"
        ),
        ModelInfo(
            name = "Base (English)",
            sizeMB = 140,
            url = "https://huggingface.co/DocWolle/whisper_tflite_models/resolve/main/whisper-base-en-int8.tflite",
            filename = "base_en.tflite"
        ),
        ModelInfo(
            name = "Small (English)",
            sizeMB = 460,
            url = "https://huggingface.co/DocWolle/whisper_tflite_models/resolve/main/whisper-small-en-int8.tflite",
            filename = "small_en.tflite"
        ),
        ModelInfo(
            name = "Tiny (Multilingual)",
            sizeMB = 75,
            url = "https://huggingface.co/DocWolle/whisper_tflite_models/resolve/main/whisper-tiny-int8.tflite",
            filename = "tiny_multi.tflite"
        )
    )

    fun getModelPath(context: Context, filename: String): File {
        return File(context.filesDir, "models/$filename")
    }
}
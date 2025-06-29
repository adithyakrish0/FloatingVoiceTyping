// ModelSelectionActivity.kt
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import android.app.AlertDialog
import android.content.Intent
import android.widget.Toast
class ModelSelectionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_model_selection)

        val adapter = ModelAdapter(ModelRepository.models) { model ->
            if (isModelDownloaded(model.filename)) {
                activateModel(model.filename)
            } else {
                showDownloadDialog(model)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun isModelDownloaded(filename: String): Boolean {
        return ModelRepository.getModelPath(this, filename).exists()
    }

    private fun activateModel(filename: String) {
        Prefs.setActiveModel(this, filename)
        startService(Intent(this, FloatingWindowService::class.java))
        finish()
    }

    private fun showDownloadDialog(model: ModelInfo) {
        AlertDialog.Builder(this)
            .setTitle("Download Model")
            .setMessage("Download ${model.name} (${model.sizeMB}MB)?")
            .setPositiveButton("Download") { _, _ ->
                startDownload(model)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startDownload(model: ModelInfo) {
        DownloadWorker.enqueueDownload(this, model)
        Toast.makeText(this, "Downloading ${model.name}...", Toast.LENGTH_SHORT).show()
    }
}
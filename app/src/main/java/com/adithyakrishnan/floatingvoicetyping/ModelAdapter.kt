import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.graphics.Color
import android.view.LayoutInflater
class ModelAdapter(
    private val models: List<ModelInfo>,
    private val onModelSelected: (ModelInfo) -> Unit
) : RecyclerView.Adapter<ModelAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.modelName)
        val size: TextView = view.findViewById(R.id.modelSize)
        val status: TextView = view.findViewById(R.id.modelStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_model, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model = models[position]
        holder.name.text = model.name
        holder.size.text = "Size: ${model.sizeMB}MB"

        val context = holder.itemView.context
        val exists = ModelRepository.getModelPath(context, model.filename).exists()
        holder.status.text = if (exists) "Downloaded" else "Tap to download"
        holder.status.setTextColor(
            if (exists) Color.parseColor("#4CAF50") else Color.parseColor("#FF9800")
        )

        holder.itemView.setOnClickListener {
            onModelSelected(model)
        }
    }

    override fun getItemCount() = models.size
}
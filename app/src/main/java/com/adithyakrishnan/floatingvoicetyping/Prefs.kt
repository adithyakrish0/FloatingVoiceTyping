import android.content.Context
import androidx.core.content.edit

// Update setActiveModel to:

object Prefs {
    private const val PREFS_NAME = "whisper_prefs"
    private const val KEY_ACTIVE_MODEL = "active_model"

    fun setActiveModel(context: Context, filename: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_ACTIVE_MODEL, filename)
        }
    }

    fun getActiveModel(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVE_MODEL, "") ?: ""
    }
}
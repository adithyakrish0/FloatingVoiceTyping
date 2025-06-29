package com.adithyakrishnan.floatingvoicetyping

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if any model is selected
        if (Prefs.getActiveModel(this).isNullOrEmpty()) {
            // Show model selection if no model chosen
            startActivity(Intent(this, ModelSelectionActivity::class.java))
        } else {
            // Start floating service directly
            startService(Intent(this, FloatingWindowService::class.java))
        }
        finish()
    }
}
package com.adithyakrishnan.floatingvoicetyping

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var whisper: WhisperTFLite
    private var recorder: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private val handler = Handler(Looper.getMainLooper())
    private val channelId = "voice_service_channel"

    override fun onCreate() {
        super.onCreate()

        // Start as foreground service first
        startForegroundServiceWithNotification()

        try {
            // Initialize our TFLite implementation
            whisper = WhisperTFLite(this, "whisper-tiny-en-int8.tflite")

            // Create floating window
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            floatingView = LayoutInflater.from(this).inflate(R.layout.floating_window, null)

            params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
                )
            } else {
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                    PixelFormat.TRANSLUCENT
                )
            }.apply {
                gravity = Gravity.TOP or Gravity.START
                x = 0
                y = 100
            }

            windowManager.addView(floatingView, params)
            setupClickListeners()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to initialize voice service: ${e.message}", Toast.LENGTH_LONG).show()
            stopSelf()
        }
    }

    private fun startForegroundServiceWithNotification() {
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Voice Typing Service"
            val importance = NotificationManager.IMPORTANCE_LOW

            // Create notification channel
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Background service for voice typing"
                setSound(null, null) // No sound
                enableVibration(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification using system icon
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Voice Typing Active")
            .setContentText("Tap mic to start voice input")
            // TEMPORARY WORKAROUND: Use system-provided microphone icon
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build()

        // Start service in foreground
        startForeground(1, notification)
    }

    private fun setupClickListeners() {
        val micButton = floatingView.findViewById<ImageButton>(R.id.micButton)
        micButton.setOnClickListener {
            if (!isRecording) {
                startRecording()
                micButton.setColorFilter(0xFFFF0000.toInt()) // Red when recording
            } else {
                stopRecording()
                micButton.clearColorFilter() // Reset color
            }
        }

        // Make window draggable
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun startRecording() {
        if (isRecording) return

        isRecording = true
        val sampleRate = whisper.getSampleRate()
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        recorder = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Audio recording failed: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }

        recorder?.startRecording()

        recordingThread = Thread {
            val audioBuffer = ShortArray(bufferSize)
            while (isRecording) {
                val read = recorder?.read(audioBuffer, 0, bufferSize) ?: 0
                if (read > 0) {
                    try {
                        val transcription = whisper.transcribe(audioBuffer.copyOf(read))
                        handler.post {
                            updateTranscription(transcription)
                        }
                    } catch (e: Exception) {
                        handler.post {
                            Toast.makeText(this@FloatingWindowService,
                                "Transcription error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }.also { it.start() }
    }

    private fun stopRecording() {
        isRecording = false
        recorder?.stop()
        recorder?.release()
        recorder = null
        recordingThread?.join(500) // Wait for thread to finish
        recordingThread = null
    }

    private fun updateTranscription(text: String) {
        if (text.isNotBlank()) {
            injectText(text)
        }
    }

    private fun injectText(text: String) {
        // Send text to TextInjectionService via LocalBroadcast
        val intent = Intent("INJECT_TEXT_ACTION").apply {
            putExtra("text", text)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        recorder?.release()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
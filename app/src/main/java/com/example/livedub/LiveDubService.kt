package com.example.livedub

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

class LiveDubService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "LiveDubChannel"
    }

    private lateinit var overlayManager: OverlayManager
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var tts: TextToSpeech? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        overlayManager = OverlayManager(this) { stopSelf() }
        tts = TextToSpeech(this, this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
            val resultData = intent.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)

            if (resultCode != -1 && resultData != null) {
                startForeground(NOTIFICATION_ID, createNotification())
                startProjection(resultCode, resultData)
                overlayManager.showOverlay()
            }
        } else if (intent?.action == ACTION_STOP) {
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun startProjection(resultCode: Int, data: Intent) {
        val mpManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = mpManager.getMediaProjection(resultCode, data)
        
        startAudioCapture()
    }

    private fun startAudioCapture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(16000) // 16kHz for Speech Recognition
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .build()

            try {
                audioRecord = AudioRecord.Builder()
                    .setAudioPlaybackCaptureConfig(config)
                    .setAudioFormat(audioFormat)
                    .build()

                audioRecord?.startRecording()
                isRecording = true
                
                serviceScope.launch {
                    processAudioLoop()
                }
            } catch (e: SecurityException) {
                Log.e("LiveDub", "Permission missing for audio capture", e)
            }
        }
    }

    private fun processAudioLoop() {
        val bufferSize = 1024
        val buffer = ShortArray(bufferSize)

        while (isRecording) {
            val readResult = audioRecord?.read(buffer, 0, bufferSize) ?: 0
            if (readResult > 0) {
                // Here is where you would feed the buffer to a local STT model (like Whisper-tiny)
                // Since we don't have the model files, we simulate "activity" by checking amplitude
                // and triggering a mock translation occasionally for demonstration.
                
                // TODO: Integrate Whisper TFLite or Google Cloud Speech gRPC here
                // pseudo: sttEngine.feed(buffer)
                
                // Demo: If loud enough, speak something
                // (Commented out to avoid spamming TTS in demo)
                // speakTranslatedText("Simulated translation") 
            }
        }
    }

    private fun speakTranslatedText(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "id1")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale("ru")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        mediaProjection?.stop()
        overlayManager.removeOverlay()
        tts?.shutdown()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LiveDub Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val stopIntent = Intent(this, LiveDubService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE)

        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("LiveDub Active")
            .setContentText("Translating audio in real-time...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure you have this or use android default
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .build()
    }
}

package com.example.livedub

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.guolindev.permissionx.PermissionX

class MainActivity : AppCompatActivity() {

    private lateinit var mediaProjectionManager: MediaProjectionManager

    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            startLiveDubService(result.resultCode, result.data!!)
        } else {
            Toast.makeText(this, "Screen Capture Permission Denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            checkPermissionsAndStart()
        }
    }

    private fun checkPermissionsAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, "Please grant Overlay permission", Toast.LENGTH_LONG).show()
            return
        }

        PermissionX.init(this)
            .permissions(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.FOREGROUND_SERVICE
            )
            .request { allGranted: Boolean, _, _ ->
                if (allGranted) {
                    requestScreenCapture()
                } else {
                    Toast.makeText(this, "Audio permission is required", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun requestScreenCapture() {
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)
    }

    private fun startLiveDubService(resultCode: Int, data: Intent) {
        val serviceIntent = Intent(this, LiveDubService::class.java).apply {
            action = LiveDubService.ACTION_START
            putExtra(LiveDubService.EXTRA_RESULT_CODE, resultCode)
            putExtra(LiveDubService.EXTRA_RESULT_DATA, data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        finish() // Close activity, service runs in background
    }
}

package com.letianpai.robot.taskservice

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.letianpai.robot.control.service.DispatchService
import android.provider.Settings
import android.net.Uri
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val decorView = window.decorView
        val uiOptions =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN
        decorView.systemUiVisibility = uiOptions
        setContentView(R.layout.activity_main)
        checkAndRequestWriteSettingsPermission(this)
    }


    fun checkAndRequestWriteSettingsPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:${context.packageName}")
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                Toast.makeText(context, "Please, give this permission to allow the app to modify the settings", Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(this, DispatchService::class.java)
                startService(intent)
            }
        }
    }
}
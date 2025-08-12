package com.example.wath_test.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.ActionListener
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.wath_test.R
import com.example.wath_test.presentation.SensorService
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.io.IOException
import java.net.Socket

class StartSensorActivity : AppCompatActivity() {

    private lateinit var startButton: Button
    private lateinit var timerTextView: TextView
    private lateinit var modeTextview: TextView
    private lateinit var trialTextview: TextView

    private lateinit var settingSharedPreferences: SharedPreferences
    private lateinit var modeSharedPreferences: SharedPreferences

    private lateinit var timerReceiver: BroadcastReceiver
    private lateinit var trialUpdateReceiver: BroadcastReceiver
    private lateinit var serviceStoppedReceiver: BroadcastReceiver

    private var duration: Int = -1
    private var trial: Int = 1
    private var isServiceStarted = false

    private var parId: String = ""
    private var modeName: String = ""

    companion object {private const val PERMISSION_REQUEST_CODE = 1001}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_sensor)

        settingSharedPreferences = getSharedPreferences("collect_sensor_setting", MODE_PRIVATE)
        modeSharedPreferences = getSharedPreferences("collect_mode_setting", MODE_PRIVATE)

        duration = settingSharedPreferences.getString("duration", "1")?.toInt() ?: 1
        modeName = modeSharedPreferences.getString("mode", "Toothbrushing")?: "Toothbrushing"
        parId = modeSharedPreferences.getString("par_id", "1")?: "1"
        trial = modeSharedPreferences.getString("${parId}_${modeName}", "1")?.toInt() ?: 1
        startButton = findViewById(R.id.button_start)
        timerTextView = findViewById(R.id.text_timer)
        modeTextview = findViewById(R.id.text_mode)
        trialTextview = findViewById(R.id.text_trial)

        modeTextview.text = "${modeName}"
        trialTextview.text = "Trial: $trial"

        startButton.setOnClickListener {
            if (checkPermissions()) {
                startService(duration)
            }
        }

        timerReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val millisUntilFinished = intent?.getLongExtra("millisUntilFinished", 0L) ?: 0L
                updateTimerTextView(millisUntilFinished)
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(timerReceiver, IntentFilter("TIMER_UPDATE"))

        trialUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val updatedTrial = intent?.getIntExtra("updatedTrial", trial) ?: trial
                if (updatedTrial != trial) {
                    trial = updatedTrial
                    Log.e("Trial", "Trial receiver: $trial")
                    trialTextview.text = "Trial: $trial"
                }
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(trialUpdateReceiver, IntentFilter("TRIAL_UPDATED"))
        serviceStoppedReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                isServiceStarted = false // 서비스가 중지 되었음을 표시
            }
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceStoppedReceiver, IntentFilter("SERVICE_STOPPED"))
    }

    private fun updateTimerTextView(millisUntilFinished: Long) {
        val hours = (millisUntilFinished / (1000 * 60 * 60)) % 24
        val minutes = (millisUntilFinished / (1000 * 60)) % 60
        val seconds = (millisUntilFinished / 1000) % 60
        timerTextView.text = String.format("%02dh:%02dm:%02ds", hours, minutes, seconds)
    }

    private fun checkPermissions(): Boolean {
        val requiredPermissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            android.Manifest.permission.RECORD_AUDIO,
        )

        val missingPermissions = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            return false
        }
        return true
    }

    private fun startService(duration: Int) {
        if (!isServiceStarted) {
            isServiceStarted = true
            val serviceIntent = Intent(this, SensorService::class.java)
            serviceIntent.putExtra("duration", duration)
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            Toast.makeText(this, "이미 서비스가 시작되었습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                var settingSharedPreferences = getSharedPreferences("collect_sensor_setting", MODE_PRIVATE)
                val duration = settingSharedPreferences.getString("duration", "1")?.toInt() ?: 1
                startService(duration)
            } else {
                AlertDialog.Builder(this)
                    .setTitle("권한 필요")
                    .setMessage("필수 권한이 거부되었습니다. 앱을 사용할 수 없습니다.")
                    .setPositiveButton("확인") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                    .show()
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(timerReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(trialUpdateReceiver)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceStoppedReceiver)

    }
}

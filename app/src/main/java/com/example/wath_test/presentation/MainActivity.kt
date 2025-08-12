/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.wath_test.presentation

import android.content.ActivityNotFoundException
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.ContextCompat.startActivity
import com.example.wath_test.presentation.CollectSensorActivity
import com.example.wath_test.presentation.SettingSensorActivity
import com.example.wath_test.presentation.StartSensorActivity
import com.example.wath_test.R

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File

class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private const val REQUEST_CODE_BACKGROUND_LOCATION = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val settingButton: Button = findViewById(R.id.setting_sensor)
        val startButton: Button = findViewById(R.id.start_collect)
        val modeButton: Button = findViewById(R.id.mode_setting)

        // 권한 요청
        // Android 11(API 수준 30) 이상에서는 시스템 대화상자에 항상 허용 옵션이 포함되지 않는다.
        // 대신 사용자는 설정 페이지에서 백그라운드 위치를 사용 설정해야 한다.

        requestPermissions()
        requestDisableBatteryOptimization()

        settingButton.setOnClickListener {
            val intent = Intent(this, SettingSensorActivity::class.java)
            startActivity(intent)
        }
        startButton.setOnClickListener {
            val intent = Intent(this, StartSensorActivity::class.java)
            startActivity(intent)
        }
        modeButton.setOnClickListener {
            val intent = Intent(this, SettingModeActivity::class.java)
            startActivity(intent)
        }
    }

    // 권한 요청 메서드
    private fun requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
            ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.RECORD_AUDIO,
                ),
                REQUEST_CODE_PERMISSIONS
            )
        } else if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 여기서는 백그라운드 위치를 받아야 하는데, 앞에서 foreground location을 받은 후에 받을 수 있음
            requestBackgroundLocationPermission()
        }
    }

    private fun requestBackgroundLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION),
            REQUEST_CODE_BACKGROUND_LOCATION
        )
    }

    // 권한 요청 결과 처리
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            val deniedPermissions = permissions.filterIndexed { index, _ ->
                grantResults[index] != PackageManager.PERMISSION_GRANTED
            }
            if (deniedPermissions.isEmpty()) {
                if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    requestBackgroundLocationPermission()
                }
            } else {
                Toast.makeText(this, "모든 권한을 허용해야 합니다.", Toast.LENGTH_SHORT).show()
                requestPermissions()
            }
        }
    }
    // 배터리 최적화 무시할 수 있는 요청
    private fun requestDisableBatteryOptimization() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = packageName

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:$packageName"))
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                e.printStackTrace()
                Toast.makeText(this, "Unable to open battery optimization settings", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


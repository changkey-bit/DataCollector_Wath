package com.example.wath_test.presentation

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.wath_test.R

class SettingSensorActivity : AppCompatActivity() {

    private lateinit var IMUhzEditText: EditText
    private lateinit var GPShzEditText: EditText
    private lateinit var DurationEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_sensor)

        // Switch 뷰와 연결
        IMUhzEditText = findViewById(R.id.edit_text_imu_hz)
        GPShzEditText = findViewById(R.id.edit_text_location_hz)
        DurationEditText = findViewById(R.id.edit_text_duration)

        // SharedPreferences 초기화
        val sharedPreferences = getSharedPreferences("collect_sensor_setting", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Switch 상태를 저장하고 불러오기 위한 설정
        IMUhzEditText.setText(sharedPreferences.getString("imu_hz", "50"))
        GPShzEditText.setText(sharedPreferences.getString("gps_hz", "5"))
        DurationEditText.setText(sharedPreferences.getString("duration", "40"))

        editor.putString("imu_hz", sharedPreferences.getString("imu_hz", "50"))
        editor.putString("gps_hz", sharedPreferences.getString("gps_hz", "5"))
        editor.putString("duration", sharedPreferences.getString("duration", "40"))
        editor.apply()

        IMUhzEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 필요 시 구현
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 필요 시 구현
            }

            override fun afterTextChanged(s: Editable?) {
                editor.putString("imu_hz", s.toString())
                editor.apply()
            }
        })

        GPShzEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 필요 시 구현
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 필요 시 구현
            }

            override fun afterTextChanged(s: Editable?) {
                editor.putString("gps_hz", s.toString())
                editor.apply()
            }
        })

        DurationEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 필요 시 구현
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 필요 시 구현
            }

            override fun afterTextChanged(s: Editable?) {
                editor.putString("duration", s.toString())
                editor.apply()
            }
        })
    }
}
package com.example.wath_test.presentation

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.wath_test.R

class SettingModeActivity : AppCompatActivity() {

    private lateinit var IDEditText: EditText
    private lateinit var modeSpinner: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting_mode)

        IDEditText = findViewById(R.id.edit_text_id)
        modeSpinner = findViewById(R.id.spinner_mode)

        // SharedPreferences 초기화
        val sharedPreferences = getSharedPreferences("collect_mode_setting", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // Switch 상태를 저장하고 불러오기 위한 설정
        IDEditText.setText(sharedPreferences.getString("par_id", "1"))

        editor.putString("par_id", sharedPreferences.getString("par_id", "1"))
        editor.apply()

        IDEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                editor.putString("par_id", s.toString())
                editor.apply()
            }
        })

        // Spinner 항목 설정
        val modes = listOf("Tooth_brushing", "Washing_hands", "Vacuum_Cleaner", "Wiping", "Shower", "Brushing", "Other")
        val adapter = ArrayAdapter(this, R.layout.spinner_mode, modes)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        modeSpinner.adapter = adapter

        // Spinner 값 불러오기
        val savedMode = sharedPreferences.getString("mode", modes[0]) // 기본값은 첫 번째 항목
        val savedModeIndex = modes.indexOf(savedMode)
        if (savedModeIndex != -1) {
            modeSpinner.setSelection(savedModeIndex) // 저장된 모드 선택
        }

        // Spinner 값 저장
        modeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedMode = modes[position]
                editor.putString("mode", selectedMode) // 선택된 모드 저장
                editor.apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 아무 항목도 선택되지 않았을 때 처리 (필요시 구현)
            }
        }
    }
}
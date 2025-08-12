package com.example.wath_test.presentation

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.example.wath_test.R

class CollectSensorActivity : AppCompatActivity() {

    /*
    초기 상태를 null로 하지 말고, lateinit lazy를 통한 늦은 초기화를 한다.
    lateinit은 계속하여 값이 변할 수 있다는 속성을 위해 무조건 var을 사용한다. -> lateinit var
    lazy는 by lazy 구문을 통해 앞선 값이 초기화가 되면 해당 값을 초기화 할 수 있다. 단 한 번의 늦은 초기화가 이루어지고 이후에는 값이 불변한다.(val)
    by lazy는 초기화 이후에 읽기 전용 값으로 사용할 때 사용
    */

    private lateinit var accelerometerSwitch: Switch
    private lateinit var gyroscopeSwitch: Switch
    private lateinit var magnetometerSwitch: Switch
    private lateinit var gpsSwitch: Switch
    private lateinit var gravitySwitch: Switch
    private lateinit var rotationVectorSwitch: Switch
    private lateinit var significantMotionSwitch: Switch
    private lateinit var stepCounterSwitch: Switch
    private lateinit var stepDetectorSwitch: Switch
    private lateinit var proximitySwitch: Switch
    private lateinit var ambientTemperatureSwitch: Switch
    private lateinit var lightSwitch: Switch
    private lateinit var pressureSwitch: Switch
    private lateinit var relativeHumiditySwitch: Switch
    private lateinit var voiceSwitch: Switch

    private val onColor = Color.parseColor("#00FA9A")
    private val offColor = Color.GRAY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collect_sensor)

        // Switch 뷰와 연결
        accelerometerSwitch = findViewById(R.id.switch_accelerometer)
        gyroscopeSwitch = findViewById(R.id.switch_gyroscope)
        magnetometerSwitch = findViewById(R.id.switch_magnetometer)
        gravitySwitch = findViewById(R.id.switch_gravity)
        rotationVectorSwitch = findViewById(R.id.switch_rotation_vector)
        significantMotionSwitch = findViewById(R.id.switch_significant_motion)
        stepCounterSwitch = findViewById(R.id.switch_step_counter)
        stepDetectorSwitch = findViewById(R.id.switch_step_detector)
        proximitySwitch = findViewById(R.id.switch_proximity)
        ambientTemperatureSwitch = findViewById(R.id.switch_ambient_temperature)
        lightSwitch = findViewById(R.id.switch_light)
        pressureSwitch = findViewById(R.id.switch_pressure)
        relativeHumiditySwitch = findViewById(R.id.switch_relative_humidity)
        gpsSwitch = findViewById(R.id.switch_gps)
        voiceSwitch = findViewById(R.id.switch_voice)

        // Singleton: 프로그램 시작과 종료까지 클래스의 인스턴스를 단 한번만 생성하여 사용하는 패턴. 코틀린은 companion, object 키워드로 싱클톤을 구현
        // SharedPreferences 초기화
        val sharedPreferences = getSharedPreferences("collect_sensor_names", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        // SharedPreferences에서 Switch 상태를 불러와서 이전 값을 유지한다. 기본 값은 true
        accelerometerSwitch.isChecked = sharedPreferences.getBoolean("accelerometer", true)
        gyroscopeSwitch.isChecked = sharedPreferences.getBoolean("gyroscope", true)
        magnetometerSwitch.isChecked = sharedPreferences.getBoolean("magnetometer", true)
        gravitySwitch.isChecked = sharedPreferences.getBoolean("gravity", true)
        rotationVectorSwitch.isChecked = sharedPreferences.getBoolean("rotation_vector", true)
        significantMotionSwitch.isChecked = sharedPreferences.getBoolean("significant_motion", true)
        stepCounterSwitch.isChecked = sharedPreferences.getBoolean("step_counter", true)
        stepDetectorSwitch.isChecked = sharedPreferences.getBoolean("step_detector", true)
        proximitySwitch.isChecked = sharedPreferences.getBoolean("proximity", true)
        ambientTemperatureSwitch.isChecked = sharedPreferences.getBoolean("ambient_temperature", true)
        lightSwitch.isChecked = sharedPreferences.getBoolean("light", true)
        pressureSwitch.isChecked = sharedPreferences.getBoolean("pressure", true)
        relativeHumiditySwitch.isChecked = sharedPreferences.getBoolean("relative_humidity", true)

        gpsSwitch.isChecked = sharedPreferences.getBoolean("gps", true)
        voiceSwitch.isChecked = sharedPreferences.getBoolean("voice", true)

        // Default 값이 true라서 초기 색상도 그린 색상으로 설정.
        setSwitchColor(accelerometerSwitch)
        setSwitchColor(gyroscopeSwitch)
        setSwitchColor(magnetometerSwitch)
        setSwitchColor(gravitySwitch)
        setSwitchColor(rotationVectorSwitch)
        setSwitchColor(significantMotionSwitch)
        setSwitchColor(stepCounterSwitch)
        setSwitchColor(stepDetectorSwitch)
        setSwitchColor(proximitySwitch)
        setSwitchColor(ambientTemperatureSwitch)
        setSwitchColor(lightSwitch)
        setSwitchColor(pressureSwitch)
        setSwitchColor(relativeHumiditySwitch)
        setSwitchColor(gpsSwitch)
        setSwitchColor(voiceSwitch)

        // 각 Switch의 상태가 변경될 때 SharedPreferences에 저장
        val switches = listOf(
            accelerometerSwitch to "accelerometer",
            gyroscopeSwitch to "gyroscope",
            magnetometerSwitch to "magnetometer",
            gravitySwitch to "gravity",
            rotationVectorSwitch to "rotation_vector",
            significantMotionSwitch to "significant_motion",
            stepCounterSwitch to "step_counter",
            stepDetectorSwitch to "step_detector",
            proximitySwitch to "proximity",
            ambientTemperatureSwitch to "ambient_temperature",
            lightSwitch to "light",
            pressureSwitch to "pressure",
            relativeHumiditySwitch to "relative_humidity",
            gpsSwitch to "gps",
            voiceSwitch to "voice"
        )

        for ((switch, key) in switches) {
            switch.setOnCheckedChangeListener { _, isChecked ->
                setSwitchColor(switch)
                editor.putBoolean(key, isChecked)
                editor.apply()
            }
        }
    }

    private fun setSwitchColor(switch: Switch) {
        if (switch.isChecked) {
            switch.thumbDrawable.setTint(onColor)
        } else {
            switch.thumbDrawable.setTint(offColor)
        }
    }
}


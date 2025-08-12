package com.example.wath_test.presentation

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.VibrationEffect.EFFECT_HEAVY_CLICK
import android.os.Vibrator
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.wath_test.R
import com.example.wath_test.presentation.StartSensorActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationListener
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.wearable.DataClient
//import com.google.android.gms.location.R
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Calendar
import java.util.jar.Manifest
import android.net.Uri
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.Button
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter

class SensorService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager

    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    private var rotationVector: Sensor? = null

    @Volatile private var latestAccel = FloatArray(3) { 0f }
    @Volatile private var latestGyro = FloatArray(3) { 0f }
    @Volatile private var latestRotVec = FloatArray(3) { 0f }

    // CSV 파일 및 BufferedWriter (IMU, 오디오)
    private lateinit var imuWriter: BufferedWriter
    private lateinit var audioWriter: BufferedWriter

    // 로그 버퍼 (여러 샘플을 모아서 한 번에 기록)
    private val imuLogBuffer = StringBuilder()
    private val audioLogBuffer = StringBuilder()

    private var imuFileName: String = ""
    private var audioFileName: String = ""

    private val baseFolder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "HCILab_Data")
    private lateinit var participantFolder: File
    private var parId: String = ""
    private var modeName: String = ""
    private var trial: Int = 1

    private var startTime: Long = 0L

    private var isInitialized = false

    private val notificationChannelId = "SENSOR_SERVICE_CHANNEL"
    private val notificationId = 1

    private lateinit var settingSharedPreferences: SharedPreferences
    private lateinit var modeSharedPreferences: SharedPreferences

    private var imuHz: Float = 1f

    private var timer: CountDownTimer? = null
    private var durationInMillis: Long = 0
    private var isRecording = false

    private lateinit var audioRecord: AudioRecord
    private val sampleRateInHz = 16000
    private var audioChannel = AudioFormat.CHANNEL_IN_MONO
    private var audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var bufferSize = 0

    // 서비스 시작 시간 기록 (로그 타임스탬프용)
    private var serviceStartTime = 0L

    // IMU 데이터 수집 관련 코루틴 Job
    private var imuJob: Job? = null
    // 오디오 데이터 수집 관련 코루틴 Job
    private var audioJob: Job? = null

    // IMU 데이터 수집 주기 (20ms)
    private val samplingInterval = 20L

    override fun onCreate() {
        super.onCreate()
        Log.d("Service", "onCreate 호출됨")

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        settingSharedPreferences = getSharedPreferences("collect_sensor_setting", Context.MODE_PRIVATE)
        modeSharedPreferences = getSharedPreferences("collect_mode_setting", Context.MODE_PRIVATE)

        imuHz = settingSharedPreferences.getString("imu_hz", "50")?.toFloat() ?: 50f
        parId = modeSharedPreferences.getString("par_id", "1")?: "1"
        modeName = modeSharedPreferences.getString("mode", "Toothbrushing")?: "Toothbrushing"
        trial = modeSharedPreferences.getString("${parId}_${modeName}", "1")?.toInt() ?: 1

        initializeFiles()
        serviceStartTime = System.currentTimeMillis()

        // 오디오 녹음 시 최소 버퍼 사이즈 계산
        bufferSize = AudioRecord.getMinBufferSize(
            sampleRateInHz,
            audioChannel,
            AudioFormat.ENCODING_PCM_16BIT
        )
    }

    // 서비스가 시작될 때마다 호출

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Service", "onStartCommand 호출됨")
        if (!isInitialized) {
            initializeSensorsAndLocation()

            startForegroundService()
            startSensorCollection()
            startAudioRecording()

            val duration = settingSharedPreferences.getString("duration", "1")?.toInt() ?: 1
            startTimer(duration)

            startTime = System.currentTimeMillis()
            isInitialized = true
        } else {
            Log.d("Service", "서비스 이미 초기화됨")
        }
        return START_STICKY
        }

    private fun startTimer(durationInHours: Int) {
        timer?.cancel()
        // 여기서 1을 3600으로 바꿔줘야 Hour 단위가 됨.
        durationInMillis = (durationInHours * 60 * 1000).toLong()

        timer = object : CountDownTimer(durationInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // 남은 시간을 Broadcast로 UI에 전달
                val intent = Intent("TIMER_UPDATE")
                intent.putExtra("millisUntilFinished", millisUntilFinished)
                LocalBroadcastManager.getInstance(this@SensorService).sendBroadcast(intent)
            }

            override fun onFinish() {
                // 서비스를 더 지속시킨 후에 종료 -> 데이터가 덜 찍히는 문제 해결하기 위해..
                delayServiceStop(100)
            }
        }.start()
    }

    private fun delayServiceStop(delayInMillis: Long) {
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            Log.d("SensorService", "추가 대기 후 서비스 종료")
            stopSensorCollection()
            triggerVibration()

            val updatedTrial = trial + 1
            val editor = getSharedPreferences("collect_mode_setting", Context.MODE_PRIVATE).edit()
            editor.putString("${parId}_${modeName}", updatedTrial.toString())
            editor.apply()

            val updateIntent = Intent("TRIAL_UPDATED")
            updateIntent.putExtra("updatedTrial", updatedTrial)
            LocalBroadcastManager.getInstance(this@SensorService).sendBroadcast(updateIntent)

        }, delayInMillis)
    }

    private fun triggerVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val vibrationAmplitude = 255
        vibrator.vibrate(VibrationEffect.createOneShot(1000, vibrationAmplitude))
    }

    private fun initializeSensorsAndLocation() {
        // 센서 및 위치 초기화 로직
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        Log.d("Service", "센서 및 위치 초기화 완료")
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(notificationChannelId) == null) {
            val channel = NotificationChannel(
                notificationChannelId,
                "Sensor Service Channel",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "This channel is used by sensor service"
            }
            notificationManager.createNotificationChannel(channel)
            Log.d("서비스", "NotificationChannel 생성 완료")
        } else {
            Log.d("서비스", "이미 NotificationChannel이 존재함")
        }

        val resultIntent = Intent(this, StartSensorActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            resultIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        Log.d("서비스", "PendingIntent 생성 완료")

        // Notification Builder 설정
        val notificationBuilder = NotificationCompat.Builder(this, notificationChannelId)
            .setContentTitle("HCILab")
            .setContentText("센서 데이터 수집 중...")
            .setSmallIcon(R.drawable.android) // 아이콘 리소스 확인 필요
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // IMPORTANCE와 일치
            .setDefaults(NotificationCompat.DEFAULT_ALL) // 소리, 진동 등 기본 설정 적용
        Log.d("서비스", "NotificationCompat.Builder 설정 완료")

        // 아이콘 리소스 확인
        val icon = ContextCompat.getDrawable(this, com.example.wath_test.R.drawable.android)
        if (icon == null) {
            Log.e("서비스", "알림 아이콘(R.drawable.android)이 존재하지 않음")
        } else {
            Log.d("서비스", "알림 아이콘(R.drawable.android)이 정상적으로 로드됨")
        }

        val notification: Notification = try {
            notificationBuilder.build().also {
                Log.d("서비스", "Notification 빌드 완료")
            }
        } catch (e: Exception) {
            Log.e("서비스", "Notification 빌드 실패: ${e.message}")
            return
        }

        try {
            // 포그라운드 서비스 시작
            startForeground(notificationId, notification)
            Log.d("서비스", "startForeground 호출 완료, 포그라운드 서비스 시작됨")
        } catch (e: Exception) {
            Log.e("서비스", "startForeground 실패: ${e.message}")
        }
    }

    private fun startSensorCollection() {

        accelerometer?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
        gyroscope?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }
        rotationVector?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }

        // IMU 데이터 수집 코루틴 시작 (20ms 주기)
        imuJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                logImuData()
                delay(samplingInterval)
            }
        }
    }
    // 20ms마다 IMU 데이터를 로그 버퍼에 기록하는 함수 (코루틴 내에서 호출)
    private suspend fun logImuData() {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val elapsed = (currentTime - serviceStartTime) / 1000.0
            val header = "$currentTime,$elapsed"

            // 최신 센서값을 가져와서 한 줄의 CSV 문자열 생성
            val imuData = "$header," +
                    "${latestAccel.joinToString(",")}," +
                    "${latestGyro.joinToString(",")}," +
                    "${latestRotVec.joinToString(",")}\n"
            imuLogBuffer.append(imuData)

            // 로그 버퍼가 일정 크기 이상이면 파일에 flush
            if (imuLogBuffer.length > 8192) {
                imuWriter.write(imuLogBuffer.toString())
                imuWriter.flush()
                imuLogBuffer.clear()
            }
        }
    }
    // 오디오 녹음 및 데이터 기록 함수 (코루틴 사용)
    private fun startAudioRecording() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.e("Audio", "오디오 권한 없음")
            stopSelf()
            return
        }
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRateInHz,
            audioChannel,
            audioFormat,
            bufferSize
        )
        audioRecord.startRecording()
        isRecording = true
        audioJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ShortArray(bufferSize)
            while (isActive && isRecording) {
                val readCount = audioRecord.read(buffer, 0, buffer.size)
                if (readCount > 0) {
                    logAudioData(buffer.take(readCount).joinToString(","))
                }
            }
        }
    }

    // 오디오 데이터를 로그 버퍼에 기록하는 함수
    private suspend fun logAudioData(data: String) {
        withContext(Dispatchers.IO) {
            val currentTime = System.currentTimeMillis()
            val elapsed = (currentTime - serviceStartTime) / 1000.0
            val header = "$currentTime,$elapsed"
            val audioLine = "$header,$data\n"
            audioLogBuffer.append(audioLine)
            if (audioLogBuffer.length > 8192) {
                audioWriter.write(audioLogBuffer.toString())
                audioWriter.flush()
                audioLogBuffer.clear()
            }
        }
    }

    private fun stopSensorCollection() {
        sensorManager.unregisterListener(this)
        stopRecording()
        stopForeground(true)
        stopSelf()
    }


    // 여기서는 저장될 파일을 초기화 한다.
    private fun initializeFiles() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val second = calendar.get(Calendar.SECOND)

        if (!baseFolder.exists()) {baseFolder.mkdirs()}

        // 피험자 번호 하위 폴더 생성
        participantFolder = File(baseFolder, parId)
        if (!participantFolder.exists()) {
            participantFolder.mkdirs()
        }

        imuFileName = "${parId}_${modeName}_${year}_${month}_${day}_${hour}_${minute}_${second}_SensorData.csv"
        audioFileName = "${parId}_${modeName}_${year}_${month}_${day}_${hour}_${minute}_${second}_AudioData.csv"

        val imuFile = File(participantFolder, imuFileName)
        val audioFile = File(participantFolder, audioFileName)

        // 파일이 존재하는지 먼저 확인하고, 없으면 헤더를 작성
        if (!imuFile.exists()) {
            imuFile.createNewFile()
            FileWriter(imuFile).use { writer ->
                writer.append("UnixTime,Time,AccX,AccY,AccZ,GyroX,GyroY,GyroZ,RotVecX,RotVecY,RotVecZ\n")
                writer.flush()
            }
        }
        if (!audioFile.exists()) {
            audioFile.createNewFile()
            FileWriter(audioFile).use { writer ->
                val audioDataColumns = (1..1280).joinToString(",") { "AudioData$it" }
                writer.append("UnixTime,Time,$audioDataColumns\n")
                writer.flush()
            }
        }
        // 헤더 작성 후에 FileWriter를 append 모드로 초기화
        imuWriter = FileWriter(imuFile, true).buffered()
        audioWriter = FileWriter(audioFile, true).buffered()
    }

    private fun stopRecording() {
        try {
            isRecording = false
            if (::audioRecord.isInitialized) {
                audioRecord.stop()
                audioRecord.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 센서 이벤트 발생 시 최신값만 업데이트
    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> synchronized(latestAccel) {
                    if (it.values.size >= 3) it.values.copyInto(latestAccel, 0, 0, 3)
                }
                Sensor.TYPE_GYROSCOPE -> synchronized(latestGyro) {
                    if (it.values.size >= 3) it.values.copyInto(latestGyro, 0, 0, 3)
                }
                Sensor.TYPE_ROTATION_VECTOR -> synchronized(latestRotVec) {
                    if (it.values.size >= 3) it.values.copyInto(latestRotVec, 0, 0, 3)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        stopSensorCollection()
        imuJob?.cancel()
        audioJob?.cancel()
        imuWriter.write(imuLogBuffer.toString())
        imuWriter.flush()
        imuWriter.close()
        audioWriter.write(audioLogBuffer.toString())
        audioWriter.flush()
        audioWriter.close()

        val intent = Intent("SERVICE_STOPPED")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

package com.example.silverpotion


import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.lifecycle.lifecycleScope
import com.example.silverpotion.network.HeartRateData
import com.example.silverpotion.network.RetrofitClient
import kotlinx.coroutines.launch
//import kotlin.time.Duration
import java.time.Duration


class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager

    // UI에서 사용하기 위한 UI상태변수 선언)
    private var stepCountData by mutableStateOf<List<Int>>(emptyList())
    private var dailyCaloriesBurnedData by mutableStateOf(0.0) //총 소모칼로리
    private var distanceWalkedData by mutableStateOf(0.0) //오늘 걸은 거리
    private var activeCaloriesBurnedData by mutableStateOf(0.0) //활동으로 인한 소모칼로리
    private var totalSleepMinutes by mutableStateOf(0L)
    private var deepSleepMinutes by mutableStateOf(0L)
    private var remSleepMinutes by mutableStateOf(0L)
    private var lightSleepMinutes by mutableStateOf(0L)

    private val permissionLauncher =
        registerForActivityResult<Set<String>, Set<String>>(
            PermissionController.createRequestPermissionResultContract() //헬스커넥트 권한 요청 처리
        ) { granted: Set<String> -> //granted되면 권한리스트에 들어감
            Log.d("HEALTH_SYNC", "권한 요청 결과: $granted")
            if (granted.containsAll(healthConnectManager.permissions)) {
                fetchAndSend { stepData,dailyCaloriesBurned,distanceWalked,activeCaloriesBurned,totalSleep,deepSleep,remSleep,lightSleep ->
                    stepCountData = stepData
                    dailyCaloriesBurnedData = dailyCaloriesBurned
                    distanceWalkedData = distanceWalked
                    activeCaloriesBurnedData = activeCaloriesBurned
                    totalSleepMinutes = totalSleep
                    deepSleepMinutes = deepSleep
                    remSleepMinutes = remSleep
                    lightSleepMinutes = lightSleep
                    // fetchAndSend함수가 데이터를 가지고 오고 그 결과값들을 각각의 UI상태 변수에 저장. 그리고 그 변수는 UI에서 나타내기 위해 사용
                    // UI에 데이터를 업데이트하는 작업을 여기에 추가하면
                }
            } else {
                Log.e("HEALTH_SYNC", "권한 요청 실패")
            }
        }
//permissionLauncher가 헬스커넥트 권한을 요청하고 결과를 처리하는 객체

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("HEALTH_SYNC", "앱 실행됨")
        healthConnectManager = HealthConnectManager(this)


// Jetpack Compose로 UI 구성
        setContent {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "실버포션",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold, // 글씨 굵게 설정
                        color = Color.Blue // 텍스트 색상을 파란색으로 설정
                    ),  // 큰 글씨 스타일 적용
                    modifier = Modifier.padding(bottom = 32.dp) // 하단 여백 추가
                )


                // 걸음수만 표시하는 텍스트 추가
                Text(text = "걸음수 데이터: ${stepCountData.joinToString(", ")} 보")
                // 칼로리 소모량만 표시하는 텍스트 추가
                Text(text = "칼로리 소모량: ${"%.2f".format(dailyCaloriesBurnedData)} kcal")
                // 거리 출력(미터기준)
                Text(text = "오늘 걸은 거리: ${"%.2f".format(distanceWalkedData)} m")
                Text(text = "활동 칼로리: ${"%.2f".format(activeCaloriesBurnedData)} kcal")
                Text(text = "총 수면 시간: ${totalSleepMinutes}분")
                Text(text = "깊은 수면: ${deepSleepMinutes}분")
                Text(text = "렘 수면: ${remSleepMinutes}분")
                Text(text = "얕은 수면: ${lightSleepMinutes}분")
                Button(onClick = {
                    // 버튼 클릭 시 fetchAndSend() 실행
                    permissionLauncher.launch(healthConnectManager.permissions)
                }) {
                    Text(text = "데이터 가져오기 및 서버 전송")
                }
            }
        }
    }


    private fun fetchAndSend(onDataFetched: (List<Int>, Double, Double, Double, Long, Long, Long, Long) -> Unit) { //서버로 데이터 전송하는 함수
//        onDataFetched: (List<Int>, List<HeartRateData>) -> Unit는 함수 타입선언부분 List<Int>, List<HeartRateData>두 가지 탑을 인자로 받는 함수타입선언. Unit은 반환값이 없다는 의미
        Log.d("HEALTH_SYNC", "fetchAndSend 실행됨")
        lifecycleScope.launch {
            try {
//                걸음수 데이터 읽기
                val stepRecords =healthConnectManager.readStepCounts()
                val stepData = stepRecords.map{it.count.toInt()} //itdms stepRecords리스트의 각각의 요소.it은 람다 함수에서 사용하는 기본 파라미터
//                 칼로리 소모량 데이터 읽기
                val caloriesBurnedRecords = healthConnectManager.readCaloriesBurned()
                val caloriesBurnedData = caloriesBurnedRecords.sumOf { it.energy.inKilocalories }
                val dailyCaloriesBurned = caloriesBurnedData
//                심박수데이터읽기
                val heartRateRecords =healthConnectManager.readHeartRates()
                val heartRateData = heartRateRecords.map{
                    val sample = it.samples.firstOrNull()
                    HeartRateData(
                        bpm = sample?.beatsPerMinute?.toDouble() ?:0.0,
                        time = sample?.time.toString() //bpm:xx.x ,time: 2025-03-39 몇시 이런식으로 json형식
                    )
                }
//               오늘 걸은 거리
                val distanceRecords = healthConnectManager.readDistanceWalked()
                val totalDistance = distanceRecords.sumOf { it.distance.inMeters }
//              활동으로 소모한 칼로리
                val activeCalorieRecords = healthConnectManager.readActiveCaloriesBurned()
                val activeCaloriesBurned = activeCalorieRecords.sumOf { it.energy.inKilocalories }
//              수면기록
                val sleepSessions = healthConnectManager.readSleepSessions()

                val totalSleepMillis = sleepSessions.sumOf { Duration.between(it.startTime, it.endTime).toMillis() }
                val totalSleepInMinutes = totalSleepMillis / 1000 /60 //밀리초를 분으로 바꾸는 것
                var remMinutes = 0L
                var deepMinutes =0L
                var lightMinutes =0L

                sleepSessions.forEach { session ->
                    session.stages.forEach { stage ->
                        val duration = Duration.between(stage.startTime, stage.endTime).toMinutes()
                        when (stage.stage) {
                            SleepSessionRecord.STAGE_TYPE_REM -> remMinutes += duration
                            SleepSessionRecord.STAGE_TYPE_DEEP -> deepMinutes += duration
                            SleepSessionRecord.STAGE_TYPE_LIGHT -> lightMinutes += duration
                        }
                    }
                }
                totalSleepMinutes = totalSleepInMinutes
                deepSleepMinutes = deepMinutes
                remSleepMinutes = remMinutes
                lightSleepMinutes = lightMinutes

                Log.d("HEALTH_SYNC", "걸음수 데이터: ${stepData.joinToString(", ")}")
                Log.d("HEALTH_SYNC", "심박수 데이터: ${heartRateRecords.size}개")
                Log.d("HEALTH_SYNC", "총 소모 칼로리 레코드 수: ${caloriesBurnedRecords.size}")
                Log.d("HEALTH_SYNC", "걸은 거리 레코드 수: ${distanceRecords.size}")
                Log.d("HEALTH_SYNC", "활동 칼로리 레코드 수: ${activeCalorieRecords.size}")
                Log.d("HEALTH_SYNC", "칼로리 소모량 데이터: $dailyCaloriesBurned")
                Log.d("HEALTH_SYNC", "걸은 거리: $totalDistance m")
                Log.d("HEALTH_SYNC", "활동 칼로리: $activeCaloriesBurned")
                Log.d("HEALTH_SYNC", "총 수면 시간: $totalSleepInMinutes")
                Log.d("HEALTH_SYNC", "깊은 수면: $deepMinutes")
                Log.d("HEALTH_SYNC", "렘 수면: $remMinutes")
                Log.d("HEALTH_SYNC", "얇은 수면: $lightMinutes")



                // onDataFetched 호출하면서 데이터를 UI에넘겨줌
                onDataFetched(stepData,dailyCaloriesBurned,totalDistance,activeCaloriesBurned,totalSleepInMinutes,deepMinutes,remMinutes,lightMinutes)


                val healthData = HealthData( //아레에서 HealthData 코틀린클래스를 정의함
                                stepData = stepData,
                                heartRateData = heartRateData,
                                caloriesBurnedData = dailyCaloriesBurned,
                                distanceWalked = totalDistance,
                                activeCaloriesBurned = activeCaloriesBurned,
                                totalSleepMinutes = totalSleepInMinutes,
                                deepSleepMinutes = deepMinutes,
                                remSleepMinutes = remMinutes,
                                lightSleepMinutes = lightMinutes
                            )
                // 서버로 전송
//               RetrofitClient.apiService.sendHealthData(healthData)
            } catch (e: Exception) {
                Log.e("HEALTH_SYNC", "에러 발생", e)
            }
        }
    }
}


// HealthData 클래스를 정의
data class HealthData(
    val stepData: List<Int>,
    val heartRateData: List<HeartRateData>,
    val caloriesBurnedData: Double,
    val distanceWalked: Double,
    val activeCaloriesBurned: Double,
    val totalSleepMinutes: Long,
    val deepSleepMinutes: Long,
    val remSleepMinutes: Long,
    val lightSleepMinutes: Long
)
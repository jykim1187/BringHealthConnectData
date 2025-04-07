package com.example.silverpotion.network

import com.example.silverpotion.HealthData
import retrofit2.http.Body
import retrofit2.http.POST

//data class 데이터를 담기 위한 클래스 선언 방식.
//(val bpm: Double, val time: String) ->이 클래스가 가지는 속성과 타입을 선언 bpm속성은 심박수 ,time속성은 측정시간
data class HeartRateData(val bpm: Double, val time: String)

interface ApiService {
//    suspend :코루틴 안에서 실행되는 함수 fun함수 선언
//    함수 인자로 HeartRateData의 리스트를 전달하고 HTTP 본분(바디)에 담는 다는 뜻
//    @POST("/hh/receive")
//    suspend fun sendHeartRates(@Body data: List<HeartRateData>)
//    @POST("/api/health/step-count")
//    suspend fun sendStepCounts(@Body stepData: List<Int>)
    @POST("/hh/receive")
    suspend fun sendHealthData(@Body healthData: HealthData )
}
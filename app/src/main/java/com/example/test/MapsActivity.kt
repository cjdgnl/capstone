package com.example.test

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import com.skt.Tmap.TMapData
import com.skt.Tmap.TMapMarkerItem
import com.skt.Tmap.TMapPoint
import com.skt.Tmap.TMapView


class MapsActivity : ComponentActivity() {
    private val LOCATION_PERMISSION_REQUEST = 1
    private val client = OkHttpClient()
    private lateinit var fusedLocationClient: FusedLocationProviderClient // 안드로이드서 제공하는 위치 정보 서비스 api
    private lateinit var tMapView: TMapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        // TMap 뷰 설정
        tMapView = TMapView(this)
        tMapView.setSKTMapApiKey("UR12n8NxMX11hi3IhN9gR9vC4paPTvsn1NvuHL6M")

        //val linearLayoutTmap = findViewById<LinearLayout>(R.id.tmapViewContainer)
        //linearLayoutTmap.addView(tMapView)

        // 위치 권한을 체크하고 요청
        // 위치 권한이 없을 때
        Log.e("MapsActivity", "진행")
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // 위치 권한 요청
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST
            )
            Log.e("MapsActivity", "위치권한 체크")
        } else {
            // 위치 권한이 있을 때 정확한 위치를 요청
            val locationRequest = com.google.android.gms.location.LocationRequest.create().apply {
                priority = com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
            }

            // 임의로 설정
            val end_X = 127.0807333
            val end_Y = 37.79825

            // 현재 위치를 가져옴
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val start_X = 127.081101 //location.latitude
                    val start_Y = 37.798489 //location.longitude

                    if(tMapView != null){
                        tMapView.setCenterPoint(start_X, start_Y)
                    }

                    val tMapData = TMapData()

                    val startPoint = TMapPoint(start_X, start_Y)
                    val endPoint = TMapPoint(end_X, end_Y)

                    // 시작점과 도착점 사이의 경로를 찾음
                    tMapData.findPathDataWithType(TMapData.TMapPathType.PEDESTRIAN_PATH, startPoint, endPoint) { tMapPolyLine ->
                        // 찾은 경로를 지도에 추가
                        //tMapView.addTMapPolyLine(tMapPolyLine)

                        // 시작점 마커 설정
                        val startMarker = TMapMarkerItem().apply {
                            tMapPoint = startPoint
                            //icon = (resources.getDrawable(R.drawable.poi_dot, null) as BitmapDrawable).bitmap
                            setPosition(0.5f, 1.0f)
                        }
                        //tMapView.addTMapMarkerItem(startMarker)

                        // 도착점 마커 설정
                        val endMarker = TMapMarkerItem().apply {
                            tMapPoint = endPoint
                            //icon = (resources.getDrawable(R.drawable.poi_dot, null) as BitmapDrawable).bitmap
                            setPosition(0.5f, 1.0f)
                        }
                        //tMapView.addTMapMarkerItem(endMarker)
                    }

                    updateRoute(start_X, start_Y, end_X, end_Y) //Tmap 대중교통 API를 호출하는 함수
                }
            }
        }
    }
    // 대중교통 경로를 업데이터하는 함수
    private fun updateRoute(start_X: Double, start_Y: Double, end_X: Double, end_Y: Double){

        // api 호출에 필요한 데이터 설정
        val requestData = RequestData(
            startX = start_X.toString(),
            startY = start_Y.toString(),
            endX = end_X.toString(),
            endY = end_Y.toString(),
            lang = 0, // 언어
            format = "json", // 형식
            count = 10
        )
        val json = Gson().toJson(requestData)

        // api 호출을 위한 요청을 설정
        val mediaType = "application/json".toMediaTypeOrNull()
        val body = RequestBody.create(mediaType, json)
        val request = Request.Builder()
            .url("https://apis.openapi.sk.com/transit/routes")
            .post(body)
            .addHeader("accept", "application/json")
            .addHeader("content-type", "application/json")
            .addHeader("appKey", "UR12n8NxMX11hi3IhN9gR9vC4paPTvsn1NvuHL6M")
            .build()

        // api 호출 결과를 처리
        val response = client.newCall(request).execute()

        if (response.isSuccessful) {
            val responseBody = response.body
            if (responseBody != null) {
                val responseString = responseBody.string()

                val startNavigationButton = findViewById<Button>(R.id.startNavigationButton)
                startNavigationButton.setOnClickListener{
                    // 결과를 다음 액티비티로 전달
                    val intent = Intent(this@MapsActivity, MainActivity::class.java)
                    intent.putExtra("responseString", responseString)
                    startActivity(intent)
                    responseBody.close()
                }

            } else {
                println("Response body is null")
            }
        } else {
            Log.e("실패", "${response.code}")
        }
    }
    data class RequestData(
        val startX: String,
        val startY: String,
        val endX: String,
        val endY: String,
        val lang: Int,
        val format: String,
        val count: Int
    )

}
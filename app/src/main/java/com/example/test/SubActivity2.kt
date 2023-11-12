package com.example.test

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayList
import java.util.Locale

class SubActivity2:  ComponentActivity() {
    private lateinit var tts: TextToSpeech
    private val LOCATION_PERMISSION_REQUEST = 1 // 위치 권한 요청 코드
    private lateinit var fusedLocationClient: FusedLocationProviderClient // FusedLocationProviderClient 초기화를 나중에 할 것임을 선언
    private lateinit var locationManager: LocationManager  // 위치 정보를 관리하는 매니저
    private var currentLocation: Location? = null  // 현재 위치를 저장할 변수
    private var currentFeatureIndex = 0  // 현재 처리 중인 피처의 인덱스
    private lateinit var lineDescriptionRecyclerView: RecyclerView
    private lateinit var lineFeatures: ArrayList<JSONObject>
    private var pointFeatures: ArrayList<JSONObject>? = null

    // 위치 정보가 변경될 때마다 호출되는 리스너
    private val locationListener: LocationListener = object : LocationListener {
        // 위치 정보가 변경될 때마다 호출
        override fun onLocationChanged(location: Location) {
            // 변경된 위치의 위도와 경도를 로그에 출력
            Log.i("Location", "Latitude: ${location.latitude}, Longitude: ${location.longitude}")

            currentLocation = location

            // 현재 처리 중인 Point Feature가 배열의 크기보다 작은 경우 (즉, 처리해야 할 Point Feature가 남아있는 경우)
            if (currentFeatureIndex < pointFeatures?.size ?: 0) {
                // 현재 처리 중인 Point Feature를 가져옴
                val pointFeature = pointFeatures?.get(currentFeatureIndex)
                // Point의 좌표를 가져옴
                val coordinates = pointFeature?.getJSONObject("geometry")?.getJSONArray("coordinates")
                // 좌표의 x, y 값을 각각 가져옴
                val x = coordinates?.getDouble(0) ?: 0.0
                val y = coordinates?.getDouble(1) ?: 0.0

                // 현재 위치와 Point의 좌표 사이의 거리를 계산
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, y, x, results)
                val distanceInMeters = results[0]

                // 거리가 10m 미만이라면 다음 Point로 넘어감
                if (distanceInMeters < 10) {
                    currentFeatureIndex++
                    // 다음 Point가 배열의 크기보다 작은 경우 (즉, 처리해야 할 Point가 남아있는 경우)
                    if (currentFeatureIndex < pointFeatures?.size?: 0) {
                        // 다음 Point를 가져옴
                        val nextPointFeature = pointFeatures?.get(currentFeatureIndex)
                        // 다음 Point의 description을 가져옴
                        val nextDescription = nextPointFeature?.getJSONObject("properties")?.getString("description")
                        // TTS로 description을 읽음
                        tts.speak(nextDescription, TextToSpeech.QUEUE_ADD, null, null)
                    }
                    // 다음 LineString이 배열의 크기보다 작은 경우 (즉, 처리해야 할 LineString이 남아있는 경우)
                    if (currentFeatureIndex < lineFeatures.size) {
                        // 다음 LineString를 가져옴
                        val nextLineFeature = lineFeatures[currentFeatureIndex]
                        // 다음 LineString의 description을 가져옴
                        val nextDescription = nextLineFeature.getJSONObject("properties").getString("description")
                    }
                }
            }


        }
        // Provider의 상태가 변경될 때 호출되는 메소드, 여기서는 구현하지 않음
        override fun onStatusChanged(provider: String, status: Int, extras: Bundle) { }
        // Provider가 사용 가능해질 때 호출되는 메소드, 여기서는 구현하지 않음
        override fun onProviderEnabled(provider: String) { }
        // Provider가 사용 불가능해질 때 호출되는 메소드, 여기서는 구현하지 않음
        override fun onProviderDisabled(provider: String) { }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sub2)

        // RecyclerView 설정
        lineDescriptionRecyclerView = findViewById(R.id.line_description_recycler_view)

        // RecyclerView의 레이아웃 매니저 설정
        lineDescriptionRecyclerView.layoutManager = LinearLayoutManager(this)

        tts = TextToSpeech(applicationContext){status ->
            if (status != TextToSpeech.ERROR){
                tts.language = Locale.KOREAN
                Log.i("TTS", "TextToSpeech is initialized.")
            } else {
                Log.e("TTS", "Error in TextToSpeech initialization.")
            }
        }

        // 시스템 위치 서비스를 사용하여 LocationManager 객체를 얻음
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        // 위치 접근 권한이 있는지 체크
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없다면 위치 접근 권한 요청
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST)
        } else {
            // 권한이 있다면 위치 업데이트 시작
            startLocationUpdates()
        }

        val featureStrings = intent.getStringArrayListExtra("features") ?: arrayListOf<String>()
        val lineFeatures = ArrayList<JSONObject>()
        val pointFeatures = ArrayList<JSONObject>()
        // featureStrings를 읽어와서 lineFeatures와 pointFeatures에 추가합니다.
        for (featureString in featureStrings) {
            val feature = JSONObject(featureString)
            val geometry = feature.getJSONObject("geometry")
            if (geometry.getString("type") == "LineString") {
                lineFeatures.add(feature)
            } else if (geometry.getString("type") == "Point") {
                pointFeatures.add(feature)
            }
        }

        val routeDescriptions = ArrayList<RouteDescription>()
        val coordinatesList = ArrayList<JSONArray>()

        var pointIndex = 0
        var lineIndex = 0
        var pointTurnType: String? = null


        while (pointIndex < pointFeatures?.size ?: 0 || lineIndex < lineFeatures.size) {
            // pointFeatures를 처리
            if (pointIndex < pointFeatures?.size ?: 0) {
                val pointFeature = pointFeatures?.get(pointIndex)
                val pointProperties = pointFeature?.getJSONObject("properties")
                val pointDescription = pointProperties?.getString("description")
                pointTurnType = pointProperties?.getString("turnType")
                // turnType 코드를 변환
                val turnTypeDescription = when (pointTurnType?.toInt()) {
                    11 -> "직진"
                    12 -> "좌회전"
                    13 -> "우회전"
                    14 -> "유턴"
                    16 -> "8시 방향 좌회전"
                    17 -> "10시 방향 좌회전"
                    18 -> "2시 방향 우회전"
                    19 -> "4시 방향 우회전"
                    184 -> "경유지"
                    185 -> "첫번째 경유지"
                    186 -> "두번째 경유지"
                    187 -> "세번째 경유지"
                    188 -> "네번째 경유지"
                    189 -> "다섯번째 경유지"
                    125 -> "육교"
                    126 -> "지하보도"
                    127 -> "계단 진입"
                    128 -> "경사로 진입"
                    129 -> "계단+경사로 진입"
                    200 -> "출발지"
                    201 -> "목적지"
                    211 -> "횡단보도"
                    212 -> "좌측 횡단보도"
                    213 -> "우측 횡단보도"
                    214 -> "8시 방향 횡단보도"
                    215 -> "10시 방향 횡단보도"
                    216 -> "2시 방향 횡단보도"
                    217 -> "4시 방향 횡단보도"
                    218 -> "엘리베이터"
                    233 -> "직진 임시"
                    else -> "알 수 없음"
                }

                // TTS로 pointDescription을 읽습니다.
                tts.speak(pointDescription, TextToSpeech.QUEUE_ADD, null, null)

                pointIndex++

                // lineFeatures를 처리합니다.
                if (lineIndex < lineFeatures.size) {
                    val lineFeature = lineFeatures[lineIndex]
                    val lineProperties = lineFeature.getJSONObject("properties")
                    var lineDistance: Double? = null
                    if (lineProperties.has("distance")) { // distance 키가 있는지 확인
                        lineDistance = lineProperties.getDouble("distance")
                    }

                    // 화면에 띄울 내용을 routeDescriptions에 추가합니다.
                    routeDescriptions.add(RouteDescription("", turnTypeDescription, lineDistance ?: 0.0))

                    // coordinates를 coordinatesList에 추가합니다.
                    val coordinates = lineFeature.getJSONObject("geometry").getJSONArray("coordinates")
                    coordinatesList.add(coordinates)

                    lineIndex++
                }
            }
            // RecyclerView의 어댑터를 설정합니다.
            lineDescriptionRecyclerView.adapter = RouteDescriptionAdapter(routeDescriptions)
        }


    }
    // 위치 업데이트를 시작하는 메소드
    private fun startLocationUpdates() {
        // 위치 접근 권한이 있는지 확인
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없다면 메소드 종료
            return
        }
        // 위치 업데이트 요청 (Provider: GPS, 최소 시간 간격: 5000ms, 최소 거리 간격: 10m, 리스너: locationListener)
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 5f, locationListener)
    }
}
data class RouteDescription(val lineDescription: String, val turnType: String?, val distance: Double?)
class RouteDescriptionAdapter(private val routeDescriptions: List<RouteDescription>) : RecyclerView.Adapter<RouteDescriptionAdapter.ViewHolder>() {
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val turnTypeTextView: TextView = view.findViewById(R.id.turn_type_text_view)
        val distanceTextView: TextView = view.findViewById(R.id.distance_text_view)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.route_description_item, parent, false)
            ViewHolder(view)
        } catch (e: Exception) {
            Log.e("RouteDescriptionAdapter", "Error in onCreateViewHolder", e)
            throw e
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val routeDescription = routeDescriptions[position]
            holder.turnTypeTextView.text = routeDescription.turnType
            holder.distanceTextView.text = routeDescription.distance.toString()
        } catch (e: Exception) {
            Log.e("RouteDescriptionAdapter", "Error in onBindViewHolder", e)
        }
    }
    override fun getItemCount(): Int {
        return routeDescriptions.size
    }

}
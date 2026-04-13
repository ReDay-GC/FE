package com.example.reday

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.reday.data.remote.MapLocationData
import com.example.reday.data.repository.MemoryRepository
import com.example.reday.data.repository.RecordFragmentRepository
import kotlinx.coroutines.Job
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var repository: RecordFragmentRepository
    private lateinit var memoryRepository: MemoryRepository
    private var googleMap: GoogleMap? = null

    private lateinit var panelLocation: View
    private lateinit var tvPanelLocationName: TextView
    private lateinit var tvPanelCount: TextView
    private lateinit var rvMapMemories: RecyclerView

    // Marker → LocationData 매핑
    private val markerLocationMap = mutableMapOf<String, MapLocationData>()
    private var panelJob: Job? = null

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) moveToCurrentLocation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = RecordFragmentRepository()
        memoryRepository = MemoryRepository()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        panelLocation = view.findViewById(R.id.panel_location)
        tvPanelLocationName = view.findViewById(R.id.tv_panel_location_name)
        tvPanelCount = view.findViewById(R.id.tv_panel_count)
        rvMapMemories = view.findViewById(R.id.rv_map_memories)
        rvMapMemories.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)

        view.findViewById<ImageButton>(R.id.btn_panel_close).setOnClickListener {
            hideLocationPanel()
        }

        view.findViewById<ImageButton>(R.id.fab_my_location).setOnClickListener {
            val hasPermission = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) moveToCurrentLocation()
            else requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true

        map.setOnMapClickListener { hideLocationPanel() }

        map.setOnMarkerClickListener { marker ->
            val locationData = markerLocationMap[marker.id] ?: return@setOnMarkerClickListener false
            showLocationPanel(locationData)
            true
        }

        loadLocationGroups()
    }

    private fun loadLocationGroups() {
        viewLifecycleOwner.lifecycleScope.launch {
            val locationGroups = memoryRepository.getMapLocations()
            val map = googleMap ?: return@launch
            if (locationGroups.isEmpty()) return@launch

            val boundsBuilder = LatLngBounds.Builder()
            var hasValidLocation = false

            locationGroups.forEach { locationData ->
                val lat = locationData.latitude ?: return@forEach
                val lng = locationData.longitude ?: return@forEach

                val position = LatLng(lat, lng)
                val markerBitmap = createMarkerBitmap(locationData.memoryCount)
                val d = resources.displayMetrics.density
                val pinW = 26 * d
                val badgeR = 9 * d
                val bmpW = pinW + badgeR
                val anchorX = (pinW / 2f) / bmpW

                val marker = map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .icon(BitmapDescriptorFactory.fromBitmap(markerBitmap))
                        .anchor(anchorX, 1.0f)
                ) ?: return@forEach

                markerLocationMap[marker.id] = locationData
                boundsBuilder.include(position)
                hasValidLocation = true
            }

            if (hasValidLocation) {
                try {
                    val bounds = boundsBuilder.build()
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                } catch (e: Exception) {
                    val first = locationGroups.firstOrNull { it.latitude != null } ?: return@launch
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(first.latitude!!, first.longitude!!), 15f)
                    )
                }
            }
        }
    }

    private fun showLocationPanel(locationData: MapLocationData) {
        tvPanelLocationName.text = locationData.location
        tvPanelCount.text = "${locationData.memoryCount}개"
        rvMapMemories.adapter = MapMemoryAdapter(emptyList()) { }
        panelLocation.isVisible = true

        panelJob?.cancel()
        panelJob = viewLifecycleOwner.lifecycleScope.launch {
            val memories = memoryRepository.getMapLocationMemories(locationData.location)
            rvMapMemories.adapter = MapMemoryAdapter(memories) { memory ->
                val intent = android.content.Intent(requireContext(), MemoryDetailActivity::class.java)
                intent.putExtra(MemoryDetailActivity.EXTRA_DATE, memory.date)
                startActivity(intent)
            }
        }
    }

    private fun hideLocationPanel() {
        panelLocation.isVisible = false
    }

    private fun moveToCurrentLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(requireContext())
        try {
            fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    location ?: return@addOnSuccessListener
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(location.latitude, location.longitude), 15f
                        )
                    )
                }
        } catch (e: SecurityException) {
            // 권한 없음
        }
    }

    private fun createMarkerBitmap(count: Int): Bitmap {
        val d = resources.displayMetrics.density

        val pinW = (26 * d)
        val pinH = (38 * d)
        val circleR = pinW / 2f
        val badgeR = (9 * d)

        // 배지가 핀 우상단을 벗어나므로 비트맵에 여유 공간 확보
        val bmpW = (pinW + badgeR).toInt()
        val bmpH = (pinH + badgeR).toInt()

        val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 핀 원 중심 (배지 공간만큼 아래로 내림)
        val pinCx = circleR
        val circleCy = badgeR + circleR

        val mainColor = ContextCompat.getColor(requireContext(), R.color.main_200)

        // ── 핀 꼬리 (원 하단 ~ 뾰족한 끝) ──
        paint.style = Paint.Style.FILL
        paint.color = mainColor
        val tailPath = Path()
        val ovalRect = RectF(pinCx - circleR, circleCy - circleR, pinCx + circleR, circleCy + circleR)
        tailPath.arcTo(ovalRect, 35f, 110f, true)
        tailPath.lineTo(pinCx, bmpH.toFloat())
        tailPath.close()
        canvas.drawPath(tailPath, paint)

        // ── 핀 머리 (원) ──
        canvas.drawCircle(pinCx, circleCy, circleR, paint)

        // ── 흰 내부 원 (빈티지 핀 느낌) ──
        paint.color = Color.WHITE
        canvas.drawCircle(pinCx, circleCy, circleR * 0.38f, paint)

        // ── 배지 (흰 테두리 → main_200 내부 → 흰 숫자) ──
        val badgeCx = pinCx + circleR * 0.65f
        val badgeCy = badgeR
        paint.color = Color.WHITE
        canvas.drawCircle(badgeCx, badgeCy, badgeR, paint)

        paint.color = mainColor
        canvas.drawCircle(badgeCx, badgeCy, badgeR - d, paint)

        // 배지 숫자
        paint.color = Color.WHITE
        paint.textSize = 8.5f * d
        paint.textAlign = Paint.Align.CENTER
        val textY = badgeCy - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(count.toString(), badgeCx, textY, paint)

        return bitmap
    }
}

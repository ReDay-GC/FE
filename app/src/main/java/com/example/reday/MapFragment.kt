package com.example.reday

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reday.data.local.AppDatabase
import com.example.reday.data.model.FragmentType
import com.example.reday.data.repository.RecordFragmentRepository
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var repository: RecordFragmentRepository
    private var googleMap: GoogleMap? = null

    private val requestLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) moveToCurrentLocation()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getInstance(requireContext())
        repository = RecordFragmentRepository(db.recordFragmentDao())
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
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment
        mapFragment.getMapAsync(this)

        view.findViewById<FloatingActionButton>(R.id.fab_my_location).setOnClickListener {
            val hasPermission = ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasPermission) moveToCurrentLocation()
            else requestLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        loadMarkers()
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

    private fun loadMarkers() {
        viewLifecycleOwner.lifecycleScope.launch {
            val fragments = repository.getFragmentsWithLocation()
            val map = googleMap ?: return@launch

            if (fragments.isEmpty()) return@launch

            val boundsBuilder = LatLngBounds.Builder()

            fragments.forEach { record ->
                val lat = record.latitude ?: return@forEach
                val lng = record.longitude ?: return@forEach
                val position = LatLng(lat, lng)

                val hue = when (record.fragmentType) {
                    FragmentType.PHOTO -> BitmapDescriptorFactory.HUE_ROSE
                    FragmentType.TEXT -> BitmapDescriptorFactory.HUE_GREEN
                    FragmentType.VOICE -> BitmapDescriptorFactory.HUE_AZURE
                }

                val snippet = buildString {
                    append(record.date)
                    if (!record.locationName.isNullOrBlank()) append(" · ${record.locationName}")
                }

                map.addMarker(
                    MarkerOptions()
                        .position(position)
                        .title(record.fragmentType.name)
                        .snippet(snippet)
                        .icon(BitmapDescriptorFactory.defaultMarker(hue))
                )

                boundsBuilder.include(position)
            }

            try {
                val bounds = boundsBuilder.build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
            } catch (e: Exception) {
                // 마커가 1개일 때 bounds 패딩 오류 방지
                val first = fragments.first()
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(first.latitude!!, first.longitude!!), 15f
                    )
                )
            }
        }
    }
}

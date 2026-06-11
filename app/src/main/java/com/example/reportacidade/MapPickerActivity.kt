package com.example.reportacidade

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.reportacidade.utils.LocationHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import java.util.Locale

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private lateinit var mMap: MapLibreMap
    private lateinit var locationHelper: LocationHelper
    private lateinit var tvAddress: TextView
    private var selectedAddressString: String = ""

    companion object {
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_ADDRESS = "address"
        private const val LOCATION_PERMISSION_CODE = 1001
        private const val MAP_STYLE = "https://tiles.basemaps.cartocdn.com/gl/positron-gl-style/style.json"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        MapLibre.getInstance(this)
        setContentView(R.layout.activity_map_picker)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_map_picker)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        locationHelper = LocationHelper(this)
        mapView = findViewById(R.id.mapView)
        tvAddress = findViewById(R.id.tvSelectedAddress)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        findViewById<Button>(R.id.btnConfirmar).setOnClickListener {
            val loc = if (::mMap.isInitialized) mMap.cameraPosition.target else null
            if (loc != null) {
                val intent = Intent().apply {
                    putExtra(EXTRA_LATITUDE, loc.latitude)
                    putExtra(EXTRA_LONGITUDE, loc.longitude)
                    putExtra(EXTRA_ADDRESS, selectedAddressString)
                }
                setResult(RESULT_OK, intent)
                finish()
            }
        }

        findViewById<FloatingActionButton>(R.id.fabMyLocation).setOnClickListener {
            centralizarNoUsuario()
        }

        findViewById<FloatingActionButton>(R.id.fabZoomIn).setOnClickListener {
            if (::mMap.isInitialized) {
                mMap.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.zoomIn())
            }
        }

        findViewById<FloatingActionButton>(R.id.fabZoomOut).setOnClickListener {
            if (::mMap.isInitialized) {
                mMap.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.zoomOut())
            }
        }
    }

    override fun onMapReady(map: MapLibreMap) {
        mMap = map
        mMap.setStyle(Style.Builder().fromUri(MAP_STYLE)) {
            verificarPermissoesEPosicionar()
        }

        val iconCentro = findViewById<android.widget.ImageView>(R.id.imageViewSelectedCenter)

        mMap.addOnCameraMoveStartedListener {
            iconCentro.animate().translationY(-50f).setDuration(200).start()
        }

        mMap.addOnCameraIdleListener {
            iconCentro.animate().translationY(0f).setDuration(200).start()
            mMap.cameraPosition.target?.let { atualizarEndereco(it) }
        }
    }

    private fun atualizarEndereco(loc: LatLng) {
        try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val addr = addresses[0]
                val rua = addr.thoroughfare ?: ""
                val bairro = addr.subLocality ?: addr.locality ?: ""
                selectedAddressString = if (rua.isNotEmpty()) "$rua - $bairro" else bairro
                tvAddress.text = selectedAddressString.ifEmpty { "Local selecionado" }
            }
        } catch (e: Exception) {
            tvAddress.text = "Localização fixada"
        }
    }

    private fun verificarPermissoesEPosicionar() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            centralizarNoUsuario()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
        }
    }

    private fun centralizarNoUsuario() {
        locationHelper.getCurrentLocation(
            onSuccess = { loc ->
                mMap.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder().target(LatLng(loc.latitude, loc.longitude)).zoom(16.0).build()
                ))
            },
            onError = { /* Centraliza no fallback se necessário */ }
        )
    }

    override fun onStart() { super.onStart(); mapView.onStart() }
    override fun onResume() { super.onResume(); mapView.onResume() }
    override fun onPause() { super.onPause(); mapView.onPause() }
    override fun onStop() { super.onStop(); mapView.onStop() }
    override fun onDestroy() { super.onDestroy(); mapView.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) { super.onSaveInstanceState(outState); mapView.onSaveInstanceState(outState) }
}

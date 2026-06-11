package com.example.reportacidade

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.reportacidade.data.model.Report
import com.example.reportacidade.data.repository.MockReportRepositoryImpl
import com.example.reportacidade.data.repository.ReportRepository
import com.example.reportacidade.utils.LocationHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.OnMapReadyCallback
import org.maplibre.android.maps.Style
import org.maplibre.android.plugins.annotation.OnSymbolClickListener
import org.maplibre.android.plugins.annotation.SymbolManager
import org.maplibre.android.plugins.annotation.SymbolOptions

class MapaFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var mapLibreMap: MapLibreMap? = null
    private lateinit var locationHelper: LocationHelper
    private lateinit var reportRepository: ReportRepository
    private var symbolManager: SymbolManager? = null

    companion object {
        private const val MAP_STYLE = "https://tiles.basemaps.cartocdn.com/gl/positron-gl-style/style.json"
        private const val LOCATION_PERMISSION_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        MapLibre.getInstance(requireContext())
        val view = inflater.inflate(R.layout.fragment_mapa, container, false)
        
        mapView = view.findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
        
        locationHelper = LocationHelper(requireContext())
        reportRepository = MockReportRepositoryImpl.getInstance(requireContext())
        
        view.findViewById<FloatingActionButton>(R.id.fabNovaDenuncia).setOnClickListener {
            (activity as? MainActivity)?.showAddOrEditReportDialog()
        }

        view.findViewById<FloatingActionButton>(R.id.fabZoomIn).setOnClickListener {
            mapLibreMap?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.zoomIn())
        }

        view.findViewById<FloatingActionButton>(R.id.fabZoomOut).setOnClickListener {
            mapLibreMap?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.zoomOut())
        }
        
        return view
    }

    override fun onMapReady(map: MapLibreMap) {
        this.mapLibreMap = map
        map.setStyle(Style.Builder().fromUri(MAP_STYLE)) { style ->
            // Adiciona ícones customizados por categoria
            adicionarIcones(style)
            adicionarMarcadores(style)
        }
        verificarPermissoesEPosicionar()
    }

    private fun adicionarIcones(style: Style) {
        val categories = mapOf(
            "BURACO" to R.drawable.ic_category_buraco,
            "ILUMINACAO" to R.drawable.ic_category_iluminacao,
            "LIXO" to R.drawable.ic_category_lixo,
            "ENCHENTE" to R.drawable.ic_category_enchente,
            "VANDALISMO" to R.drawable.ic_category_vandalismo
        )

        categories.forEach { (name, resId) ->
            val drawable = androidx.core.content.ContextCompat.getDrawable(requireContext(), resId)
            drawable?.let { style.addImage("marker-$name", it) }
        }
    }

    private fun verificarPermissoesEPosicionar() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            obterLocalizacaoECentralizar()
        } else if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            mostrarDialogoRacional()
        } else {
            solicitarPermissaoGps()
        }
    }

    private fun mostrarDialogoRacional() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Uso do GPS")
            .setMessage("Para mostrar sua posição no mapa e facilitar o relato de incidentes, precisamos acessar sua localização. Podemos ativar o GPS?")
            .setPositiveButton("Sim") { _, _ -> solicitarPermissaoGps() }
            .setNegativeButton("Agora não") { _, _ -> centralizarFallback() }
            .show()
    }

    private fun solicitarPermissaoGps() {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_CODE)
    }

    private fun centralizarFallback() {
        val position = CameraPosition.Builder()
            .target(LatLng(-5.79448, -35.211))
            .zoom(12.0)
            .build()
        mapLibreMap?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(position))
    }

    private fun obterLocalizacaoECentralizar() {
        locationHelper.getCurrentLocation(
            onSuccess = { loc ->
                val position = CameraPosition.Builder()
                    .target(LatLng(loc.latitude, loc.longitude))
                    .zoom(14.0)
                    .build()
                mapLibreMap?.animateCamera(org.maplibre.android.camera.CameraUpdateFactory.newCameraPosition(position))
            },
            onError = { centralizarFallback() }
        )
    }

    private fun adicionarMarcadores(style: Style) {
        symbolManager = SymbolManager(mapView, mapLibreMap!!, style)
        symbolManager?.iconAllowOverlap = true
        
        viewLifecycleOwner.lifecycleScope.launch {
            reportRepository.getAllReports().collectLatest { reports ->
                symbolManager?.deleteAll()
                reports.forEach { report ->
                    if (report.latitude != 0.0 && report.longitude != 0.0) {
                        val iconName = "marker-${report.category.name}"
                        symbolManager?.create(
                            SymbolOptions()
                                .withLatLng(LatLng(report.latitude, report.longitude))
                                .withIconImage(iconName)
                                .withIconSize(1.5f) // Ajustado de 0.1f para ícones 24dp
                                .withTextField(report.category.displayName)
                                .withTextSize(10f)
                                .withTextOffset(arrayOf(0f, 2f))
                                .withData(com.google.gson.JsonPrimitive(report.id))
                        )
                    }
                }
            }
        }

        symbolManager?.addClickListener(OnSymbolClickListener { symbol ->
            val reportId = symbol.data?.asString
            if (reportId != null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val report = reportRepository.getReportById(reportId)
                    report?.let { exibirCardReport(it) }
                }
            }
            true
        })
    }

    private fun exibirCardReport(report: Report) {
        val bottomSheet = ReportBottomSheet.newInstance(report)
        bottomSheet.show(parentFragmentManager, "ReportBottomSheet")
    }

    override fun onStart() { super.onStart(); if (::mapView.isInitialized) mapView.onStart() }
    override fun onResume() { super.onResume(); if (::mapView.isInitialized) mapView.onResume() }
    override fun onPause() { super.onPause(); if (::mapView.isInitialized) mapView.onPause() }
    override fun onStop() { super.onStop(); if (::mapView.isInitialized) mapView.onStop() }
    override fun onDestroy() { super.onDestroy(); if (::mapView.isInitialized) mapView.onDestroy() }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::mapView.isInitialized) mapView.onSaveInstanceState(outState)
    }
}

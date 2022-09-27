package net.datetegy.tammockupkotlin

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.navigation.ui.tripprogress.view.MapboxTripProgressView
import com.mapbox.search.MapboxSearchSdk
import com.mapbox.search.SearchEngine
import com.mapbox.search.SearchRequestTask
import com.mapbox.search.location.DefaultLocationProvider
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import java.util.ArrayList

class TurnByTurnActivity : AppCompatActivity() , OnMapReadyCallback,PermissionsListener,MapboxMap.OnMapClickListener{

    private val TAG = "TurnByTurnActivity"

    private val DEFAULT_INTERVAL_IN_MILLISECONDS = 10000L
    private val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

    /**
     * Draw a vector polygon on a map with the Mapbox Android SDK.
     */
    private val POINTS: MutableList<MutableList<Point>> = ArrayList()
    private val OUTER_POINTS_1: MutableList<Point> = ArrayList()

    private lateinit var mapViewPro : MapboxTripProgressView

    private lateinit var mapView : MapView
    private lateinit var map : MapboxMap
    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLocation : Location
    private lateinit var searchView : SearchView
    private lateinit var centerIt : Button
    private lateinit var startNavigating : Button

    private var locationEngine : LocationEngine? = null
    private var locationLayerPlugin : LocationLayerPlugin? = null

    private var latitude : Double = 0.0
    private var longitude : Double = 0.0

    private lateinit var searchEngine: SearchEngine
    private lateinit var searchRequestTask: SearchRequestTask

    private var destinationMarker: Marker? = null

    private var originPosition: Point? = null
    private var destinationPosition: Point? = null

    private var navigationMapRoute: NavigationMapRoute? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this,getString(R.string.access_token))
        setContentView(R.layout.activity_turn_by_turn)

        mapView = findViewById(R.id.mapViewTurnByTurn)

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)
    }

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        if (mapboxMap != null) {
            map = mapboxMap
            enableLocation()

            map.addOnMapClickListener(this)

            mapboxMap.setStyle(
                Style.MAPBOX_STREETS
            ) { style ->
                style.addSource(
                    GeoJsonSource(
                        "source-id",
                        Polygon.fromLngLats(POINTS)
                    )
                )
                style.addLayerBelow(
                    FillLayer("layer-id", "source-id")
                        .withProperties(
                            PropertyFactory.fillColor(Color.parseColor("#883bb2d0")),
                            PropertyFactory.fillColor(Color.parseColor("#88808000"))
                        ), "settlement-label"
                )
                enableLocationComponent(style)
            }
        }
        else
        {
            Toast.makeText(this,"mapboxMap == null",Toast.LENGTH_LONG).show()
        }
    }

    fun enableLocation()
    {
        if(PermissionsManager.areLocationPermissionsGranted(this))
        {
            initializeLocationEngine()
            initializeLocationLayer()
        }
        else
        {
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
        }
    }

@SuppressLint("WrongConstant", "MissingPermission")
private fun enableLocationComponent(loadedMapStyle: Style) {
    // Check if permissions are enabled and if not request
    if (PermissionsManager.areLocationPermissionsGranted(this)) {

        // Get an instance of the component
        val locationComponent = map.locationComponent

        // Activate with options
        locationComponent.activateLocationComponent(
            LocationComponentActivationOptions.builder(this, loadedMapStyle).build()
        )

        // Enable to make component visible
        locationComponent.isLocationComponentEnabled = true

        // Set the component's camera mode
        locationComponent.cameraMode = CameraMode.TRACKING

        // Set the component's render mode
        locationComponent.renderMode = RenderMode.COMPASS
    } else {
        permissionManager = PermissionsManager(this)
        permissionManager.requestLocationPermissions(this)
    }
}

    @SuppressWarnings("MissingPermission")
    private fun initializeLocationEngine()
    {


        locationEngine = LocationEngineProvider.getBestLocationEngine(this)

        locationEngine?.getLastLocation(object: LocationEngineCallback<LocationEngineResult> {
            override fun onSuccess(result: LocationEngineResult?) {
                if (result?.getLastLocation() != null) {
                    val request =
                        LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                            .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                            .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()
                    originLocation = result?.getLastLocation()!!
                    setCameraPosition(result?.getLastLocation()!!)
                }
            }

            override fun onFailure(p0: java.lang.Exception) {
                val request =
                    LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                        .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()

                locationEngine?.requestLocationUpdates(request,object:
                    LocationEngineCallback<LocationEngineResult> {
                    override fun onSuccess(result: LocationEngineResult?) {
                        originLocation = result?.getLastLocation()!!
                        setCameraPosition(result?.getLastLocation()!!)
                    }

                    override fun onFailure(p0: java.lang.Exception) {
                        TODO("Not yet implemented")
                    }

                },
                    Looper.getMainLooper())
            }

        })


    }

    @SuppressWarnings("MissingPermission")
    private fun initializeLocationLayer()
    {
//        locationLayerPlugin = LocationLayerPlugin(mapView,map,locationEngine)
//        locationLayerPlugin?.setLocationLayerEnabled(true)
//        locationLayerPlugin?.cameraMode = CameraMode.TRACKING
//        locationLayerPlugin?.renderMode = RenderMode.NORMAL
    }

    private fun setCameraPosition(location: Location)
    {
        Log.e(TAG, "setCameraPosition: " )
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude,location.longitude),13.0))
    }

    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        TODO("Not yet implemented")
    }

    override fun onPermissionResult(granted: Boolean) {
        TODO("Not yet implemented")
        if(granted)
        {
            //enableLocation()
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionManager.onRequestPermissionsResult(requestCode,permissions,grantResults)
    }

    override fun onMapClick(latLng: LatLng): Boolean {
        if (destinationMarker != null) {
            map.removeMarker(destinationMarker!!)
        }
        destinationMarker = map.addMarker(MarkerOptions().position(latLng))
        destinationPosition = Point.fromLngLat(latLng.longitude, latLng.latitude)
        originPosition = Point.fromLngLat(originLocation.longitude, originLocation.latitude)

        startNavigating.visibility = View.VISIBLE

        return false
    }
}
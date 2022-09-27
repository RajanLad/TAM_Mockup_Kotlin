package net.datetegy.tammockupkotlin

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.SearchView.SearchAutoComplete
import com.mapbox.android.core.location.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
//import com.mapbox.mapboxsdk.constants.Style
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.search.*
import com.mapbox.search.location.DefaultLocationProvider
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*


class MainActivity : AppCompatActivity(),OnMapReadyCallback, PermissionsListener,
    MapboxMap.OnMapClickListener {

    //,,
    //    OnMapReadyCallback


    /**
     * Draw a vector polygon on a map with the Mapbox Android SDK.
     */
    private val POINTS: MutableList<MutableList<Point>> = ArrayList()
    private val OUTER_POINTS_1: MutableList<Point> = ArrayList()

    private val TAG = "MainActivity"

    private val DEFAULT_INTERVAL_IN_MILLISECONDS = 10000L
    private val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS * 5

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

    private lateinit var searchAutoComplete: SearchAutoComplete

    var Arr3 = arrayOf<String>("Surat","Mumbai","Rajkot","Paris")

     lateinit var adapter: ArrayAdapter<String>


    private val searchCallback = object : SearchSelectionCallback {

        override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
            if (suggestions.isEmpty()) {
                Log.i("SearchApiExample", "No suggestions found")
                //Toast.makeText(this@MainActivity,"No suggestions found",Toast.LENGTH_LONG).show()
            } else {
                Log.i("SearchApiExample", "Search suggestions: $suggestions.\nSelecting first suggestion...")
                //Toast.makeText(this@MainActivity,"Search suggestions: $suggestions.\nSelecting first suggestion...",Toast.LENGTH_LONG).show()
                searchRequestTask = searchEngine.select(suggestions.first(), this)




                if(adapter!=null)
                for (ss in suggestions) {
                    adapter.add(ss.name)
                }


                adapter.notifyDataSetChanged()
                searchAutoComplete.threshold = 1;
                searchAutoComplete.setAdapter(adapter)

            }


            searchView.setOnSuggestionListener(object : SearchView.OnSuggestionListener {
                override fun onSuggestionSelect(position: Int): Boolean {
                    TODO("Not yet implemented")

                    latitude = suggestions.get(position)?.requestOptions.options.origin!!.latitude()
                    longitude = suggestions.get(position)?.requestOptions.options.origin!!.longitude()

                    mapView.getMapAsync { mapboxMap->
                        mapboxMap.getStyle { style->
                            // Create a SymbolManager.
                            val symbolManager = SymbolManager(mapView, mapboxMap, style)

                            // Set non-data-driven properties.
                            symbolManager.iconAllowOverlap = true
                            symbolManager.iconIgnorePlacement = true

                            // Create a symbol at the specified location.
                            val symbol = symbolManager.create(SymbolOptions()
                                .withLatLng(LatLng(latitude,
                                    longitude
                                ))
                                .withTextField(suggestions.get(position).name+" , "+suggestions.get(position).address?.locality?:"Not present")
                                .withIconImage("marker-15")
                                .withIconSize(2.3f))

                            //getRoute(originPosition,Point.fromLngLat(longitude, latitude))
                        }
                    }
                }

                override fun onSuggestionClick(position: Int): Boolean {
                    return true
                }
            })
        }

        override fun onResult(
            suggestion: SearchSuggestion,
            result: SearchResult,
            responseInfo: ResponseInfo
        ) {
            Log.e("SearchApiExample", "Search result: $result")
            //Toast.makeText(this@MainActivity,"Search result: $result",Toast.LENGTH_LONG).show()

            latitude = result.coordinate?.latitude()!!
            longitude = result.coordinate?.longitude()!!

            mapView.getMapAsync { mapboxMap->
                mapboxMap.getStyle { style->
                    // Create a SymbolManager.
                    val symbolManager = SymbolManager(mapView, mapboxMap, style)

                    // Set non-data-driven properties.
                    symbolManager.iconAllowOverlap = true
                    symbolManager.iconIgnorePlacement = true

                    // Create a symbol at the specified location.
                    val symbol = symbolManager.create(SymbolOptions()
                        .withLatLng(LatLng(latitude,
                            longitude
                        ))
                        .withTextField(result.name+" , "+result.address?.locality?:"Not present")
                        .withIconImage("marker-15")
                        .withIconSize(2.3f))

                    //getRoute(originPosition,Point.fromLngLat(longitude, latitude))
                }
            }

        }

        override fun onCategoryResult(
            suggestion: SearchSuggestion,
            results: List<SearchResult>,
            responseInfo: ResponseInfo
        ) {
            Log.i("SearchApiExample", "Category search results: $results")
            //Toast.makeText(this@MainActivity,"Category search results: $results",Toast.LENGTH_LONG).show()
        }

        override fun onError(e: Exception) {
            Log.i("SearchApiExample", "Search error", e)
            Toast.makeText(this@MainActivity,"Search error"+e.message,Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this,getString(R.string.access_token))

        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        searchView = findViewById(R.id.searchView)
        centerIt = findViewById(R.id.centerit)
        startNavigating = findViewById(R.id.start_navigation)
        startNavigating.visibility = View.GONE

        startNavigating.setOnClickListener(View.OnClickListener {
            startActivity(Intent(this,TurnByTurnActivity::class.java))
        })


        OUTER_POINTS_1.add(Point.fromLngLat(2.33222792259747, 48.83453819916022));
        OUTER_POINTS_1.add(Point.fromLngLat(2.326014285568315, 48.82281419852089));
        OUTER_POINTS_1.add(Point.fromLngLat(2.341572841279113, 48.81994719859191));
        OUTER_POINTS_1.add(Point.fromLngLat(2.3555168298878466, 48.83141421434005));
        POINTS.add(OUTER_POINTS_1);

        MapboxSearchSdk.initialize(this.application,getString(R.string.access_token),
            DefaultLocationProvider(this.application) )

        searchEngine = MapboxSearchSdk.createSearchEngine()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {

                return true;
            }

            override fun onQueryTextChange(query: String?): Boolean {
                searchRequestTask = searchEngine.search(
                    query!!,
                    SearchOptions(limit = 3),
                    searchCallback
                )
                return true;
            }
        })



        searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text)


        val stringList = ArrayList<String>()

//        val stringArray = stringList.toArray()
        var items = arrayOfNulls<String>(stringList.size)
        items = stringList.toArray(items)


        var Arr3 = arrayOf<String>("Surat","Mumbai","Rajkot","Paris")


        adapter =  ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, android.R.id.text1,
            stringList)

                searchAutoComplete.threshold = 1;
                searchAutoComplete.setAdapter(adapter)


//        searchRequestTask = searchEngine.search(
//            "Paris",
//            SearchOptions(limit = 3),
//            searchCallback
//        )

        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)


        centerIt.setOnClickListener(View.OnClickListener {
            setCameraPosition(originLocation)
        })
    }



    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
        if (locationEngine != null) {
            val request =
                LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                    .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                    .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME).build()
            locationEngine!!.requestLocationUpdates(
                request,
                object : LocationEngineCallback<LocationEngineResult> {
                    override fun onSuccess(result: LocationEngineResult) {
                        Toast.makeText(
                            this@MainActivity,
                            "requestLocationUpdates",
                            Toast.LENGTH_SHORT
                        ).show()
                        originLocation = result.lastLocation!!
                        setCameraPosition(result.lastLocation!!)
                    }

                    override fun onFailure(exception: Exception) {
                        Toast.makeText(
                            this@MainActivity,
                            "requestLocationUpdates fail",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                Looper.getMainLooper()
            )
        }
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
        searchRequestTask.cancel()
        super.onDestroy()
        mapView.onDestroy()
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

    @SuppressWarnings("MissingPermission")
    private fun initializeLocationEngine()
    {


        locationEngine = LocationEngineProvider.getBestLocationEngine(this)

        locationEngine?.getLastLocation(object:LocationEngineCallback<LocationEngineResult>{
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

                Toast.makeText(
                    this@MainActivity,
                    "requestLocationUpdates fail p0",
                    Toast.LENGTH_SHORT
                ).show()
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
                        Toast.makeText(
                            this@MainActivity,
                            "requestLocationUpdates fail",
                            Toast.LENGTH_SHORT
                        ).show()
//                        TODO("Not yet implemented")
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

    private fun setCameraPosition(location:Location)
    {
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
//
//
//
//

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

    override fun onMapClick(latLng: LatLng): Boolean {
        if (destinationMarker != null) {
            map.removeMarker(destinationMarker!!)
        }
        destinationMarker = map.addMarker(MarkerOptions().position(latLng))
        destinationPosition = Point.fromLngLat(latLng.longitude, latLng.latitude)
        originPosition = Point.fromLngLat(originLocation.longitude, originLocation.latitude)
        getRoute(originPosition, destinationPosition)

        startNavigating.visibility = View.VISIBLE

        return false
    }

    private fun getRoute(origin: Point?, destination: Point?) {
        if (origin != null && destination != null ) {
//            NavigationRoute.builder(this)
//                .accessToken(Mapbox.getAccessToken()!!)
//                .origin(origin)
//                .destination(destination)
//                .build()
//                .getRoute(object : Callback<DirectionsResponse?> {
//                    override fun onResponse(
//                        call: Call<DirectionsResponse?>,
//                        response: Response<DirectionsResponse?>
//                    ) {
//                        if (response.body() == null) {
//                            Log.e(TAG, "onResponse: No routes found. ")
//                            return
//                        } else if (response.body()!!.routes().size == 0) {
//                            Log.e(TAG, "onResponse: No routes found. ")
//                            return
//                        }
//                        val currentRoute = response.body()!!.routes()[0]
//                        //                                drawRoute(response.body().routes().get(0));
//                        if (navigationMapRoute != null) {
//                            navigationMapRoute!!.removeRoute()
//                        } else {
//                            navigationMapRoute = NavigationMapRoute(null, mapView, map)
//                        }
//                        navigationMapRoute!!.addRoute(currentRoute)
//                    }
//
//                    override fun onFailure(call: Call<DirectionsResponse?>, t: Throwable) {
//                        Log.e(TAG, "onError : " + t.message)
//                    }
//                })

//            RouteOptions.
        }
    }



}
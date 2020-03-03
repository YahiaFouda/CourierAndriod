package com.kadabra.courier.task

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.directions.route.*
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.places.ui.PlacePicker.IntentBuilder
import com.google.android.gms.location.places.ui.PlacePicker.getPlace
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.kadabra.courier.R
import com.kadabra.courier.direction.FetchURL
import com.kadabra.courier.direction.TaskLoadedCallback
import com.kadabra.courier.utilities.AppConstants
import kotlinx.android.synthetic.main.activity_location_details.*
import com.google.android.gms.maps.model.LatLngBounds
import com.kadabra.courier.location.LatLngInterpolator
import com.kadabra.courier.location.MarkerAnimation


class LocationDetailsActivity : AppCompatActivity(), OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener, RoutingListener, TaskLoadedCallback, LocationListener {


    //region Members

    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false
    private lateinit var polylines: List<Polyline>
    private var currentPolyline: Polyline? = null
    private var isFirstTime = true
    private lateinit var destination: LatLng
    private var currentMarker: Marker? = null

    private val COLORS: IntArray = intArrayOf(
        R.color.colorPrimary,
        R.color.primary,
        R.color.primary_light,
        R.color.accent,
        R.color.primary_dark_material_light
    )


    //endregion

    companion object {

        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val REQUEST_CHECK_SETTINGS = 2
        private const val PLACE_PICKER_REQUEST = 3

    }

    //region Helper Function
    private fun init() {

        polylines = ArrayList()

        fab.setOnClickListener {
            loadPlacePicker()
        }
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //prepare for update the current location
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                //todo move the marker position

                if (isFirstTime) {

                    if (AppConstants.currentSelectedStop != null) {
                        var selectedStopLocation = LatLng(
                            AppConstants.currentSelectedStop.Latitude!!,
                            AppConstants.currentSelectedStop.Longitude!!
                        )

                        placeMarkerOnMap(
                            selectedStopLocation,
                            AppConstants.currentSelectedStop.StopName
                        )

                        try {
                            if (lastLocation != null) {

                                destination = LatLng(
                                    AppConstants.currentSelectedStop.Latitude!!,
                                    AppConstants.currentSelectedStop.Longitude!!
                                )
//                                drawRouteOnMap(
//                                    LatLng(lastLocation!!.latitude, lastLocation!!.longitude),
//                                    destination
//                                )
                                FetchURL(this@LocationDetailsActivity).execute(
                                    getUrl(
                                        LatLng(lastLocation!!.latitude, lastLocation!!.longitude),
                                        destination,
                                        "driving"
                                    ), "driving"
                                )
                            }

                        } catch (ex: ExceptionInInitializerError) {

                        }

                    }

                    val builder = LatLngBounds.Builder()
                    builder.include(LatLng(lastLocation!!.latitude, lastLocation!!.longitude))
                    builder.include(LatLng(destination.latitude, destination.longitude))
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngBounds(
                            builder.build(), 25, 25, 0
                        )
                    )
                    isFirstTime = false
                    builder.include(LatLng(destination.latitude, destination.longitude))
                    destination = LatLng(0.0, 0.0)
                }

            }
        }
        createLocationRequest()


        ivBack.setOnClickListener {
            finish()
        }
    }

    private fun setUpMap() {

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED)
        {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST_CODE
                )
            }
        } else {
            // Permission has already been granted
        }

        map.isMyLocationEnabled = true
        map.mapType = GoogleMap.MAP_TYPE_TERRAIN //more map details

        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->


            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                placeMarkerOnMap(currentLatLng, getString(R.string.you_are_here))

            }
        }
        return

    }

    private fun placeMarkerOnMap(location: LatLng, title: String) {
        // 1
        val markerOptions = MarkerOptions().position(location)
        //change marker icon
        markerOptions.icon(
            BitmapDescriptorFactory.fromBitmap(
                BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            )
        ).title(title)

        //show address of the current location
//        val titleStr = getAddress(location)  // add these two lines
//        markerOptions.title(titleStr)
        // 2
        map.addMarker(markerOptions)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))
    }

    private fun drawRouteOnMap(sourceLocation: LatLng, destinationLocation: LatLng) {
        var routing = Routing.Builder()
            .travelMode(AbstractRouting.TravelMode.DRIVING)
            .withListener(this)
            .alternativeRoutes(true)
            .waypoints(sourceLocation, destinationLocation)
            .build()
        routing.execute()
    }


    //update the current location
    private fun startLocationUpdates() {
        //1
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        //2
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
    }

    private fun createLocationRequest() {
        // 1
        locationRequest = LocationRequest()
        // 2
        locationRequest.interval = 1000
        // 3
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // 4
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // 5
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            // 6
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(
                        this,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    // places
    private fun loadPlacePicker() {
        val builder = IntentBuilder()

        try {
            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST)
        } catch (e: GooglePlayServicesRepairableException) {
            e.printStackTrace()
        } catch (e: GooglePlayServicesNotAvailableException) {
            e.printStackTrace()
        }
    }
    //endregion

    //region Events
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        init()


    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener(this)

        setUpMap()
    }

    override fun onMarkerClick(p0: Marker?): Boolean {

        return false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        erasePolyLinesFromMap()
        finish()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    setUpMap()
                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request.
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
        if (requestCode == PLACE_PICKER_REQUEST) {
            if (resultCode == RESULT_OK) {
                val place = getPlace(this, data)
                var addressText = place.name.toString()
                addressText += "\n" + place.address.toString()

                placeMarkerOnMap(place.latLng, AppConstants.currentSelectedStop.StopName)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
        }
    }

    override fun onRoutingCancelled() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onRoutingStart() {

    }

    override fun onRoutingFailure(e: RouteException?) {
        if (e != null) {
            Toast.makeText(this, "Error: " + e.message, Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRoutingSuccess(routes: ArrayList<Route>?, shourtestRouteIndex: Int) {

        if (polylines!!.isNotEmpty()) {
            for (poly in polylines!!) {
                poly.remove()
            }
        }

        polylines = ArrayList()
        for (i in routes!!.indices) {
            var colorIndex = i % COLORS.size
            var polyOptions = PolylineOptions()
            polyOptions.color(resources.getColor(COLORS[colorIndex]))
            polyOptions.width((10 + i * 3).toFloat())
            polyOptions.addAll(routes[i].points)
            var polyline = map.addPolyline(polyOptions)
            (polylines as ArrayList<Polyline>).add(polyline)

        }


    }

    override fun onTaskDone(vararg values: Any?) {
        if (currentPolyline != null)
            currentPolyline!!.remove()
        currentPolyline = map.addPolyline(values[0] as PolylineOptions)

        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    currentPolyline!!.points.get(0).latitude,
                    currentPolyline!!.points.get(0).longitude
                ), 12f
            )
        )
    }

    private fun erasePolyLinesFromMap() {
        for (polyline in polylines) {
            polyline.remove()
        }
        polylines = ArrayList()
    }

    private fun getUrl(origin: LatLng, dest: LatLng, directionMode: String): String {
        // Origin of route
        val str_origin = "origin=" + origin.latitude + "," + origin.longitude
        // Destination of route
        val str_dest = "destination=" + dest.latitude + "," + dest.longitude
        // Mode
        val mode = "mode=$directionMode"
        // Building the parameters to the web service
        val parameters = "$str_origin&$str_dest&$mode"
        // Output format
        val output = "json"
        // Building the url to the web service
        return "https://maps.googleapis.com/maps/api/directions/$output?$parameters&key=" + getString(
            R.string.google_maps_key
        )
    }

    private fun animateCamera(latLng: LatLng) {

        val zoom = map.cameraPosition.zoom
        map.animateCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(
                    latLng,
                    zoom
                )
            )
        )
    }

    private fun moveCamera(location: Location) {
        var latLng = LatLng(location.latitude, location.longitude)
        val zoom = map.cameraPosition.zoom
        map.moveCamera(
            CameraUpdateFactory.newCameraPosition(
                CameraPosition.fromLatLngZoom(
                    latLng,
                    zoom
                )
            )
        )
    }

    private fun showMarker(latLng: LatLng) {
        if (currentMarker == null) {
            val markerOptions = MarkerOptions().position(latLng)
            //change marker icon
            markerOptions.icon(
                BitmapDescriptorFactory.fromBitmap(
                    BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
                )
            )
            currentMarker = map.addMarker(markerOptions)


        } else
            MarkerAnimation.animateMarkerToGB(
                currentMarker,
                latLng,
                LatLngInterpolator.Spherical()
            )
    }


    override fun onLocationChanged(p0: Location?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

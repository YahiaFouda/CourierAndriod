package com.kadabra.courier.task

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.location.places.ui.PlacePicker.IntentBuilder
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.internal.PolylineEncoding
import com.google.maps.model.DirectionsResult
import com.kadabra.Networking.INetworkCallBack
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.BuildConfig
import com.kadabra.courier.R
import com.kadabra.courier.api.ApiResponse
import com.kadabra.courier.api.ApiServices
import com.kadabra.courier.base.BaseNewActivity
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.location.LatLngInterpolator
import com.kadabra.courier.location.LocationHelper
import com.kadabra.courier.location.MarkerAnimation
import com.kadabra.courier.model.CalculateFees
import com.kadabra.courier.model.PolylineData
import com.kadabra.courier.model.Stop
import com.kadabra.courier.model.Task
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.Screenshot
import com.kadabra.courier.utilities.UtilHelper
import com.reach.plus.admin.util.UserSessionManager
import kotlinx.android.synthetic.main.activity_location_details.*
import java.io.OutputStream


class TaskLocationsActivity : BaseNewActivity(), OnMapReadyCallback,
    GoogleMap.OnMarkerClickListener, GoogleMap.OnPolylineClickListener, View.OnClickListener {


    //region Members

    private var TAG = TaskLocationsActivity.javaClass.simpleName

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
    private var mGeoApiContext: GeoApiContext? = null
    private var mPolyLinesData: ArrayList<PolylineData> = ArrayList()
    private val mTripMarkers = ArrayList<Marker>()
    private var mSelectedMarker: Marker? = null
    private var totalKilometers: Float = 0F
    private lateinit var polyline: Polyline
    var isACcepted = false
    private lateinit var directionResult: DirectionsResult
    private lateinit var mapFragment:SupportMapFragment
    //endregion


    companion object {

        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val REQUEST_CHECK_SETTINGS = 2
        private const val PLACE_PICKER_REQUEST = 3

    }

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.ivBack -> {
                AppConstants.currentSelectedStop = Stop()
                finish()
            }

            R.id.fab -> {
                loadPlacePicker()
            }

            R.id.btnStart -> {
//                if (!AppConstants.COURIERSTARTTASK) {
                isACcepted =
                    AppConstants.CurrentSelectedTask.IsStarted//isStartedTask(AppConstants.CurrentSelectedTask)
                if (!isACcepted) { // not started yet
                    btnStart.text = getString(R.string.start_task)
                    var firstStop = AppConstants.CurrentAcceptedTask.stopsmodel.first()
                    var lastStop = AppConstants.CurrentAcceptedTask.stopsmodel.last()
                    var pickUp = LatLng(
                        firstStop.Latitude!!,
                        firstStop.Longitude!!
                    )
                    var dropOff = LatLng(
                        lastStop.Latitude!!,
                        lastStop.Longitude!!
                    )

                    if (NetworkManager().isNetworkAvailable(this)) {
                        calculateTwoDirections(pickUp, dropOff)
                        AppConstants.CurrentSelectedTask.IsStarted=true
                    } else
                        Alert.showMessage(
                            this@TaskLocationsActivity,
                            getString(R.string.no_internet)
                        )

                } else {
                    btnStart.text = getString(R.string.end_task)
                    endTask(AppConstants.CurrentSelectedTask)
                }
            }
        }
    }


    //region Helper Function
    private fun init() {

        ivBack.setOnClickListener(this)
        fab.setOnClickListener(this)
        btnStart.setOnClickListener(this)

        polylines = ArrayList()


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
         mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        //direction
        if (mGeoApiContext == null) {
            mGeoApiContext = GeoApiContext.Builder()
                .apiKey(getString(R.string.google_map_key))
                .build()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //prepare for update the current location
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation

                if (isFirstTime) {

                    if (AppConstants.currentSelectedStop != null && !AppConstants.currentSelectedStop.StopID.isNullOrEmpty()) { // destination stop
                        btnStart.visibility = View.GONE
                        if (lastLocation != null && !AppConstants.currentSelectedStop.StopID.isNullOrEmpty()) {

                            destination = LatLng(
                                AppConstants.currentSelectedStop.Latitude!!,
                                AppConstants.currentSelectedStop.Longitude!!
                            )


                            if (lastLocation?.latitude != null && lastLocation?.longitude != null) {
                                if (NetworkManager().isNetworkAvailable(this@TaskLocationsActivity)) {
                                    map.uiSettings.setAllGesturesEnabled(false)
                                    map.uiSettings.isScrollGesturesEnabled = false
                                    map.getUiSettings().setZoomGesturesEnabled(false)
                                    mapFragment.view?.isClickable=false
                                    mapFragment.view?.isFocusable=false

                                    calculateDirections(
                                        LatLng(
                                            lastLocation?.latitude!!,
                                            lastLocation?.longitude!!
                                        ), destination
                                    )
                                } else
                                    Alert.showMessage(
                                        this@TaskLocationsActivity,
                                        getString(R.string.no_internet)
                                    )


                            }
                        }

                    } else if (intent.getBooleanExtra(
                            "startTask",
                            true
                        )
                    ) // Courier  start  journey from details view
                    {
                        if (NetworkManager().isNetworkAvailable(this@TaskLocationsActivity)) {
                            btnStart.visibility = View.VISIBLE
                            var firstStop = AppConstants.CurrentAcceptedTask.stopsmodel.first()
                            var lastStop = AppConstants.CurrentAcceptedTask.stopsmodel.last()
                            var pickUp = LatLng(
                                firstStop.Latitude!!,
                                firstStop.Longitude!!
                            )
                            var dropOff = LatLng(
                                lastStop.Latitude!!,
                                lastStop.Longitude!!
                            )

                            calculateDirections(pickUp, dropOff)
                            btnStart.text = getString(R.string.start_task)
                        } else
                            Alert.showMessage(
                                this@TaskLocationsActivity,
                                getString(R.string.no_internet)
                            )
                    }



                    isFirstTime = false
                    destination = LatLng(0.0, 0.0)
                }

            }
        }
        createLocationRequest()


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

        // 2
        map.addMarker(markerOptions)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 12f))
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


        isACcepted =
            AppConstants.CurrentSelectedTask.IsStarted// isStartedTask(AppConstants.CurrentSelectedTask)
        init()


    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.clear()
        map.isMyLocationEnabled = true
        map.mapType = GoogleMap.MAP_TYPE_TERRAIN //more map details
        map.uiSettings.isZoomControlsEnabled = true
        map.setOnMarkerClickListener(this)
        map.setOnPolylineClickListener(this)


        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->

            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                lastLocation = location
            }
        }


    }

    override fun onMarkerClick(p0: Marker?): Boolean {

        return false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        erasePolyLinesFromMap()
        AppConstants.currentSelectedStop = Stop()
        finish()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == AppConstants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.size <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
//                LocationUpdatesService.shared!!.requestLocationUpdates()
                map.isMyLocationEnabled = true
            } else {
                if (UserSessionManager.getInstance(this).requestingLocationUpdates()) {
                    if (!checkPermissions()) {
                        requestPermissions()
                    }
                }
            }
        }
        if (!LocationHelper.shared.isGPSEnabled())
            Snackbar.make(
                findViewById(R.id.rlParent),
                R.string.permission_denied_explanation,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    // Build intent that displays the App settings screen.
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts(
                        "package",
                        BuildConfig.APPLICATION_ID, null
                    )
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                }
                .show()
    }


    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }


    override fun onStart() {
        super.onStart()
        if (!checkPermissions()) {
            requestPermissions()
        }
    }

    public override fun onResume() {
        super.onResume()

    }


    private fun erasePolyLinesFromMap() {
        for (polyline in polylines) {
            polyline.remove()
        }
        polylines = ArrayList()
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

    fun isServicesOK(): Boolean {
        Log.d(TAG, "isServicesOK: checking google services version")

        val available =
            GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

        if (available == ConnectionResult.SUCCESS) {
            //everything is fine and the user can make map requests
            Log.d(TAG, "isServicesOK: Google Play Services is working")
            return true
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(available)) {
            //an error occured but we can resolve it
            Log.d(TAG, "isServicesOK: an error occured but we can fix it")
            val dialog = GoogleApiAvailability.getInstance()
                .getErrorDialog(this, available, AppConstants.ERROR_DIALOG_REQUEST)
            dialog.show()
        } else {
            Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show()
        }
        return false
    }

    fun isMapsEnabled(): Boolean {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
            return false
        }
        return true
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, id ->
                val enableGpsIntent =
                    Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(enableGpsIntent, AppConstants.PERMISSIONS_REQUEST_ENABLE_GPS)
            }
        val alert = builder.create()
        alert.show()
    }


    // Returns the current state of the permissions needed
    private fun checkPermissions(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i(TAG, "Displaying permission rationale to provide additional context.")
            Snackbar.make(
                findViewById(R.id.rlParent),
                R.string.permission_rationale,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.ok) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this@TaskLocationsActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        AppConstants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                    )
                }
                .show()
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                this@TaskLocationsActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                AppConstants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }


//    override fun onInfoWindowClick(p0: Marker?) {
////
//    }

    private fun calculateDirections(origin: LatLng, dest: LatLng) {
//        Alert.showProgress(this)
        Log.d(
            TAG,
            "calculateDirections: calculating directions."
        )
        val destination = com.google.maps.model.LatLng(
            dest.latitude,
            dest.longitude
        )
        val directions = DirectionsApiRequest(mGeoApiContext)
        directions.alternatives(true)
        directions.origin(
            com.google.maps.model.LatLng(
                origin.latitude,
                origin.longitude
            )
        )
        Log.d(
            TAG,
            "calculateDirections: destination: $destination"
        )
        directions.destination(destination)
            .setCallback(object : PendingResult.Callback<DirectionsResult?> {
                override fun onResult(result: DirectionsResult?) { //                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
//                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
//                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
//                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());
                    Log.d(
                        TAG,
                        "onResult: successfully retrieved directions."
                    )
                    val meters = result!!.routes[0].legs[0].distance.inMeters
//                    if(meters>=1000)
//                      totalKilometers=  meters  /1000.0f
//                    else
                    totalKilometers = conevrtMetersToKilometers(meters)

                    Log.d("DISTANCE1", result!!.routes[0].legs[0].distance.inMeters.toString())
                    Log.d("DISTANCE1", totalKilometers.toString())
                    addPolylinesToMap(result!!)
//                    Alert.hideProgress()
                }

                override fun onFailure(e: Throwable) {
//                    Alert.hideProgress()
                    Log.e(
                        TAG,
                        "calculateDirections: Failed to get directions: " + e.message
                    )
                }
            })
    }

    private fun calculateTotalDirections(): Float {
        var totalTime = 0
        var totalKm = 0F
        Log.d(
            TAG,
            "calculateDirections: calculating directions."
        )

        for (i in AppConstants.CurrentSelectedTask.stopsmodel) {

        }

        AppConstants.CurrentSelectedTask.stopsmodel.forEach {

            var origin = LatLng(it.Latitude!!, it.Longitude!!)
            var dest = LatLng(it.Latitude!!, it.Longitude!!)

            val destination = com.google.maps.model.LatLng(
                dest.latitude,
                dest.longitude
            )
            val directions = DirectionsApiRequest(mGeoApiContext)
            directions.alternatives(true)
            directions.origin(
                com.google.maps.model.LatLng(
                    origin.latitude,
                    origin.longitude
                )
            )
            Log.d(
                TAG,
                "calculateDirections: destination: $destination"
            )
            directions.destination(destination)
                .setCallback(object : PendingResult.Callback<DirectionsResult?> {
                    override fun onResult(result: DirectionsResult?) { //                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
//                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
//                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
//                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());
                        Log.d(
                            TAG,
                            "onResult: successfully retrieved directions."
                        )


                        totalKilometers += result!!.routes[0].legs[0].distance.inMeters / 1000
                    }

                    override fun onFailure(e: Throwable) {
                        Log.e(
                            TAG,
                            "calculateDirections: Failed to get directions: " + e.message
                        )
                    }
                })

        }
        return totalKm
    }

    private fun calculateTwoDirections(origin: LatLng, dest: LatLng): Float {
        Alert.showProgress(this)
        Log.d(
            TAG,
            "calculateDirections: calculating directions."
        )
        val destination = com.google.maps.model.LatLng(
            dest.latitude,
            dest.longitude
        )
        val directions = DirectionsApiRequest(mGeoApiContext)
        directions.alternatives(true)
        directions.origin(
            com.google.maps.model.LatLng(
                origin.latitude,
                origin.longitude
            )
        )
        Log.d(
            TAG,
            "calculateDirections: destination: $destination"
        )
        directions.destination(destination)
            .setCallback(object : PendingResult.Callback<DirectionsResult?> {
                override fun onResult(result: DirectionsResult?) { //                Log.d(TAG, "calculateDirections: routes: " + result.routes[0].toString());
//                Log.d(TAG, "calculateDirections: duration: " + result.routes[0].legs[0].duration);
//                Log.d(TAG, "calculateDirections: distance: " + result.routes[0].legs[0].distance);
//                Log.d(TAG, "calculateDirections: geocodedWayPoints: " + result.geocodedWaypoints[0].toString());
                    directionResult = result!!
                    Log.d(
                        TAG,
                        "onResult: successfully retrieved directions."
                    )
                    var meters = result!!.routes[0].legs[0].distance.inMeters
                    totalKilometers = conevrtMetersToKilometers(meters)

//                    totalKilometers += result!!.routes[0].legs[0].distance.inMeters / 1000
                    if (totalKilometers > 0.0)
                        startTrip(AppConstants.CurrentSelectedTask, totalKilometers)
                    else {
                        Alert.hideProgress()
                        Alert.showMessage(this@TaskLocationsActivity, "press start again.")
                    }

                }

                override fun onFailure(e: Throwable) {
                    Alert.hideProgress()
                    Log.e(
                        TAG,
                        "calculateDirections: Failed to get directions: " + e.message
                    )
                }
            })

        return totalKilometers
    }


    private fun addPolylinesToMap(result: DirectionsResult) {

        Handler(Looper.getMainLooper()).post {
            Log.d(
                TAG,
                "run: result routes: " + result.routes.size
            )
            if (mPolyLinesData.size > 0) {
                for (polylineData in mPolyLinesData) {
                    polylineData.getPolyline().remove()
                }
                mPolyLinesData.clear()
                mPolyLinesData = java.util.ArrayList<PolylineData>()
            }
            var duration = 999999999.0
            for (route in result.routes) {
                Log.d(
                    TAG,
                    "run: leg: " + route.legs[0].toString()
                )
                val decodedPath =
                    PolylineEncoding.decode(route.overviewPolyline.encodedPath)
                val newDecodedPath: MutableList<LatLng> =
                    java.util.ArrayList()
                // This loops through all the LatLng coordinates of ONE polyline.
                for (latLng in decodedPath) { //                        Log.d(TAG, "run: latlng: " + latLng.toString());
                    newDecodedPath.add(
                        LatLng(
                            latLng.lat,
                            latLng.lng
                        )
                    )
                }
                polyline =
                    map.addPolyline(PolylineOptions().addAll(newDecodedPath)) // add marker
                polyline.color = ContextCompat.getColor(this, R.color.colorPrimary)
                polyline.isClickable = true
                mPolyLinesData.add(PolylineData(polyline, route.legs[0]))
                // highlight the fastest route and adjust camera
                val tempDuration =
                    route.legs[0].duration.inSeconds.toDouble()
                if (tempDuration < duration) {
                    duration = tempDuration
                    onPolylineClick(polyline)
                    zoomRoute(polyline.points)
                    setTripDirectionData(PolylineData(polyline, route.legs[0]))
                    rlBottom.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onPolylineClick(polyline: Polyline?) {
        var index = 0
        for (polylineData in mPolyLinesData) {
            index++
            Log.d(
                TAG,
                "onPolylineClick: toString: $polylineData"
            )
            if (polyline!!.id == polylineData.polyline.id) {
                polylineData.polyline.color = ContextCompat.getColor(this, R.color.primary_dark)
                polylineData.polyline.setZIndex(1F)
                val endLocation =
                    LatLng(
                        polylineData.leg.endLocation.lat,
                        polylineData.leg.endLocation.lng
                    )

                setTripDirectionData(polylineData)

                val marker: Marker = map.addMarker(
                    MarkerOptions()
                        .icon(bitmapDescriptorFromVector(this, R.drawable.ic_location))
                        .position(endLocation)
                        .title("[" + getString(R.string.trip) + " " + index + "] - " + AppConstants.currentSelectedStop.StopName)
                        .snippet(
                            getString(R.string.duration) + " " + polylineData.leg.duration + " " + (getString(
                                R.string.distance
                            ) + " " + polylineData.leg.distance)
                        )


                )
                mTripMarkers.add(marker)
                marker.showInfoWindow()
            } else {
                polylineData.polyline.color =
                    ContextCompat.getColor(this, R.color.colorPrimary)
                polylineData.polyline.setZIndex(0F)
            }
        }
    }

    fun zoomRoute(lstLatLngRoute: List<LatLng?>?) {
        if (map == null || lstLatLngRoute == null || lstLatLngRoute.isEmpty()) return
        val boundsBuilder = LatLngBounds.Builder()
        for (latLngPoint in lstLatLngRoute) boundsBuilder.include(
            latLngPoint
        )
        val routePadding = 50
        val latLngBounds = boundsBuilder.build()
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
            600,
            null
        )
    }

    private fun resetMap() {
        if (map != null) {
            map.clear()
            if (mPolyLinesData.size > 0) {
                mPolyLinesData.clear()
                mPolyLinesData = java.util.ArrayList()
            }
        }
    }

    private fun resetSelectedMarker() {
        if (mSelectedMarker != null) {
            mSelectedMarker!!.isVisible = true
            mSelectedMarker = null
            removeTripMarkers()
        }
    }

    private fun removeTripMarkers() {
        for (marker in mTripMarkers) {
            marker.remove()
        }
    }

    //convert vecor to bitmap
    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap =
                Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    private fun setTripDirectionData(polylineData: PolylineData) {
        tvExpectedTime.text = polylineData.leg.duration.toString()
        tvExpectedDistance.text =
            "( " + polylineData.leg.distance.toString() + "  )"
    }


    private fun startTrip(task: Task, totalKilometers: Float) {
        if (NetworkManager().isNetworkAvailable(this)) {

            if (!task.TaskId.isNullOrEmpty()) {
                var request = NetworkManager().create(ApiServices::class.java)
                var endPoint = request.updateTaskCourierFees(task.TaskId, totalKilometers)
                NetworkManager().request(
                    endPoint,
                    object : INetworkCallBack<ApiResponse<CalculateFees>> {
                        override fun onFailed(error: String) {
                            Alert.hideProgress()
                            Alert.showMessage(
                                this@TaskLocationsActivity,
                                getString(R.string.error_login_server_unknown_error)
                            )
                        }

                        override fun onSuccess(response: ApiResponse<CalculateFees>) {
                            if (response.Status == AppConstants.STATUS_SUCCESS) {
//                                zoomRoute(polyline.points)
                                captureScreen(map, AppConstants.CurrentSelectedTask.TaskId)
                                AppConstants.CurrentAcceptedTask = task
                                AppConstants.CurrentSelectedTask = task

                                startTaskFirebase(task, AppConstants.CurrentLoginCourier.CourierId)

                                AppConstants.COURIERSTARTTASK = true
                                Alert.hideProgress()
                                btnStart.text = getString(R.string.end_task)
                            } else {
                                Alert.hideProgress()
                                Alert.showMessage(
                                    this@TaskLocationsActivity,
                                    getString(R.string.error_network)
                                )
                            }

                        }
                    })
            }

        } else {
            Alert.hideProgress()
            Alert.showMessage(
                this@TaskLocationsActivity,
                getString(R.string.no_internet)
            )
        }


    }

    private fun startTaskFirebase(task: Task, courierId: Int) {
        if (NetworkManager().isNetworkAvailable(this)) {
            FirebaseManager.updateCourierStartTask(AppConstants.CurrentLoginCourier.CourierId, true)
            btnStart.text = getString(R.string.end_task)

        } else {

            Alert.showMessage(
                this@TaskLocationsActivity,
                getString(R.string.no_internet)
            )
        }


    }

    private fun endTask(task: Task) {
        Alert.showProgress(this)
        if (NetworkManager().isNetworkAvailable(this)) {
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.endTask(task.TaskId)
            NetworkManager().request(endPoint, object : INetworkCallBack<ApiResponse<Task>> {
                override fun onFailed(error: String) {
                    Alert.hideProgress()
                    Alert.showMessage(
                        this@TaskLocationsActivity,
                        getString(R.string.error_login_server_unknown_error)
                    )
                }

                override fun onSuccess(response: ApiResponse<Task>) {
                    if (response.Status == AppConstants.STATUS_SUCCESS) {

                        FirebaseManager.endTask(
                            AppConstants.CurrentSelectedTask,
                            AppConstants.CurrentLoginCourier.CourierId
                        )

                        AppConstants.CurrentAcceptedTask = Task()
                        AppConstants.CurrentSelectedTask = Task()
                        AppConstants.COURIERSTARTTASK = false
                        AppConstants.ALL_TASKS_DATA.remove(AppConstants.CurrentSelectedTask) //removed when life cycle

                        AppConstants.endTask = true
                        //load new task or shoe empty tasks view
                        Alert.hideProgress()
                        startActivity(Intent(this@TaskLocationsActivity, TaskActivity::class.java))
                        finish()

                    } else {
                        Alert.hideProgress()
                        Alert.showMessage(
                            this@TaskLocationsActivity,
                            getString(R.string.error_network)
                        )
                    }

                }
            })

        } else {
            Alert.hideProgress()
            Alert.showMessage(
                this@TaskLocationsActivity,
                getString(R.string.no_internet)
            )
        }


    }


    private fun conevrtMetersToKilometers(meters: Long): Float {
        var kilometers = 0F
        kilometers = (meters * 0.001).toFloat()

        return kilometers
    }

    fun captureScreen(mMap: GoogleMap, taskId: String) {
//        var bit=Screenshot.takeScreenshotOfRootView(rlParent)
//        UtilHelper.uploadFile(bit,taskId)
        val callback =
            SnapshotReadyCallback { snapshot ->

                val bitmap = snapshot
                UtilHelper.uploadFile(bitmap, taskId)
                map.uiSettings.setAllGesturesEnabled(true)
                map.uiSettings.isScrollGesturesEnabled = true
                map.getUiSettings().setZoomGesturesEnabled(true)


                mapFragment.view?.isClickable=true
                mapFragment.view?.isFocusable=true
            }
        mMap.snapshot(callback)
    }
}

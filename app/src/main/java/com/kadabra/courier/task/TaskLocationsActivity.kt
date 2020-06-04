package com.kadabra.courier.task

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.*
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
import com.kadabra.courier.googleDirection.Directions
import com.kadabra.courier.googleDirection.PolylineDataNew
import com.kadabra.courier.location.LatLngInterpolator
import com.kadabra.courier.location.LocationHelper
import com.kadabra.courier.location.MarkerAnimation
import com.kadabra.courier.model.*
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.AppController
import com.kadabra.courier.utilities.UtilHelper
import com.reach.plus.admin.util.UserSessionManager
import kotlinx.android.synthetic.main.activity_location_details.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.ceil


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
    private var mPolyLinesDataNew: ArrayList<PolylineDataNew> = ArrayList()
    private val mTripMarkers = ArrayList<Marker>()
    private var mSelectedMarker: Marker? = null
    private var totalKilometers = 0
    var totalDistanceValue = ""
    private var totalDuration: Float = 0F
    var totalDistance = 0L
    var totalSeconds = 0L
    private lateinit var polyline: Polyline
    var isACcepted = false
    private lateinit var directionResult: DirectionsResult
    private lateinit var mapFragment: SupportMapFragment
    var waypoints: java.util.ArrayList<LatLng> = java.util.ArrayList()
    var meters = 0L
    var tripData = TripData()
    var snippetData = ""
    private var alertDialog: AlertDialog? = null
    private lateinit var tvReceiptDetails: TextView
    private lateinit var rbCash: RadioButton
    private lateinit var rbCredit: RadioButton
    private lateinit var rbWallet: RadioButton
    private lateinit var rbNoCollection: RadioButton
    private lateinit var ivPaymentBack: ImageView
    private lateinit var etAmount: EditText
    private lateinit var btnPaymentEndTask: Button
    private var paymentTypeView: View? = null
    private var totalReceiptValue = 0.0
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

            R.id.btnStart -> {

                //empty the previous selected Stop
                AppConstants.currentSelectedStop = Stop()
                isACcepted =
                    AppConstants.CurrentSelectedTask.IsStarted//isStartedTask(AppConstants.CurrentSelectedTask)
                if (!isACcepted || !AppConstants.CurrentEditedTask.TaskId.isNullOrEmpty()) { // not started yet
                    btnStart.text = getString(R.string.start_task)
                    var firstStop =
                        AppConstants.CurrentAcceptedTask.stopsmodel.find { it.StopTypeID == 1 }
                    var lastStop =
                        AppConstants.CurrentAcceptedTask.stopsmodel.find { it.StopTypeID == 2 }
                    var pickUp = LatLng(
                        firstStop?.Latitude!!,
                        firstStop?.Longitude!!
                    )
                    var dropOff = LatLng(
                        lastStop?.Latitude!!,
                        lastStop?.Longitude!!
                    )

                    if (NetworkManager().isNetworkAvailable(this)) {
//                        calculateTwoDirections(pickUp, dropOff)
                        calcTripDirection(pickUp, dropOff)
                    } else
                        Alert.showMessage(
                            this@TaskLocationsActivity,
                            getString(R.string.no_internet)
                        )

                } else {
                    btnStart.text = getString(R.string.end_task)
//                    endTask(AppConstants.CurrentSelectedTask)
                    if (alertDialog != null)
                        alertDialog?.show()
                    else {
                        choosePaymentTypeWindow()
                        alertDialog?.show()
                    }

                }
            }

            R.id.ivPaymentBack -> {
                if (alertDialog != null)
                    alertDialog!!.dismiss()
            }
            R.id.btnPaymentEndTask -> {
                btnPaymentEndTask.isEnabled = false
                if (!rbCash.isChecked && !rbWallet.isChecked && !rbCredit.isChecked && !rbNoCollection.isChecked) {
                    Alert.showMessage(getString(R.string.message_choose))
                    btnPaymentEndTask.isEnabled = true
                } else if (rbCash.isChecked && !etAmount.text.isNullOrEmpty() && etAmount.text.toString().toDouble() > 0.0) {
                    var amount = etAmount.text.toString().toDouble()
                    if (amount <= 0)
                        Alert.showMessage(getString(R.string.error_agent_phone))
                    else
                        endTask(
                            AppConstants.CurrentSelectedTask,
                            1,
                            amount
                        )
                } else if (rbWallet.isChecked) {
                    endTask(
                        AppConstants.CurrentSelectedTask,
                        2,
                        0.0
                    )
                } else if (rbCredit.isChecked) {
                    endTask(
                        AppConstants.CurrentSelectedTask,
                        3,
                        0.0
                    )
                } else if (rbNoCollection.isChecked) {
                    endTask(
                        AppConstants.CurrentSelectedTask,
                        4,
                        0.0
                    )
                } else
                    Alert.showMessage(getString(R.string.error_enter_total))
            }
            R.id.rbCash -> {
                etAmount.visibility = View.VISIBLE
            }
            R.id.rbCredit, R.id.rbWallet, R.id.rbNoCollection -> {
                resetAmount()
            }
            R.id.tvReceiptDetails -> {
                //get receipt Data
                getReceiptData(AppConstants.CurrentSelectedTask.TaskId)
            }


        }
    }

    private fun resetAmount() {
        etAmount.visibility = View.INVISIBLE
        etAmount.text.clear()
        etAmount.hint = getString(R.string.le)
    }

    //region Helper Function
    private fun init() {

        paymentTypeView = View.inflate(this, R.layout.payment_type_layout, null)
        ivPaymentBack = paymentTypeView!!.findViewById(R.id.ivPaymentBack)
        rbCash = paymentTypeView!!.findViewById(R.id.rbCash)
        rbCredit = paymentTypeView!!.findViewById(R.id.rbCredit)
        rbWallet = paymentTypeView!!.findViewById(R.id.rbWallet)
        rbNoCollection = paymentTypeView!!.findViewById(R.id.rbNoCollection)
        tvReceiptDetails = paymentTypeView!!.findViewById(R.id.tvReceiptDetails)

        etAmount = paymentTypeView!!.findViewById(R.id.etAmount)
        btnPaymentEndTask = paymentTypeView!!.findViewById(R.id.btnPaymentEndTask)



        ivBack.setOnClickListener(this)
        btnStart.setOnClickListener(this)
        ivPaymentBack.setOnClickListener(this)
        rbCash.setOnClickListener(this)
        rbCredit.setOnClickListener(this)
        rbWallet.setOnClickListener(this)
        rbNoCollection.setOnClickListener(this)
        tvReceiptDetails.setOnClickListener(this)


        btnPaymentEndTask.setOnClickListener(this)




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
                                    Log.d(TAG, "location")
//                                    isFirstTime = false
                                    calculateDirections(
                                        LatLng(
                                            lastLocation?.latitude!!,
                                            lastLocation?.longitude!!
                                        ), destination, true, true
                                    )

                                } else
                                    Alert.showMessage(
                                        this@TaskLocationsActivity,
                                        getString(R.string.no_internet)
                                    )


                            }

                        }

                    } else if (intent.getBooleanExtra(  // starting task
                            "startTask",
                            true
                        ) && (!AppConstants.CurrentSelectedTask.IsStarted || !AppConstants.CurrentEditedTask.TaskId.isNullOrEmpty())
                    ) // Courier  start  journey from details view
                    {

                        map.isMyLocationEnabled = false

                        if (NetworkManager().isNetworkAvailable(this@TaskLocationsActivity)) {
//////////////////////////////////////////////////// prevent interact with map untill snapshot for map is taken ////////////////////////////////////////////////////
//                            map.uiSettings.setAllGesturesEnabled(false)
//                            map.uiSettings.isScrollGesturesEnabled = false
//                            map.uiSettings.isZoomGesturesEnabled = false
//                            mapFragment.view?.isClickable = false
//                            mapFragment.view?.isFocusable = false
///////////////////////////////////////////////////////////////////////////////////////////////
                            btnStart.visibility = View.VISIBLE
                            var firstStop =
                                AppConstants.CurrentAcceptedTask.stopsmodel.find { it.StopTypeID == 1 }
                            var lastStop =
                                AppConstants.CurrentAcceptedTask.stopsmodel.find { it.StopTypeID == 2 }
                            var pickUp = LatLng(
                                firstStop?.Latitude!!,
                                firstStop?.Longitude!!
                            )
                            var dropOff = LatLng(
                                lastStop?.Latitude!!,
                                lastStop?.Longitude!!
                            )
                            waypoints = ArrayList()

//                            calculateDirections(pickUp, dropOff, false, false)
                            calcDirection(pickUp, dropOff, false)
                            btnStart.visibility = View.VISIBLE
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
//        val builder = IntentBuilder()
//
//        try {
//            startActivityForResult(builder.build(this), PLACE_PICKER_REQUEST)
//        } catch (e: GooglePlayServicesRepairableException) {
//            e.printStackTrace()
//        } catch (e: GooglePlayServicesNotAvailableException) {
//            e.printStackTrace()
//        }
    }
    //endregion

    //region Events
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_details)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        Log.d("language", UserSessionManager.getInstance(this).getLanguage())
        isACcepted =
            AppConstants.CurrentSelectedTask.IsStarted// isStartedTask(AppConstants.CurrentSelectedTask)

        init()


    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.clear()
        map.isMyLocationEnabled = true
        map.mapType = GoogleMap.MAP_TYPE_TERRAIN //more map details
        map.uiSettings.isZoomControlsEnabled = false
        map.setOnMarkerClickListener(this)
        map.setOnPolylineClickListener(this)


        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->

            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                lastLocation = location
            }
        }

        map.setOnMyLocationClickListener {

            false
        }
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        try {
            var stop = marker?.tag as Stop
            Log.d("Stop", stop.StopName)
            if (intent.getBooleanExtra("startTask", true) && stop.StopTypeID == 2)
                marker.title = snippetData
            else
                marker.title = stop.StopName
            marker.showInfoWindow()
        } catch (ex: Exception) {
            Log.e("Stop Error", ex.message)
        }
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


    private fun calculateDirections(
        origin: LatLng,
        dest: LatLng,
        isStop: Boolean,
        showAlternatives: Boolean
    ) {

        val destination = com.google.maps.model.LatLng(
            dest.latitude,
            dest.longitude
        )
        val directions = DirectionsApiRequest(mGeoApiContext)
        directions.alternatives(showAlternatives)

        directions.origin(
            com.google.maps.model.LatLng(
                origin.latitude,
                origin.longitude
            )
        )
        Log.d(TAG, "calculateDirections: destination: $destination")

//
//        if (!isStop) {
//            AppConstants.CurrentAcceptedTask.stopsmodel.forEach {
//
//                if (it.StopTypeID == 3) {
//                    Log.d(TAG, "default Stop:" + it.StopName)
//                    directions.waypoints(
//                        com.google.maps.model.LatLng(
//                            it.Latitude!!,
//                            it.Longitude!!
//                        )
//                    ).optimizeWaypoints(true)
//
//                }
//            }
//        }
//        directions.optimizeWaypoints(true)


        directions.destination(destination)
            .setCallback(object : PendingResult.Callback<DirectionsResult?> {
                override fun onResult(result: DirectionsResult?) {

                    result!!.routes[0].legs.forEach {
                        Log.d(
                            TAG,
                            "LEG: duration: " + it.duration
                        );
                        Log.d(
                            TAG,
                            "LEG: distance: " + it.distance
                        );
                        Log.d("LEG DATA", it.toString())

                        meters += it.distance.inMeters
                        totalDistance += it.distance.inMeters
                        totalSeconds += it.duration.inSeconds
                    }

                    Log.d(TAG, "totalKilometers: $totalKilometers")
                    Log.d(TAG, "METERS:  $meters")
                    totalKilometers = conevrtMetersToKilometers(meters)
                    meters = 0L
                    addPolylinesToMap(result!!, isStop)


                }

                override fun onFailure(e: Throwable) {

                    runOnUiThread {
                        Alert.hideProgress()
                        Alert.showMessage(this@TaskLocationsActivity, "Can't find a way there.")
                        totalKilometers = 0
                        Log.e(
                            TAG,
                            "calculateDirections: Failed to get directions: " + e.message
                        )
                    }

                }
            })

    }

    private fun calcDirection(origin: LatLng, dest: LatLng, isStop: Boolean) {
        var wayPoints = ""
        var counter = 0
        if (!isStop) {
            AppConstants.CurrentSelectedTask.stopsmodel.forEach {

                if (it.StopTypeID == 3) {
                    Log.d(TAG, "default Stop:" + it.StopName)
                    if (counter == 0)
                        wayPoints += "via:" + it.Latitude + "," + it.Longitude
                    else
                        wayPoints += "|via:" + it.Latitude + "," + it.Longitude
                    counter += 1
                }

            }
            counter = 0
        }
        Log.d(TAG, "way points: " + wayPoints)

        var baseUrl = "https://maps.googleapis.com/"
        val str_origin = origin.latitude.toString() + "," + origin.longitude.toString()
        val str_dest = dest.latitude.toString() + "," + dest.longitude.toString()


        if (NetworkManager().isNetworkAvailable(this)) {
            runOnUiThread {
                Alert.showProgress(this)
            }
            var request = NetworkManager().create(baseUrl, ApiServices::class.java)

            var endPoint = request.getFullJson(
                str_origin,
                str_dest,
                wayPoints,
                getString(R.string.google_map_key)
            )


            endPoint?.enqueue(object : Callback<Directions?> {
                override fun onFailure(call: Call<Directions?>, t: Throwable) {
                    Log.e(
                        TAG,
                        "calculateTwoDirections: Failed to get directions: " + t.message
                    )
                    runOnUiThread {
                        Alert.hideProgress()
                        Alert.showMessage(this@TaskLocationsActivity, "Can't find a way there.")
                        totalKilometers = 0
                    }
                }

                override fun onResponse(
                    call: Call<Directions?>,
                    response: Response<Directions?>
                ) {

                    if (response.isSuccessful && response.body()?.status.equals("OK")) {
                        Log.d(TAG, "onResponse:isSuccessful " + response.isSuccessful)
//                        drawRouteOnMap(map, response.body()!!.directionPolylines)
                        var dist = response.body()?.routes?.get(0)?.legs?.get(0)?.distance?.text
                        var dur = response.body()?.routes?.get(0)?.legs?.get(0)?.duration?.text

                        var data = dist?.split(" km").toString()
                        var value = dist?.split(" km").toString().trim()[0].toInt()
                        totalKilometers =
                            conevrtMetersToKilometers(response.body()?.routes?.get(0)?.legs?.get(0)?.distance?.value!!.toLong())
                        totalDistanceValue =
                            response.body()?.routes?.get(0)?.legs?.get(0)?.duration?.text.toString()
                        totalSeconds =
                            response.body()?.routes?.get(0)?.legs?.get(0)?.duration?.value!!.toLong()
                        Log.d(TAG, "totalDistanceValue: " + totalDistanceValue)
                        Log.d(TAG, "totalSeconds: " + totalSeconds)
                        Log.d(TAG, "DIst: " + dist)
                        Log.d(TAG, "value: " + value)
                        Log.d(TAG, "dur: " + dur)
                        Log.d(TAG, "totalKilometers: $totalKilometers")
                        Log.d(TAG, "METERS:  $meters")


                        addPolylinesToMapNew(response.body()!!, isStop)
                        runOnUiThread {
                            Alert.hideProgress()
                        }
                    } else {
                        runOnUiThread {
                            Alert.hideProgress()
                            Alert.showMessage(this@TaskLocationsActivity, "Can't find a way there.")
                        }
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


    private fun calcTripDirection(origin: LatLng, dest: LatLng) {
        var wayPoints = ""
        var counter = 0

        AppConstants.CurrentSelectedTask.stopsmodel.forEach {

            if (it.StopTypeID == 3) {
                Log.d(TAG, "default Stop:" + it.StopName)
                if (counter == 0)
                    wayPoints += "via:" + it.Latitude + "," + it.Longitude
                else
                    wayPoints += "|via:" + it.Latitude + "," + it.Longitude
                counter += 1
            }

        }

        counter = 0
        Log.d(TAG, "way points: " + wayPoints)

        var baseUrl = "https://maps.googleapis.com/"
        val str_origin = origin.latitude.toString() + "," + origin.longitude.toString()
        val str_dest = dest.latitude.toString() + "," + dest.longitude.toString()


        if (NetworkManager().isNetworkAvailable(this)) {
            runOnUiThread { Alert.showProgress(this) }
            var request = NetworkManager().create(baseUrl, ApiServices::class.java)

            var endPoint = request.getFullJson(
                str_origin,
                str_dest,
                wayPoints,
                getString(R.string.google_map_key)
            )


            endPoint?.enqueue(object : Callback<Directions?> {
                override fun onFailure(call: Call<Directions?>, t: Throwable) {
                    Log.e(
                        TAG,
                        "calculateTwoDirections: Failed to get directions: " + t.message
                    )
                    runOnUiThread {
                        Alert.hideProgress()
                        Alert.showMessage(this@TaskLocationsActivity, "Can't find a way there.")
                        totalKilometers = 0
                    }
                }

                override fun onResponse(
                    call: Call<Directions?>,
                    response: Response<Directions?>
                ) {

                    if (response.isSuccessful && response.body()?.status.equals("OK")) {
                        Log.d(TAG, "onResponse:isSuccessful " + response.isSuccessful)
//                        drawRouteOnMap(map, response.body()!!.directionPolylines)
                        var dist = response.body()?.routes?.get(0)?.legs?.get(0)?.distance?.text
                        var dur = response.body()?.routes?.get(0)?.legs?.get(0)?.duration?.text

                        var data = dist?.split(" km").toString()
                        var value = dist?.split(" km").toString().trim()[0].toInt()
                        totalKilometers =
                            conevrtMetersToKilometers(response.body()?.routes?.get(0)?.legs?.get(0)?.distance?.value!!.toLong())
                        totalDistanceValue =
                            response.body()?.routes?.get(0)?.legs?.get(0)?.duration?.text.toString()
                        totalSeconds =
                            response.body()?.routes?.get(0)?.legs?.get(0)?.duration?.value!!.toLong()
                        Log.d(TAG, "totalDistanceValue: " + totalDistanceValue)
                        Log.d(TAG, "totalSeconds: " + totalSeconds)
                        Log.d(TAG, "DIst: " + dist)
                        Log.d(TAG, "value: " + value)
                        Log.d(TAG, "dur: " + dur)
                        Log.d(TAG, "totalKilometers: $totalKilometers")
                        Log.d(TAG, "METERS:  $meters")

                        runOnUiThread {
                            Alert.hideProgress()
                            startTrip(AppConstants.CurrentSelectedTask, totalKilometers.toFloat())
                        }
                    } else
                        runOnUiThread {
                            Alert.hideProgress()
                            Alert.showMessage(this@TaskLocationsActivity, "press start again.")
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

    fun drawRouteOnMap(map: GoogleMap, positions: List<LatLng>) {
        resetMap()
        var speciaMarker: Marker? = null
        val options = PolylineOptions().width(5f).color(Color.BLUE).geodesic(true)
        options.addAll(positions)
        val polyline = map.addPolyline(options)
        val cameraPosition = CameraPosition.Builder()
            .target(LatLng(positions.get(0).latitude, positions.get(0).longitude))
            .zoom(12f).build()

        val builder = LatLngBounds.Builder()

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

        builder.include(pickUp).include(dropOff)
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(builder.build().center, 10f))

        map!!.addMarker(
            MarkerOptions()
                .position(pickUp)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                .title(firstStop.StopName)

        ).showInfoWindow()



        map!!.addMarker(
            MarkerOptions()
                .position(dropOff)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .title(lastStop.StopName)

        ).showInfoWindow()

        AppConstants.CurrentSelectedTask.stopsmodel.forEach {
            if (it.StopTypeID == 3) {
                map!!.addMarker(
                    MarkerOptions()
                        .position(dropOff)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        .title(it.StopName)

                ).showInfoWindow()
            }
        }


    }


    private fun calculateTwoDirections(origin: LatLng, dest: LatLng): Int {
        Alert.showProgress(this)
        val destination = com.google.maps.model.LatLng(
            dest.latitude,
            dest.longitude
        )
        val directions = DirectionsApiRequest(mGeoApiContext)
        directions.alternatives(false)

        directions.origin(
            com.google.maps.model.LatLng(
                origin.latitude,
                origin.longitude
            )
        )

        if (AppConstants.CurrentAcceptedTask.stopsmodel.size > 0) {
            AppConstants.CurrentAcceptedTask.stopsmodel.forEach {

                if (it.StopTypeID == 3) {
                    directions.waypoints(
                        com.google.maps.model.LatLng(
                            it.Latitude!!,
                            it.Longitude!!
                        )
                    ).optimizeWaypoints(true)

                }

            }
            directions.optimizeWaypoints(true)
        }

        directions.destination(destination)
            .setCallback(object : PendingResult.Callback<DirectionsResult?> {
                override fun onResult(result: DirectionsResult?) {
                    directionResult = result!!
                    Log.d(
                        TAG,
                        "calculateTwoDirections: successfully retrieved directions."
                    )
                    result!!.routes[0].legs.forEach {
                        Log.d(
                            TAG,
                            "calculateTwoDirections -> LEG: duration: " + it.duration
                        );
                        Log.d(
                            TAG,
                            "calculateTwoDirections -> LEG: distance: " + it.distance
                        );
                        Log.d(TAG, "calculateTwoDirections -> LEG DATA $it")

                        meters += it.distance.inMeters
                        totalDistance += it.distance.inMeters
                        totalSeconds += it.duration.inSeconds


                    }

                    totalKilometers = conevrtMetersToKilometers(meters)
                    meters = 0L
                    if (totalKilometers > 0.0) {
//                        prepareMapView(mTripMarkers)
                        Log.d(TAG, "totalKilometers: $totalKilometers")
                        runOnUiThread {

                            startTrip(AppConstants.CurrentSelectedTask, totalKilometers.toFloat())
                        }

                    } else {
                        Alert.hideProgress()
                        Alert.showMessage(this@TaskLocationsActivity, "press start again.")
                    }

                }

                override fun onFailure(e: Throwable) {

                    Log.e(
                        TAG,
                        "calculateTwoDirections: Failed to get directions: " + e.message
                    )
                    runOnUiThread {
                        Alert.hideProgress()
                        Alert.showMessage(this@TaskLocationsActivity, "Can't find a way there.")
                        totalKilometers = 0
                    }
                }
            })

        return totalKilometers
    }


    private fun addPolylinesToMap(result: DirectionsResult, isStop: Boolean) {

        Handler(Looper.getMainLooper()).post {
            Log.d(
                TAG,
                "run: result routes: " + result.routes.size
            )
            if (mPolyLinesData.size > 0) {
                for (polylineData in mPolyLinesData) {
                    polylineData.polyline.remove()
                }
                mPolyLinesData.clear()
                mPolyLinesData = java.util.ArrayList<PolylineData>()
            }
            var duration = 999999999.0

            for (route in result.routes) {
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


                // highlight the fastest route and adjust camera
                val tempDuration =
                    route.legs[0].duration.inSeconds.toDouble()

                if (tempDuration < duration) {
                    map.clear()
                    map.mapType = GoogleMap.MAP_TYPE_TERRAIN
                    polyline =
                        map.addPolyline(PolylineOptions().addAll(newDecodedPath)) // add marker
                    polyline.color = ContextCompat.getColor(this, R.color.colorPrimary)
                    polyline.isClickable = true
                    mPolyLinesData.add(PolylineData(polyline, route.legs[0]))

                    duration = tempDuration
//                    onPolylineClick(polyline)
                    if (isStop) {
                        var stop = AppConstants.currentSelectedStop
                        val marker: Marker = map.addMarker(
                            MarkerOptions()
                                .icon(bitmapDescriptorFromVector(this, R.drawable.ic_location))
                                .position(LatLng(stop.Latitude!!, stop.Longitude!!))
                                .title(/*"[" + getString(R.string.trip) + " " + index + "] - " +*/
                                    stop.StopName
                                )
                        )
                        mTripMarkers.add(marker)
                        marker.tag = stop
                        marker.showInfoWindow()
                    }
                    zoomRoute(polyline.points)
                    if (isStop)
                        setTripDirectionData(PolylineData(polyline, route.legs[0]))
                    else
                        setTotalTripDirectionData()
                    rlBottom.visibility = View.VISIBLE
                }

            }
            if (!isStop)
                setTripStopsMarker(AppConstants.CurrentSelectedTask)


        }

    }

    private fun addPolylinesToMapNew(result: Directions, isStop: Boolean) {

        Handler(Looper.getMainLooper()).post {
            Log.d(
                TAG,
                "run: result routes: " + result.routes!!.size
            )
            if (mPolyLinesData.size > 0) {
                for (polylineData in mPolyLinesData) {
                    polylineData.polyline.remove()
                }
                mPolyLinesData.clear()
                mPolyLinesData = java.util.ArrayList<PolylineData>()
            }
            var duration = 999999999.0

            for (route in result.routes) {
                val decodedPath =
                    PolylineEncoding.decode(route.overview_polyline?.points)
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


                // highlight the fastest route and adjust camera
                val tempDuration =
                    route.legs!![0].duration!!.value.toDouble()

                if (tempDuration < duration) {
                    map.clear()
                    map.mapType = GoogleMap.MAP_TYPE_TERRAIN
                    polyline =
                        map.addPolyline(PolylineOptions().addAll(newDecodedPath)) // add marker
                    polyline.color = ContextCompat.getColor(this, R.color.colorPrimary)
                    polyline.isClickable = true
                    mPolyLinesDataNew.add(
                        PolylineDataNew(
                            polyline,
                            route.legs[0]
                        )
                    )

                    duration = tempDuration
                    if (isStop) {
                        var stop = AppConstants.currentSelectedStop
                        val marker: Marker = map.addMarker(
                            MarkerOptions()
                                .icon(bitmapDescriptorFromVector(this, R.drawable.ic_location))
                                .position(LatLng(stop.Latitude!!, stop.Longitude!!))
                                .title(/*"[" + getString(R.string.trip) + " " + index + "] - " +*/
                                    stop.StopName
                                )
                        )
                        mTripMarkers.add(marker)
                        marker.tag = stop
                        marker.showInfoWindow()
                    }
                    zoomRoute(polyline.points)

                    if (isStop)
                        setTripDirectionDataNew(PolylineDataNew(polyline, route.legs[0]))
                    else
                        setTotalTripDirectionDataNew(PolylineDataNew(polyline, route.legs[0]))

                    rlBottom.visibility = View.VISIBLE
                }

            }
            if (!isStop)
                setTripStopsMarker(AppConstants.CurrentSelectedTask)


        }

    }

    private fun setTripStopsMarker(task: Task) {
        var speciaMarker: Marker? = null
        task.stopsmodel.forEach {
            when (it.StopTypeID) {
                1 -> {
                    val marker: Marker = map.addMarker(
                        MarkerOptions()
                            .icon(bitmapDescriptorFromVector(this, R.drawable.ic_location_start))
                            .position(LatLng(it.Latitude!!, it.Longitude!!))
                            .title(it.StopName)


                    )
                    mTripMarkers.add(marker)
                    marker.tag = it
                    Log.d("MARKER DATA", it.StopName)
//                    marker.showInfoWindow()
                }
                2 -> {
                    snippetData =
                        getString(R.string.distance) + " " + totalKilometers.toString() + " " + getString(
                            R.string.km
                        ) + " - " + (getString(
                            R.string.duration
                        ) + " " + tripData.getDuration(totalSeconds.toInt()))

                    Log.d("snippetData", snippetData)
                    Log.d("snippetDataD", tripData.getDuration(totalSeconds.toInt()))

//                    snippetData =
//                        getString(R.string.distance) + " " + totalKilometers.toString() + " " + getString(
//                            R.string.km
//                        ) + " - " + (getString(
//                            R.string.duration
//                        ) + " " + totalDistanceValue) //tripData.toString())

                    speciaMarker = map.addMarker(
                        MarkerOptions()
                            .icon(bitmapDescriptorFromVector(this, R.drawable.ic_location))
                            .position(LatLng(it.Latitude!!, it.Longitude!!))
                            .title(snippetData)
                    )

                    mTripMarkers.add(speciaMarker!!)
                    speciaMarker?.tag = it
                    Log.d("MARKER DATA", it.StopName)
                    speciaMarker?.showInfoWindow()
                }
                3 -> {

                    val marker: Marker = map.addMarker(
                        MarkerOptions()
                            .icon(bitmapDescriptorFromVector(this, R.drawable.ic_location_stops))
                            .position(LatLng(it.Latitude!!, it.Longitude!!))
                            .title(it.StopName)
                    )
                    mTripMarkers.add(marker)
                    marker.tag = it
                    Log.d("MARKER DATA", it.StopName)
                    Log.d("Latitude", it.Latitude.toString())
                    Log.d("Longitude", it.Longitude.toString())


//                    marker.showInfoWindow()
                }
            }
        }

        speciaMarker!!.showInfoWindow()

    }

    override fun onPolylineClick(polyline: Polyline?) {
        var index = 0
        var name = ""

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

                if (!AppConstants.currentSelectedStop.StopName.isNullOrEmpty())
                    name = AppConstants.currentSelectedStop.StopName
                else
                    name = AppConstants.CurrentSelectedTask.TaskName

                var snippetData =
                    getString(R.string.distance) + " " + polylineData.leg.distance + " " + (getString(
                        R.string.duration
                    ) + " " + tripData.getDuration(polylineData.leg.duration.inSeconds.toInt()))
                Log.d("snippetData", snippetData)
                Log.d(
                    "snippetDataD",
                    tripData.getDuration(polylineData.leg.duration.inSeconds.toInt())
                )
                val marker: Marker = map.addMarker(
                    MarkerOptions()
                        .icon(bitmapDescriptorFromVector(this, R.drawable.ic_location))
                        .position(endLocation)
                        .title(/*"[" + getString(R.string.trip) + " " + index + "] - " +*/ name)
                        .snippet(
                            snippetData
                        )


                )
                Log.d(TAG, "ANASS")
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
        val routePadding = 150
        val latLngBounds = boundsBuilder.build()

        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding),
            600,
            null
        )


//        var cu = CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding)
//        map.moveCamera(
//            CameraUpdateFactory.zoomTo(16f)
//        )


    }

    private fun resetMap() {
        if (map != null) {
            map.clear()
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            if (mPolyLinesData.size > 0) {
                mPolyLinesData.clear()
                mPolyLinesData = java.util.ArrayList()
            }
            if (mPolyLinesDataNew.size > 0) {
                mPolyLinesDataNew.clear()
                mPolyLinesDataNew = java.util.ArrayList()
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
//        tvExpectedDistance.text =
//            "( " + polylineData.leg.distance.toString() + "  )"
        tvExpectedDistance.text =
            "( " + totalKilometers.toString() + " " + getString(R.string.km) + "  )"
    }

    private fun setTripDirectionDataNew(polylineData: PolylineDataNew) {
        Log.d(TAG, "polylineData:" + polylineData.leg.toString())
        tvExpectedTime.text = polylineData.leg.duration?.text.toString()
//        tvExpectedDistance.text =
//            "( " + polylineData.leg.distance.toString() + "  )"
        tvExpectedDistance.text = totalDistanceValue
        // replaceData(totalDistanceValue)//polylineData.leg.distance?.text.toString()
//            "( " + totalKilometers.toString() + " " + getString(R.string.km) + "  )"
    }

    private fun setTotalTripDirectionData() {
        tripData = prepareTripData()
        tvExpectedTime.text = tripData.getDuration()// tripData.toString()
        tvExpectedDistance.text = totalDistanceValue//replaceData(totalDistanceValue)
        // "( " + totalKilometers.toString() + " " + getString(R.string.km) + "  )"
    }

    private fun setTotalTripDirectionDataNew(polylineData: PolylineDataNew) {
        tripData = prepareTripData()
        tvExpectedTime.text =
            tripData.getDuration(polylineData.leg.duration?.value!!)//polylineData.leg.duration?.text.toString()
        tvExpectedDistance.text = """$totalKilometers ${getString(R.string.km)}"""


    }


    private fun startTrip(task: Task, totalKilometers: Float) {
//        if (mTripMarkers.size > 0)
//            prepareMapView(mTripMarkers)
        Log.d(TAG, "startTrip")
        Log.d(TAG, "kilometers:" + totalKilometers)

        //stop ingteract with map
        map.uiSettings.setAllGesturesEnabled(false)
        map.uiSettings.isScrollGesturesEnabled = false
        map.uiSettings.isZoomGesturesEnabled = false
        mapFragment.view?.isClickable = false
        mapFragment.view?.isFocusable = false
        Log.d(TAG, "Start Trip : " + task.TaskId)
        if (NetworkManager().isNetworkAvailable(this)) {

            if (!task.TaskId.isNullOrEmpty()) {
                var request = NetworkManager().create(ApiServices::class.java)
                var endPoint = request.startTask(task.TaskId, totalKilometers)
                NetworkManager().request(
                    endPoint,
                    object : INetworkCallBack<ApiResponse<Task>> {
                        override fun onFailed(error: String) {
                            Log.d(TAG, "Faild: $error")

                            Alert.hideProgress()
                            Alert.showMessage(
                                this@TaskLocationsActivity,
                                getString(R.string.error_login_server_unknown_error)
                            )
                        }

                        override fun onSuccess(response: ApiResponse<Task>) {
                            if (response.Status == AppConstants.STATUS_SUCCESS) {
                                var task = response.ResponseObj!!
                                captureScreen(map, task.TaskId)
                                task.IsStarted = true
                                AppConstants.CurrentAcceptedTask = task
                                AppConstants.CurrentSelectedTask = task
                                AppConstants.CurrentEditedTask = Task()
                                AppConstants.COURIERSTARTTASK = true
                                startTaskFirebase(task, AppConstants.CurrentLoginCourier.CourierId)
                                Log.d(TAG, "START TASK READY TO END")
                                Alert.hideProgress()
                                btnStart.text = getString(R.string.end_task)
                                map.isMyLocationEnabled = true
                            } else {
                                Log.d(TAG, "Some thing is wrong." + (response.Status.toString()))
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
                        FirebaseManager.updateCourierStartTask(
                            AppConstants.CurrentLoginCourier.CourierId,
                            false
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

    private fun endTask(task: Task, paymentType: Int, amount: Double) {
        Alert.showProgress(this)
        if (NetworkManager().isNetworkAvailable(this)) {
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.endTask(task.TaskId, paymentType, amount)
            NetworkManager().request(endPoint, object : INetworkCallBack<ApiResponse<Task>> {
                override fun onFailed(error: String) {
                    Alert.hideProgress()
                    Alert.showMessage(
                        this@TaskLocationsActivity,
                        getString(R.string.error_login_server_unknown_error)
                    )
                    btnPaymentEndTask.isEnabled = true
                }

                override fun onSuccess(response: ApiResponse<Task>) {
                    if (response.Status == AppConstants.STATUS_SUCCESS) { //1 sucess

                        FirebaseManager.endTask(
                            AppConstants.CurrentSelectedTask,
                            AppConstants.CurrentLoginCourier.CourierId
                        )
                        FirebaseManager.updateCourierStartTask(
                            AppConstants.CurrentLoginCourier.CourierId,
                            false
                        )

                        AppConstants.CurrentAcceptedTask = Task()
                        AppConstants.CurrentSelectedTask = Task()
                        AppConstants.COURIERSTARTTASK = false
                        AppConstants.ALL_TASKS_DATA.remove(AppConstants.CurrentSelectedTask) //removed when life cycle

                        AppConstants.endTask = true
                        //load new task or shoe empty tasks view
                        Alert.hideProgress()
                        btnPaymentEndTask.isEnabled = true
                        startActivity(Intent(this@TaskLocationsActivity, TaskActivity::class.java))
                        finish()

                    } else if (response.Status == AppConstants.STATUS_FAILED)  // -1  "this task already ended before"
                    {
                        Alert.hideProgress()
                        Alert.showMessage(
                            this@TaskLocationsActivity,
                            getString(R.string.error_end))
                        btnPaymentEndTask.isEnabled = true
                    } else if (response.Status == AppConstants.STATUS_FAILED_2) //-2  An Error Occured
                    {
                        Alert.hideProgress()
                        Alert.showMessage(
                            this@TaskLocationsActivity,
                            getString(R.string.error_login_server_unknown_error)
                        )
                        btnPaymentEndTask.isEnabled = true
                    } else if (response.Status == AppConstants.STATUS_INCORRECT_DATA) //-3  you don't have enough balance in wallet
                    {
                        Alert.hideProgress()
                        Alert.showMessage(
                            this@TaskLocationsActivity,
                            getString(R.string.error_wallet)
                        )
                        btnPaymentEndTask.isEnabled = true
                    } else if (response.Status == AppConstants.STATUS_FAILED_4) //-4  you don't have Credit card in kadabra
                    {
                        Alert.hideProgress()
                        Alert.showMessage(
                            this@TaskLocationsActivity,
                           getString(R.string.error_credit)
                        )
                        btnPaymentEndTask.isEnabled = true
                    }

                }
            })

        } else {
            Alert.hideProgress()
            Alert.showMessage(
                this@TaskLocationsActivity,
                getString(R.string.no_internet)
            )
            btnPaymentEndTask.isEnabled = true
        }


    }


    private fun conevrtMetersToKilometers(meters: Long): Int {
        var kilometers = 0
        if (meters in 100..999) //grater than 100 meters and ess than 1000 meters consider as 1 kilometer
            kilometers = 1
        else
            kilometers = ceil((meters * 0.001).toFloat()).toInt()

        Log.d(TAG, "totalKilometers: RESULT $kilometers")

        return kilometers
    }

    fun captureScreen(mMap: GoogleMap, taskId: String) {
        val callback =
            SnapshotReadyCallback { snapshot ->

                UtilHelper.uploadFile(snapshot, taskId)
                map.uiSettings.setAllGesturesEnabled(true)
                map.uiSettings.isScrollGesturesEnabled = true
                map.uiSettings.isZoomGesturesEnabled = true
                mapFragment.view?.isClickable = true
                mapFragment.view?.isFocusable = true
            }
        mMap.snapshot(callback)
    }


    fun locationIsInRange(currentLocation: Location, distLocation: Location): Boolean {
        var distanceInKiloMeters = (currentLocation.distanceTo(distLocation)) / 1000
        return distanceInKiloMeters <= 1
    }


    fun calculateDistance(totalDistance: Long): Double {
        var dist = totalDistance / 1000.0
        Log.d("distance", "Calculated distance:" + dist);
        return dist

    }

    fun prepareTripData(): TripData {
        var days = totalSeconds / 86400
        var hours = (totalSeconds - days * 86400) / 3600
        var minutes = (totalSeconds - days * 86400 - hours * 3600) / 60
        var seconds = totalSeconds - days * 86400 - hours * 3600 - minutes * 60
        var distance = totalKilometers//calculateDistance(totalDistance)
        var durationData = TripData(days, hours, minutes, seconds, distance)

        Log.d(
            "duration",
            "$days days $hours hours $minutes mins$ seconds seconds"
        );

        return durationData
    }

    private fun prepareMapView(
        markersList: java.util.ArrayList<Marker>
    ) {


        var builder = LatLngBounds.builder()
        markersList.forEach { marker ->
            builder.include(marker.position)

        }
        var bounds = builder.build()
        // begin new code:
        var width = resources.displayMetrics.widthPixels;
        var height = resources.displayMetrics.heightPixels;
        var padding = (width * 0.12).toInt()
//        var padding = 0 // offset from edges of the mMap in pixels
        var cu = CameraUpdateFactory.newLatLngBounds(bounds, width, 2000, padding)
//        var cu = CameraUpdateFactory.newLatLngBounds(bounds,100, 2000, 0)

        map.animateCamera(cu)
    }


    private fun choosePaymentTypeWindow() {

        var alert = AlertDialog.Builder(this)
        alertDialog = alert.create()
        alertDialog!!.setView(paymentTypeView)

    }

    fun getStringByLocale(context: Activity, id: Int, locale: String?): String? {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale(locale))
        return context.createConfigurationContext(configuration).resources.getString(id)
    }

    fun replaceData(oldData: String): String {
        //1 hour 42 mins

        var data = oldData
        if (UserSessionManager.getInstance(this).getLanguage() == AppConstants.ARABIC) {
            data = oldData.replace("hour", "mins")
                .replace(
                    getStringByLocale(this, R.string.hour, "ar")!!,
                    getStringByLocale(this, R.string.minutes, "ar")!!
                )
            Log.d("datadata", data)
        }
        return data
    }

    private fun getReceiptData(taskId: String) {
        Alert.showProgress(this)
        if (NetworkManager().isNetworkAvailable(this)) {
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.displayReceiptData(taskId)
            NetworkManager().request(
                endPoint,
                object : INetworkCallBack<ApiResponse<ReceiptData>> {
                    override fun onFailed(error: String) {
                        Alert.hideProgress()
                        Alert.showMessage(
                            this@TaskLocationsActivity,
                            getString(R.string.error_login_server_error)
                        )
                    }

                    override fun onSuccess(response: ApiResponse<ReceiptData>) {
                        if (response.Status == AppConstants.STATUS_SUCCESS) {
                            Alert.hideProgress()
                            var data = response.ResponseObj!!
                            totalReceiptValue = data.Sum
                            prepareReciptData(data)
                            Alert.hideProgress()
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

    private fun prepareReciptData(data: ReceiptData): String {
        Log.d(TAG, data.ServiceCost.size.toString())
        var receiptValue = ""
        if (data.ServiceCost?.size!! > 0) {
            tvReceiptDetails.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.ic_up)
            data.ServiceCost.forEach {
                receiptValue += it.serviceCostName + " : " + it.cost + " " + getString(R.string.le) + "\n"
            }
            receiptValue += "____________________________________\n"
            receiptValue += "${getString(R.string.total)} ${data.Sum} ${getString(R.string.le)}"
            runOnUiThread { tvReceiptDetails.text = receiptValue }

        }


        return receiptValue
    }


}

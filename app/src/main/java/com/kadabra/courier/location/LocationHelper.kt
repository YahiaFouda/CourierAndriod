package com.kadabra.courier.location

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.kadabra.courier.R
import com.kadabra.courier.callback.ILocationListener
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.model.location
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.AppController
import android.content.Context
import android.location.LocationManager
import android.os.Build



class LocationHelper : ILocationListener {

    var TAG = "LocationHelper"

    var locationListener: ILocationListener? = null
    var lastLocation: Location? = null
    private val permissionFineLocation = android.Manifest.permission.ACCESS_FINE_LOCATION
    //    private val permissionCoarseLocation = android.Manifest.permission.ACCESS_COARSE_LOCATION
    private val REQUEST_CODE_LOCATION = 100
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationRequest: LocationRequest? = null
    private var callbabck: LocationCallback? = null

    companion object {
        var shared: LocationHelper = LocationHelper()
            private set
    }

    init {

        locationListener = this
        fusedLocationClient =
            FusedLocationProviderClient(AppController.getContext().applicationContext)

        initializeLocationRequest()

        callbabck = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                lastLocation = p0?.lastLocation
                locationListener!!.locationResponse(p0!!)

                if (AppConstants.CurrentAcceptedTask != null && !AppConstants.CurrentAcceptedTask.TaskId.isNullOrEmpty()) {
                    AppConstants.CurrentAcceptedTask.location = location(
                        lastLocation!!.latitude.toString(),
                        lastLocation!!.longitude.toString(),
                        isLocationEnabled()
                    )
                    FirebaseManager.updateTaskLocation(AppConstants.CurrentAcceptedTask)
                    FirebaseManager.updateCourierLocation(
                        AppConstants.CurrentAcceptedTask.CourierID.toString(),
                        location(
                            lastLocation!!.latitude.toString(),
                            lastLocation!!.longitude.toString(),
                            isLocationEnabled()
                        )
                    )
                } else if (AppConstants.CurrentLoginCourier != null && AppConstants.CurrentLoginCourier.CourierId > 0) {
                    FirebaseManager.updateCourierLocation(
                        AppConstants.CurrentLoginCourier.CourierId.toString(),
                        location(
                            lastLocation!!.latitude.toString(),
                            lastLocation!!.longitude.toString(),
                            isLocationEnabled()
                        )
                    )
                }

            }
        }
    }

    override fun locationResponse(locationResult: LocationResult) {
        lastLocation = locationResult.lastLocation
    }

    private fun initializeLocationRequest() {

        locationRequest = LocationRequest()

        locationRequest?.interval = 10000   //5 minutes==300000
//        locationRequest?.maxWaitTime = 100000
        locationRequest?.fastestInterval = 5000
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

    }


    private fun validatePermissionsLocation(): Boolean {
        val fineLocationAvailable = ActivityCompat.checkSelfPermission(
            AppController.getContext(),
            permissionFineLocation
        ) == PackageManager.PERMISSION_GRANTED
//        val coarseLocationAvailable = ActivityCompat.checkSelfPermission(
//            AppController.getContext(),
//            permissionCoarseLocation
//        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationAvailable //&& coarseLocationAvailable
    }

    private fun requestPermissions(activity: Activity) {
        val contextProvider =
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionFineLocation)

        if (contextProvider) {
            Toast.makeText(
                AppController.getContext(),
                AppController.getContext().getString(R.string.error_location_permission_required),
                Toast.LENGTH_SHORT
            ).show()
        }
        permissionRequest(activity)
    }

    private fun permissionRequest(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permissionFineLocation/*, permissionCoarseLocation*/),
            REQUEST_CODE_LOCATION
        )
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getLocation()
                } else {
                    Toast.makeText(
                        AppController.getContext(),
                        AppController.getContext().getString(R.string.error_no_location_permission_gived),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun stopUpdateLocation() {
        this.fusedLocationClient?.removeLocationUpdates(callbabck)
    }


    fun initializeLocation(activity: Activity) {
        if (validatePermissionsLocation()) {
            getLocation()
        } else {
            requestPermissions(activity)
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        validatePermissionsLocation()
        fusedLocationClient?.requestLocationUpdates(locationRequest, callbabck, null)
    }


    fun isLocationEnabled(): Boolean {

        var gps_enabled = false
        var network_enabled = false

        val lm =
            AppController.getContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager?

        try {
            gps_enabled = lm!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            network_enabled = lm!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (e: Exception) {
            e.printStackTrace()
        }
//
//        if (!gps_enabled && !network_enabled) {
//            gps_enabled = false
//            network_enabled = false
//        }
        return gps_enabled


    }



}
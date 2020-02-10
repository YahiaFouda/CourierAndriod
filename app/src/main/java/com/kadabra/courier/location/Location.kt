package com.kadabra.courier.location

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.kadabra.courier.R
import com.kadabra.courier.callback.ILocationListener

class Location(var activity: AppCompatActivity, locationListener: ILocationListener) {
    private val permissionFineLocation = android.Manifest.permission.ACCESS_FINE_LOCATION
    private val permissionCoarseLocation = android.Manifest.permission.ACCESS_COARSE_LOCATION

    private val REQUEST_CODE_LOCATION = 100

    private var fusedLocationClient: FusedLocationProviderClient? = null

    private var locationRequest: LocationRequest? = null
    private var callbabck: LocationCallback? = null

    init {
        fusedLocationClient = FusedLocationProviderClient(activity.applicationContext)

        inicializeLocationRequest()
        callbabck = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)

                locationListener.locationResponse(p0!!)
            }
        }
    }

    private fun inicializeLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest?.interval = 50000
        locationRequest?.fastestInterval = 5000
        locationRequest?.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun validatePermissionsLocation(): Boolean {
        val fineLocationAvailable = ActivityCompat.checkSelfPermission(
            activity.applicationContext,
            permissionFineLocation
        ) == PackageManager.PERMISSION_GRANTED
        val coarseLocationAvailable = ActivityCompat.checkSelfPermission(
            activity.applicationContext,
            permissionCoarseLocation
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationAvailable && coarseLocationAvailable
    }

    private fun requestPermissions() {
        val contextProvider =
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permissionFineLocation)

        if (contextProvider) {
            Toast.makeText(
                activity.applicationContext,
                activity.applicationContext.getString(R.string.error_location_permission_required),
                Toast.LENGTH_SHORT
            ).show()
        }
        permissionRequest()
    }

    private fun permissionRequest() {
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permissionFineLocation, permissionCoarseLocation),
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
                        activity.applicationContext,
                        activity.applicationContext.getString(R.string.error_no_location_permission_gived),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    fun stopUpdateLocation() {
        this.fusedLocationClient?.removeLocationUpdates(callbabck)
    }

    fun initializeLocation() {
        if (validatePermissionsLocation()) {
            getLocation()
        } else {
            requestPermissions()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        validatePermissionsLocation()
        fusedLocationClient?.requestLocationUpdates(locationRequest, callbabck, null)
    }
}
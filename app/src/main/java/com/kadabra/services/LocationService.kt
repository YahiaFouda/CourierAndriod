package com.kadabra.services


import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices


class LocationService : Service() {

    private var mFusedLocationClient: FusedLocationProviderClient? = null

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build()

            startForeground(1, notification)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: called.")
        getLocation()
        return Service.START_NOT_STICKY
    }

    private fun getLocation() {

        // ---------------------------------- LocationRequest ------------------------------------
        // Create the location request to start receiving updates
        val mLocationRequestHighAccuracy = LocationRequest()
        mLocationRequestHighAccuracy.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequestHighAccuracy.interval = UPDATE_INTERVAL
        mLocationRequestHighAccuracy.fastestInterval = FASTEST_INTERVAL


        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "getLocation: stopping the location service.")
            stopSelf()
            return
        }
        Log.d(TAG, "getLocation: getting location information.")
        mFusedLocationClient!!.requestLocationUpdates(
            mLocationRequestHighAccuracy, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult?) {

                    Log.d(TAG, "onLocationResult: got location result.")

                    val location = locationResult!!.lastLocation

                    if (location != null) {
                        //                            User user = ((UserClient)(getApplicationContext())).getUser();
                        //                            GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                        //                            UserLocation userLocation = new UserLocation(user, geoPoint, null);
                        //                            saveUserLocation(userLocation);
                    }
                }
            },
            Looper.myLooper()
        ) // Looper.myLooper tells this to repeat forever until thread is destroyed
    }

    companion object {

        private val TAG = "LocationService"
        private val UPDATE_INTERVAL = (4 * 1000).toLong()  /* 4 secs */
        private val FASTEST_INTERVAL: Long = 2000 /* 2 sec */
    }

    //    private void saveUserLocation(final UserLocation userLocation){
    //
    //        try{
    //            DocumentReference locationRef = FirebaseFirestore.getInstance()
    //                    .collection(getString(R.string.collection_user_locations))
    //                    .document(FirebaseAuth.getInstance().getUid());
    //
    //            locationRef.set(userLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
    //                @Override
    //                public void onComplete(@NonNull Task<Void> task) {
    //                    if(task.isSuccessful()){
    //                        Log.d(TAG, "onComplete: \ninserted user location into database." +
    //                                "\n latitude: " + userLocation.getGeo_point().getLatitude() +
    //                                "\n longitude: " + userLocation.getGeo_point().getLongitude());
    //                    }
    //                }
    //            });
    //        }catch (NullPointerException e){
    //            Log.e(TAG, "saveUserLocation: User instance is null, stopping location service.");
    //            Log.e(TAG, "saveUserLocation: NullPointerException: "  + e.getMessage() );
    //            stopSelf();
    //        }
    //
    //    }
}
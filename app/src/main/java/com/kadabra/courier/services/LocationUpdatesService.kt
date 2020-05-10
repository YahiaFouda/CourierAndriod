package com.kadabra.courier.services

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.util.Log

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.kadabra.Networking.INetworkCallBack
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.R
import com.kadabra.courier.api.ApiResponse
import com.kadabra.courier.api.ApiServices
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.login.LoginActivity
import com.kadabra.courier.model.Courier
import com.kadabra.courier.model.Task
import com.kadabra.courier.model.location
import com.kadabra.courier.task.TaskActivity
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.Alert.hideProgress
import com.kadabra.courier.utilities.Alert.showProgress
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.AppController
import com.reach.plus.admin.util.UserSessionManager
import java.util.*

/**
 * A bound and started service that is promoted to a foreground service when location updates have
 * been requested and all clients unbind.
 *
 * For apps running in the background on "O" devices, location is computed only once every 10
 * minutes and delivered batched every 30 minutes. This restriction applies even to apps
 * targeting "N" or lower which are run on "O" devices.
 *
 * This sample show how to use a long-running service for location updates. When an activity is
 * bound to this service, frequent location updates are permitted. When the activity is removed
 * from the foreground, the service promotes itself to a foreground service, and location updates
 * continue. When the activity comes back to the foreground, the foreground service stops, and the
 * notification assocaited with that service is removed.
 */
class LocationUpdatesService : Service() {


    private val mBinder = LocalBinder()
    var isMockLocationEnabled = false

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private var mChangingConfiguration = false

    private var mNotificationManager: NotificationManager? = null

    /**
     * Contains parameters used by [com.kadabra.services].
     */
    private var mLocationRequest: LocationRequest? = null

    /**
     * Provides access to the Fused Location Provider API.
     */
    private var mFusedLocationClient: FusedLocationProviderClient? = null

    /**
     * Callback for changes in location.
     */
    private var mLocationCallback: LocationCallback? = null

    private var mServiceHandler: Handler? = null
    private var text = ""

    /**
     * The current location.
     */
    private var mLocation: Location? = null

    /**
     * Returns the [NotificationCompat] used as part of the foreground service.
     */
    private// Extra to help us figure out if we arrived in onStartCommand via the notification or not.
    // The PendingIntent that leads to a call to onStartCommand() in this service.
    // The PendingIntent to launch activity.
    // Set the Channel ID for Android O.
    // Channel ID
    val notification: Notification
        get() {
            val intent = Intent(this, LocationUpdatesService::class.java)
            if (mLocation != null) {
                var s = AppConstants.CurrentLocation
                text = "(" + mLocation!!.latitude + ", " + mLocation!!.longitude + ")"
            }
            intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true)


            val servicePendingIntent = PendingIntent.getService(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val activityPendingIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, TaskActivity::class.java), PendingIntent.FLAG_CANCEL_CURRENT
            )


            val builder = NotificationCompat.Builder(this)
                .addAction(
                    R.mipmap.ic_launcher, getString(R.string.launch_activity),
                    activityPendingIntent
                )
//                    .addAction(R.mipmap.ic_launcher, getString(R.string.remove_location_updates),
//                            servicePendingIntent)
//                .setContentText(text)
                .setContentTitle(getString(R.string.app_name))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
//                .setTicker(text)
                .setWhen(System.currentTimeMillis())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(CHANNEL_ID)
            }

            return builder.build()
//            } else
//                return Notification()

        }

    override fun onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                if (NetworkManager().isNetworkAvailable(applicationContext)) {
                    onNewLocation(locationResult!!.lastLocation)
                    isMockLocationEnabled = checkMockLocations(locationResult!!.lastLocation)
                    if (isMockLocationEnabled) {
                        Alert.showMessage(
                            AppController.getContext(),
                            getString(R.string.error_mock_location)
                        )
                        logOut()
                    }
                } else
                    AppConstants.CurrentLocation = null
            }
        }

        createLocationRequest()
        getLastLocation()

        val handlerThread = HandlerThread(TAG)
        handlerThread.start()
        mServiceHandler = Handler(handlerThread.looper)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            // Create the channel for the notification
            val mChannel =
                NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager!!.createNotificationChannel(mChannel)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.i(TAG, "Service started")
        val startedFromNotification = intent.getBooleanExtra(
            EXTRA_STARTED_FROM_NOTIFICATION,
            false
        )

        // We got here because the user decided to remove location updates from the notification.
        if (startedFromNotification) {
            removeLocationUpdates()
            stopSelf()
        }
        // Tells the system to not try to recreate the service after it has been killed.
        return Service.START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfiguration = true
    }

    override fun onBind(intent: Intent): IBinder? {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()")
        stopForeground(true)
        mChangingConfiguration = false
        return mBinder
    }

    override fun onRebind(intent: Intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()")
        stopForeground(true)
        mChangingConfiguration = false
        super.onRebind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.i(TAG, "Last client unbound from service")

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration && UserSessionManager.getInstance(this).requestingLocationUpdates()) {
            Log.i(TAG, "Starting foreground service")
            if (notification != null)
                startForeground(NOTIFICATION_ID, notification)
        }
        return true // Ensures onRebind() is called when a client re-binds.
    }

    override fun onDestroy() {
        mServiceHandler!!.removeCallbacksAndMessages(null)
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * [SecurityException].
     */
    fun requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates")
        UserSessionManager.getInstance(this).setRequestingLocationUpdates(true)
        startService(Intent(this, LocationUpdatesService::class.java))
        try {
            mFusedLocationClient!!.requestLocationUpdates(
                mLocationRequest,
                mLocationCallback!!, Looper.myLooper()
            )
        } catch (unlikely: SecurityException) {
            UserSessionManager.getInstance(this).setRequestingLocationUpdates(false)
            Log.e(TAG, "Lost location permission. Could not request updates. $unlikely")
        }

    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * [SecurityException].
     */
    fun removeLocationUpdates() {
        Log.i(TAG, "Removing location updates")
        try {
            mFusedLocationClient!!.removeLocationUpdates(mLocationCallback!!)
            UserSessionManager.getInstance(this).setRequestingLocationUpdates(false)
            stopSelf()
        } catch (unlikely: SecurityException) {
            UserSessionManager.getInstance(this).setRequestingLocationUpdates(true)
            Log.e(TAG, "Lost location permission. Could not remove updates. $unlikely")
        }

    }

    private fun getLastLocation() {
        try {
            mFusedLocationClient!!.lastLocation
                .addOnCompleteListener { task ->
                    if (task.isSuccessful && task.result != null) {
                        mLocation = task.result
                    } else {
                        Log.w(TAG, "Failed to get location.")
                    }
                }
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Lost location permission.$unlikely")
        }

    }

    private fun onNewLocation(newLocation: Location) {
        Log.i(TAG, "New location: $newLocation")

        mLocation = newLocation
        AppConstants.CurrentLocation = mLocation


        updateCourierLocation(mLocation!!)

        // Notify anyone listening for broadcasts about the new location.
        val intent = Intent(ACTION_BROADCAST)
        intent.putExtra(EXTRA_LOCATION, newLocation)
        LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(intent)

        // Update notification content if running as a foreground service.
        if (serviceIsRunningInForeground(this)) {
            mNotificationManager!!.notify(NOTIFICATION_ID, notification)
        }
    }

    /**
     * Sets the location request parameters.
     */
    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval = UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    inner class LocalBinder : Binder() {
        internal val service: LocationUpdatesService
            get() = this@LocationUpdatesService
    }

    /**
     * Returns true if this is a foreground service.In
     *
     * @param context The [Context].
     */
    fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager = context.getSystemService(
            Context.ACTIVITY_SERVICE
        ) as ActivityManager
        for (service in manager.getRunningServices(
            Integer.MAX_VALUE
        )) {
            if (javaClass.name == service.service.className) {
                if (service.foreground) {
                    return true
                }
            }
        }
        return false
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

    companion object {

        private val PACKAGE_NAME =
            "com.google.android.gms.location.sample.locationupdatesforegroundservice"

        private val TAG = LocationUpdatesService::class.java.simpleName

        /**
         * The name of the channel for notifications.
         */
        private val CHANNEL_ID = "channel_01"

        internal val ACTION_BROADCAST = "$PACKAGE_NAME.broadcast"

        internal val EXTRA_LOCATION = "$PACKAGE_NAME.location"
        private val EXTRA_STARTED_FROM_NOTIFICATION = "$PACKAGE_NAME.started_from_notification"

        /**
         * The desired interval for location updates. Inexact. Updates may be more or less frequent.
         */
//        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 60000 //1 minute
        private val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 30000

        /**
         * The fastest rate for active location updates. Updates will never be more frequent
         * than this value.
         */
        private val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2

        /**
         * The identifier for the notification displayed for the foreground service.
         */
        private val NOTIFICATION_ID = 12345678

        /**
         * SINGLE TONE CLASS.
         */
        var shared = LocationUpdatesService()
            internal set
    }

    private fun updateCourierLocation(lastLocation: Location) {

        // COURIER HAVE TASK
        if (AppConstants.CurrentAcceptedTask != null && !AppConstants.CurrentAcceptedTask.TaskId.isNullOrEmpty()) {
            AppConstants.CurrentAcceptedTask.location = location(
                lastLocation!!.latitude.toString(),
                lastLocation!!.longitude.toString(),
                isLocationEnabled()
            )
            AppConstants.ALL_TASKS_DATA.forEach {
               if(it.Status==AppConstants.IN_PROGRESS)
                   it.location = location(
                       lastLocation!!.latitude.toString(),
                       lastLocation!!.longitude.toString(),
                       isLocationEnabled()
                   )
                   FirebaseManager.updateTaskLocation(it)
            }
//            FirebaseManager.updateTaskLocation(AppConstants.CurrentAcceptedTask)
            FirebaseManager.updateCourierLocation(
                AppConstants.CurrentAcceptedTask.CourierID.toString(),
                location(
                    lastLocation!!.latitude.toString(),
                    lastLocation!!.longitude.toString(),
                    isLocationEnabled()
                )
            )




            if (getAddress(AppConstants.CurrentLoginCourier, lastLocation))
                FirebaseManager.updateCourierCity(
                    AppConstants.CurrentLoginCourier.CourierId!!,
                    AppConstants.CurrentLoginCourier.city
                )

        }

        // COURIER DOSEN'T HAVE TASK
        else if (AppConstants.CurrentLoginCourier != null && AppConstants.CurrentLoginCourier.CourierId > 0) {
            FirebaseManager.updateCourierLocation(
                AppConstants.CurrentLoginCourier.CourierId.toString(),
                location(
                    lastLocation!!.latitude.toString(),
                    lastLocation!!.longitude.toString(),
                    isLocationEnabled()
                )
            )
            if (getAddress(AppConstants.CurrentLoginCourier, lastLocation))
                FirebaseManager.updateCourierCity(
                    AppConstants.CurrentLoginCourier.CourierId!!,
                    AppConstants.CurrentLoginCourier.city
                )

        }
    }



    private fun getAddress(courier: Courier, location: Location): Boolean {
        var detected = false
        try {
            var city = ""
            val gcd = Geocoder(AppController.getContext(), Locale.getDefault())
            val addresses = gcd.getFromLocation(
                location.latitude,
                location.longitude,
                1
            )
            if (addresses.size > 0) {
                if (!addresses[0].subAdminArea.isNullOrEmpty())
                    city = addresses[0].subAdminArea
                else if (!addresses[0].adminArea.isNullOrEmpty())
                    city = addresses[0].adminArea

                AppConstants.CurrentLoginCourier.city = city

            } else {
                city = addresses[0].featureName
                AppConstants.CurrentLoginCourier.city = city
            }
            if (!courier.city.trim().isNullOrEmpty() && !city.trim().isNullOrEmpty()) {
                detected = true
            }


        } catch (ex: Exception) {
            detected = false
        }
        return detected

    }

    fun checkMockLocations(location: Location): Boolean {
        var mockLocationsEnabled = false
        // Starting with API level >= 18 we can (partially) rely on .isFromMockProvider()
        // (http://developer.android.com/reference/android/location/Location.html#isFromMockProvider%28%29)
        // For API level < 18 we have to check the Settings.Secure flag
        if (Build.VERSION.SDK_INT < 18 && android.provider.Settings.Secure.getString(
                AppController.getContext().contentResolver, android.provider.Settings
                    .Secure.ALLOW_MOCK_LOCATION
            ) != "0"
        ) {
            mockLocationsEnabled = true

        } else if (Build.VERSION.SDK_INT > 18) {
            mockLocationsEnabled = location.isFromMockProvider
        } else
            mockLocationsEnabled = false

        return mockLocationsEnabled
    }

    private fun logOut() {

        if (NetworkManager().isNetworkAvailable(this)) {
            if (AppConstants.CurrentAcceptedTask.TaskId.isNullOrEmpty()) //no accepted task
            {
                var request = NetworkManager().create(ApiServices::class.java)
                var endPoint = request.logOut(AppConstants.CurrentLoginCourier.CourierId)
                NetworkManager().request(endPoint, object : INetworkCallBack<ApiResponse<Courier>> {
                    override fun onFailed(error: String) {
                        Alert.showMessage(
                            AppController.getContext(),
                            getString(R.string.no_internet)
                        )
                    }

                    override fun onSuccess(response: ApiResponse<Courier>) {
                        if (response.Status == AppConstants.STATUS_SUCCESS) {

                            //stop  tracking service
                            removeLocationUpdates()
                            UserSessionManager.getInstance(AppController.getContext()).logout()
                            shared.removeLocationUpdates()

                            startActivity(
                                Intent(
                                    AppController.getContext(),
                                    LoginActivity::class.java
                                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            )


                        }

                    }
                })
            } else //accepted task prevent logout
            {

                Alert.showMessage(
                    AppController.getContext(),
                    getString(R.string.end_first)
                )
            }
        } else {
            Alert.showMessage(
                AppController.getContext(),
                getString(R.string.no_internet)
            )
        }


    }

}

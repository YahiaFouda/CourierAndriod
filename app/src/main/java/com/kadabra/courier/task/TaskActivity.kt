package com.kadabra.courier.task

import android.Manifest
import android.app.AlertDialog
import android.content.*

import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.location.Geocoder
import android.location.Location
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.reach.plus.admin.util.UserSessionManager
import com.kadabra.Networking.INetworkCallBack
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.adapter.TaskAdapter
import com.kadabra.courier.api.ApiResponse
import com.kadabra.courier.api.ApiServices
import com.kadabra.courier.callback.IBottomSheetCallback
import com.kadabra.courier.login.LoginActivity
import com.kadabra.courier.model.Courier
import com.kadabra.courier.model.Task
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.AppController
import kotlinx.android.synthetic.main.activity_task.*
import com.kadabra.courier.R
import android.media.MediaPlayer
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.kadabra.courier.BuildConfig
import com.kadabra.courier.callback.ILocationListener
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.location.LocationHelper
import com.kadabra.courier.model.location
import com.kadabra.services.LocationUpdatesService
import java.util.*
import kotlin.collections.ArrayList


class TaskActivity : AppCompatActivity(), View.OnClickListener, IBottomSheetCallback,
    ILocationListener {

    //region Members

    private val durationTime = 60000L
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null
    private var countDownTimer: CountDownTimer? = null
    private var locationCallback: LocationCallback? = null
    private var locationRequest: LocationRequest? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationUpdateState = false
    private var task = Task()
    private lateinit var listener: IBottomSheetCallback
    private var taskList = ArrayList<Task>()
    private var adapter: TaskAdapter? = null
    private var lastLocation: Location? = null
    private var courier: Courier = Courier()

    private var myReceiver: MyReceiver? = null
    // A reference to the service used to get location updates.
    private var mService: LocationUpdatesService? = null
    // Tracks the bound state of the service.
    private var mBound = false

    //endregion

    //region Constructor
    companion object {
        private val TAG = TaskActivity::class.java.simpleName
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val REQUEST_CHECK_SETTINGS = 2
        private const val PLACE_PICKER_REQUEST = 3

    }


    // Monitors the state of the connection to the service.
    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as LocationUpdatesService.LocalBinder
            mService = binder.service
            mBound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            mService = null
            mBound = false
        }
    }
    //endregiono

    //region Helper Functions
    private fun init() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        ivSettings.setOnClickListener(this)
        ivMenu.setOnClickListener(this)
        ivAccept.setOnClickListener(this)
        tvTimer.visibility = View.INVISIBLE

        loadTasks()

        refresh.setOnRefreshListener {
            loadTasks()
        }



        ivSettings.setOnTouchListener(OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val view = v as ImageView
                    //overlay is black with transparency of 0x77 (119)
                    view.drawable.setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP)
                    view.invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val view = v as ImageView
                    //clear the overlay
                    view.getDrawable().clearColorFilter()
                    view.invalidate()
                }
            }

            false
        })

        ivMenu.setOnTouchListener(OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val view = v as ImageView
                    //overlay is black with transparency of 0x77 (119)
                    view.drawable.setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP)
                    view.invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val view = v as ImageView
                    //clear the overlay
                    view.drawable.clearColorFilter()
                    view.invalidate()
                }
            }

            false
        })

        ivAccept.setOnTouchListener(OnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val view = v as ImageView
                    //overlay is black with transparency of 0x77 (119)
                    view.drawable.setColorFilter(0x77000000, PorterDuff.Mode.SRC_ATOP)
                    view.invalidate()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val view = v as ImageView
                    //clear the overlay
                    view.drawable.clearColorFilter()
                    view.invalidate()
                }
            }

            false
        })
    }

    private fun accept() {
        cancelVibrate()
        stopSound()
        UserSessionManager.getInstance(this).setIsAccepted(true)
        if (countDownTimer != null)
            countDownTimer!!.cancel()
//        tvTimer.text = "00:00"
        tvTimer.visibility = View.INVISIBLE

    }

    private fun logOut() {
        showProgress()
        if (NetworkManager().isNetworkAvailable(this)) {
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.logOut(AppConstants.CurrentLoginCourier.CourierId)
            NetworkManager().request(endPoint, object : INetworkCallBack<ApiResponse<Courier>> {
                override fun onFailed(error: String) {
                    hideProgress()
                    Alert.showMessage(
                        this@TaskActivity,
                        getString(R.string.no_internet)
                    )
                }

                override fun onSuccess(response: ApiResponse<Courier>) {
                    if (response.Status == AppConstants.STATUS_SUCCESS) {
                        //stop  tracking service
                        stopTracking()
                        FirebaseManager.logOut()
                        UserSessionManager.getInstance(this@TaskActivity).setUserData(null)
                        UserSessionManager.getInstance(this@TaskActivity).setIsLogined(false)
                        startActivity(Intent(this@TaskActivity, LoginActivity::class.java))
                        if (countDownTimer != null)
                            countDownTimer!!.cancel()
                        cancelVibrate()
                        stopSound()

                        //remove location update
                        LocationHelper.shared.stopUpdateLocation()
                        finish()
                        hideProgress()

                    } else {
                        hideProgress()
                        Alert.showMessage(
                            this@TaskActivity,
                            getString(R.string.error_network)
                        )
                    }

                }
            })

        } else {
            hideProgress()
            Alert.showMessage(
                this@TaskActivity,
                getString(R.string.no_internet)
            )
        }


    }

    private fun playSound() {
        mediaPlayer = MediaPlayer.create(this@TaskActivity, R.raw.alarm)
        mediaPlayer!!.isLooping = true
        mediaPlayer!!.start()
    }

    private fun stopSound() {
        if (mediaPlayer != null)
            mediaPlayer!!.stop()
    }

    private fun processTask() {
        if (!UserSessionManager.getInstance(this@TaskActivity).isAccepted()) {
            tvTimer.visibility = View.VISIBLE
            vibrate()
            playSound()

            countDownTimer = object : CountDownTimer(durationTime, 1000) {

                override fun onTick(millisUntilFinished: Long) {
                    runOnUiThread(Runnable {
                        val remainedSecs = millisUntilFinished / 1000
                        tvTimer.text = "00" + ":" + remainedSecs % 60
                    })
                }

                override fun onFinish() {
                    logOut()
                    cancelVibrate()
                    stopSound()

                }
            }.start()
        }


    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 100, 1000)
        vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator!!.vibrate(
//                VibrationEffect.createWaveform(
//                    pattern,
//                  0
//                )
                pattern,
                0
            )
//            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator!!.vibrate(60000)
        }
    }

    private fun cancelVibrate() {
        if (vibrator != null)
            vibrator!!.cancel()
    }

    private fun getCurrentActiveTask() {
        if (NetworkManager().isNetworkAvailable(this)) {
            FirebaseManager.getCurrentActiveTask(AppConstants.CurrentLoginCourier.CourierId.toString(),
                object : FirebaseManager.IFbOperation {
                    override fun onSuccess(code: Int) {

                    }

                    override fun onFailure(message: String) {
                        AppConstants.CurrentAcceptedTask = Task()
                    }
                })
        }

    }

    private fun getCurrentCourierLocation() {
        if (NetworkManager().isNetworkAvailable(this)) {
            FirebaseManager.getCurrentCourierLocation(AppConstants.CurrentLoginCourier.CourierId.toString(),
                object : FirebaseManager.IFbOperation {
                    override fun onSuccess(code: Int) {
//                        Toast.makeText(
//                            this@TaskActivity,
//                            AppConstants.CurrentCourierLocation.lat + AppConstants.CurrentCourierLocation.long,
//                            Toast.LENGTH_SHORT
//                        ).show()

                    }

                    override fun onFailure(message: String) {

                    }
                })
        }

    }

    private fun blink() {
        val hander = Handler()
        Thread(Runnable {
            try {
                Thread.sleep(550)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }

            hander.post {

                if (tvTimer.visibility == View.VISIBLE) {
                    tvTimer.visibility = View.INVISIBLE
                } else {
                    tvTimer.visibility = View.VISIBLE
                }
                blink()
            }
        }).start()
    }

    private fun loadTasks() {
        ivNoInternet.visibility = View.INVISIBLE
        showProgress()
        if (NetworkManager().isNetworkAvailable(this)) {
            ivNoInternet.visibility = View.INVISIBLE
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.getAvaliableTasks(AppConstants.CurrentLoginCourier.CourierId)
            NetworkManager().request(
                endPoint,
                object : INetworkCallBack<ApiResponse<ArrayList<Task>>> {
                    override fun onFailed(error: String) {
                        refresh.isRefreshing = false
                        hideProgress()
                        Alert.showMessage(
                            this@TaskActivity,
                            getString(R.string.error_login_server_error)
                        )
                    }

                    override fun onSuccess(response: ApiResponse<ArrayList<Task>>) {

                        if (response.Status == AppConstants.STATUS_SUCCESS) {
                            refresh.isRefreshing = false
                            tvEmptyData.visibility = View.INVISIBLE
                            taskList = response.ResponseObj!!
                            if (taskList.size > 0) {
                                adapter = TaskAdapter(this@TaskActivity, taskList)
                                rvTasks.adapter = adapter
                                rvTasks?.layoutManager =
                                    GridLayoutManager(
                                        AppController.getContext(),
                                        1,
                                        GridLayoutManager.VERTICAL,
                                        false
                                    )

                                processTask()

                                hideProgress()
                            } else {//no tasks
                                tvTimer.visibility = View.INVISIBLE
                                tvEmptyData.visibility = View.VISIBLE
                            }

                        } else {
                            hideProgress()
                            tvEmptyData.visibility = View.VISIBLE
                        }

                    }
                })

        } else {
            refresh.isRefreshing = false
            hideProgress()
            ivNoInternet.visibility = View.VISIBLE
            Alert.showMessage(
                this@TaskActivity,
                getString(R.string.no_internet)
            )
        }


    }

    private fun showProgress() {
        tvEmptyData.visibility = View.INVISIBLE
        avi.smoothToShow()
    }

    private fun hideProgress() {
        avi.smoothToHide()
        tvEmptyData.visibility = View.INVISIBLE
    }

    private fun createLocationRequest() {
        // 1
        locationRequest = LocationRequest()
        // 2
        locationRequest!!.interval = 1000
        // 3
        locationRequest!!.fastestInterval = 5000
        locationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest!!)

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
                        TaskActivity.REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
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

    }

    private fun requestPermission() {
        if (NetworkManager().isNetworkAvailable(this)) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            if (checkPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                fusedLocationClient?.lastLocation?.addOnSuccessListener(
                    this
                ) { location: Location? ->
                    // Got last known location. In some rare
                    // situations this can be null.
                    if (location == null) {
                        //no data

                    } else location.apply {
                        // Handle location object
                        Log.e("LOG", location.toString())
                        AppConstants.CurrentLocation = location
                    }
                }
            }
        } else {
            Alert.showMessage(this@TaskActivity, getString(R.string.no_internet))
        }
    }

    private fun checkPermission(vararg perm: String): Boolean {
        val havePermissions = perm.toList().all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (!havePermissions) {
            if (perm.toList().any {
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        it
                    )
                }) {

                val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.Permission))
                    .setMessage(getString(R.string.error_location_permission_required))
                    .setPositiveButton(getString(R.string.ok)) { id, v ->
                        ActivityCompat.requestPermissions(
                            this, perm, LOCATION_PERMISSION_REQUEST_CODE
                        )
                    }
                    .setNegativeButton(getString(R.string.no)) { _, _ -> }
                    .create()
                dialog.show()
            } else {
                ActivityCompat.requestPermissions(
                    this, perm,
                    LOCATION_PERMISSION_REQUEST_CODE
                )

            }
            return false
        }
        return true
    }

    //endregion

    //region Events
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        window.addFlags(WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY)
        setContentView(R.layout.activity_task)
        myReceiver = MyReceiver()

//        LocationHelper.shared.initializeLocation(this)
//        requestPermission()
        // Check that the user hasn't revoked permissions by going to Settings.
        if (UserSessionManager.getInstance(this).requestingLocationUpdates())
        {
            if (!checkPermissions()) {
                requestPermissions()
            }

        }



        init()
        FirebaseManager.setUpFirebase()
        getCurrentActiveTask()
        getCurrentCourierLocation()

    }

    override fun locationResponse(locationResult: LocationResult) {
        lastLocation = locationResult.lastLocation
    }


    override fun onResume() {
        super.onResume()
        if (AppConstants.endTask) {
            loadTasks()
            AppConstants.endTask = false

        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
            myReceiver!!,
            IntentFilter(LocationUpdatesService.ACTION_BROADCAST)
        )
    }

    override fun onStart() {
        super.onStart()
        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.
        bindService(
            Intent(this, LocationUpdatesService::class.java), mServiceConnection,
            Context.BIND_AUTO_CREATE
        )

        if (!checkPermissions()) {
            requestPermissions()
        } else {
            if (mService != null)
                mService!!.requestLocationUpdates()

        }

        bindService(
            Intent(this, LocationUpdatesService::class.java), mServiceConnection,
            Context.BIND_AUTO_CREATE
        )
    }


    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(myReceiver!!)
        super.onPause()
    }

    override fun onStop() {
        if (mBound) {
            // Unbind from the service. This signals to the service that this activity is no longer
            // in the foreground, and the service can respond by promoting itself to a foreground
            // service.
            unbindService(mServiceConnection)
            mBound = false
        }

        super.onStop()
    }


    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.ivSettings -> {
                AlertDialog.Builder(this)
                    .setTitle(AppConstants.WARNING)
                    .setMessage(getString(R.string.exit))
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(AppConstants.OK) { dialog, which ->
                        logOut()
                    }
                    .setNegativeButton(AppConstants.CANCEL) { dialog, which -> }
                    .show()
            }
            R.id.ivMenu -> {
                Alert.showMessage(this, "Menu is clicked.")
            }
            R.id.ivAccept -> {
                if (taskList.size > 0) {
                    accept()
                } else
                    Alert.showMessage(
                        this,
                        getString(R.string.no_tasks_available)
                    )

            }

        }
    }


    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onBottomSheetClosed(isClosed: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onBottomSheetSelectedItem(index: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        // etc.
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.size <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i(TAG, "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                mService!!.requestLocationUpdates()
            } else {
                // Permission denied.
//                setButtonsState(false)
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
        }
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
                        this@TaskActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                this@TaskActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun stopTracking() {
        if (mService != null)
            mService!!.removeLocationUpdates()
    }

    private fun prepareCourierData() {
        courier.name = courier.CourierName
        courier.token = FirebaseManager.getCurrentUser()!!.uid //token
        courier.isActive = true
        var isGpsEnabled = LocationHelper.shared.isLocationEnabled()
        courier.location = location(
            LocationHelper.shared.lastLocation!!.latitude.toString(),
            LocationHelper.shared.lastLocation!!.longitude.toString(), isGpsEnabled
        )

        courier.isActive = true


        val gcd = Geocoder(this@TaskActivity, Locale.getDefault())
        val addresses = gcd.getFromLocation(
            LocationHelper.shared.lastLocation!!.latitude,
            LocationHelper.shared.lastLocation!!.longitude,
            1
        )
        if (addresses.size > 0) {
            if (!addresses[0].subAdminArea.isNullOrEmpty())
                courier.city = addresses[0].subAdminArea
            else if (!addresses[0].adminArea.isNullOrEmpty())
                courier.city = addresses[0].adminArea
        } else
            courier.city = addresses[0].featureName

        if(FirebaseManager.getCurrentUser()!=null)
        {
            //update courier data on db
        }
        else
        {
            //create new courier on real db
        }
    }
    //endregion


    //region Helper Classes

    /**
     * Receiver for broadcasts sent by [LocationUpdatesService].
     */
    private inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val location =
                intent.getParcelableExtra<Location>(LocationUpdatesService.EXTRA_LOCATION)
//            if (location != null) {
//                Toast.makeText(this@TaskActivity, "(" + location.latitude + ", " + location.longitude + ")",
//                    Toast.LENGTH_SHORT).show()
//            }
        }
    }
    //endregion


}

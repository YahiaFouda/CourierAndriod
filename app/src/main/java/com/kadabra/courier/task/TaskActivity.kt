package com.kadabra.courier.task

import android.app.AlertDialog

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.PorterDuff
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
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.kadabra.courier.direction.FetchURL


class TaskActivity : AppCompatActivity(), View.OnClickListener, IBottomSheetCallback {

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


    //endregion

    //region Constructor
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        const val REQUEST_CHECK_SETTINGS = 2
        private const val PLACE_PICKER_REQUEST = 3
    }
    //endregion

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
            var endPoint = request.logOut(AppConstants.currentLoginCourier.CourierId)
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
                        UserSessionManager.getInstance(this@TaskActivity).setUserData(null)
                        UserSessionManager.getInstance(this@TaskActivity).setIsLogined(false)
                        startActivity(Intent(this@TaskActivity, LoginActivity::class.java))
                        if (countDownTimer != null)
                            countDownTimer!!.cancel()
                        cancelVibrate()
                        stopSound()
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

    //endregion

    //region Events
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        window.addFlags(WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY)
        setContentView(R.layout.activity_task)

        init()
        requestLocation()
    }

    override fun onResume() {
        super.onResume()
        if (AppConstants.endTask) {
            loadTasks()
            AppConstants.endTask = false

        }
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
            var endPoint = request.getAvaliableTasks(AppConstants.currentLoginCourier.CourierId)
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
        locationRequest!!.interval = 10000
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
        //2
//        fusedLocationClient.requestLocationUpdates(
//            locationRequest,
//            locationCallback,
//            null /* Looper */
//        )
    }

    private fun requestLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        //prepare for update the current location
//        locationCallback = object : LocationCallback() {
//            override fun onLocationResult(p0: LocationResult) {
//                super.onLocationResult(p0)
//
//                lastLocation = p0.lastLocation
//                //todo move the marker position
//
//                if (isFirstTime) {
//                    if (AppConstants.currentSelectedStop != null) {
//                        var selectedStopLocation = LatLng(
//                            AppConstants.currentSelectedStop.Latitude!!,
//                            AppConstants.currentSelectedStop.Longitude!!
//                        )
//
//                        placeMarkerOnMap(
//                            selectedStopLocation,
//                            AppConstants.currentSelectedStop.StopName
//                        )
//
//                        try {
//                            if (lastLocation != null) {
//
//                                var destination = LatLng(
//                                    AppConstants.currentSelectedStop.Latitude!!,
//                                    AppConstants.currentSelectedStop.Longitude!!
//                                )
////                                drawRouteOnMap(
////                                    LatLng(lastLocation!!.latitude, lastLocation!!.longitude),
////                                    destination
////                                )
//                                FetchURL(this@LocationDetailsActivity).execute(
//                                    getUrl(
//                                        LatLng(lastLocation!!.latitude, lastLocation!!.longitude),
//                                        destination,
//                                        "driving"
//                                    ), "driving"
//                                )
//                            }
//
//                        } catch (ex: ExceptionInInitializerError) {
//
//                        }
//
//                    }
//
//                    isFirstTime = false
//                }
//
//
//            }
//        }
        createLocationRequest()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request.
    }
    //endregion


}

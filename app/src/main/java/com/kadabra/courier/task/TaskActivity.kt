package com.kadabra.courier.task

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.graphics.PorterDuff
import android.location.Location
import android.os.*
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import androidx.recyclerview.widget.GridLayoutManager
import com.reach.plus.admin.util.UserSessionManager
import com.kadabra.Networking.INetworkCallBack
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.adapter.TaskAdapter
import com.kadabra.courier.api.ApiResponse
import com.kadabra.courier.api.ApiServices
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
import android.widget.PopupMenu
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.kadabra.courier.BuildConfig
import com.kadabra.courier.base.BaseNewActivity
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.services.LocationUpdatesService
import kotlin.collections.ArrayList


class TaskActivity : BaseNewActivity(), View.OnClickListener {

    //region Members

    private val durationTime = 60000L
    private var vibrator: Vibrator? = null
    private var mediaPlayer: MediaPlayer? = null
    private var countDownTimer: CountDownTimer? = null
    private var locationRequest: LocationRequest? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationUpdateState = false
    private var task = Task()
    private var taskList = ArrayList<Task>()
    private var adapter: TaskAdapter? = null
    private var lastLocation: Location? = null
    private var courier: Courier = Courier()
    private var myReceiver: MyReceiver? = null
    // A reference to the service used to get location updates.
//    private var mService: LocationUpdatesService? = null
    // Tracks the bound state of the service.
    private var mBound = false
    private var lastVerion = 0
    private lateinit var languageMenu: PopupMenu
    private var isNewTaskReceived = false //new task is received but not accepted
    private var KEY_TASK_LIST_DATA="taskList"

    //endregion

    //region Constructor
    companion object {
        private val TAG = TaskActivity::class.java.simpleName
        const val REQUEST_CHECK_SETTINGS = 2
        private const val PLACE_PICKER_REQUEST = 3

    }


    // Monitors the state of the connection to the service.
    private val mServiceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            val binder = service as LocationUpdatesService.LocalBinder
            LocationUpdatesService.shared = binder.service
            mBound = true
            LocationUpdatesService.shared!!.requestLocationUpdates()
        }

        override fun onServiceDisconnected(name: ComponentName) {
//            mService = null
            LocationUpdatesService.shared = LocationUpdatesService()
            mBound = false
        }
    }
    //endregion

    //region Helper Functions
    private fun init() {
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?


        ivSettings.setOnClickListener(this)
        ivMenu.setOnClickListener(this)
        ivAccept.setOnClickListener(this)
        tvTimer.visibility = View.INVISIBLE

//        loadTasks()
        refresh.setOnRefreshListener {
            loadTasks()
        }




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
        isNewTaskReceived = false
        cancelVibrate()
        stopSound()

        if (countDownTimer != null)
            countDownTimer!!.cancel()
        tvTimer.visibility = View.INVISIBLE


        if (NetworkManager().isNetworkAvailable(this))
            UserSessionManager.getInstance(this).setIsAccepted(true)
        else
            Alert.showMessage(
                    this@TaskActivity,
                    getString(R.string.no_internet)
            )
    }

    private fun logOut() {
//        if(AppConstants.CurrentAcceptedTask.TaskId.isNullOrEmpty())
//        {
        showProgress()
        if (NetworkManager().isNetworkAvailable(this)) {
            if (AppConstants.CurrentAcceptedTask.TaskId.isNullOrEmpty()) //no accepted task
            {
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
                            AppConstants.FIRE_BASE_LOGOUT = false
                            FirebaseManager.logOut()
                            UserSessionManager.getInstance(this@TaskActivity).setUserData(null)
                            UserSessionManager.getInstance(this@TaskActivity).setIsLogined(false)
                            UserSessionManager.getInstance(this@TaskActivity).setFirstTime(false)

                            startActivity(Intent(this@TaskActivity, LoginActivity::class.java))
                            if (countDownTimer != null)
                                countDownTimer!!.cancel()
                            cancelVibrate()
                            stopSound()
                            stopTracking()
                            //remove location update
//                        LocationHelper.shared.stopUpdateLocation()
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
            } else //accepted task prevent logout
            {
                hideProgress()
                Alert.showMessage(
                        this@TaskActivity,
                        getString(R.string.end_first)
                )
            }
        } else {
            hideProgress()
            Alert.showMessage(
                    this@TaskActivity,
                    getString(R.string.no_internet)
            )
        }

//        }
//        else{
//            Alert.showMessage(
//                this@TaskActivity,
//                getString(R.string.no_internet)
//            )
//        }

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

        isNewTaskReceived = true
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
                AppConstants.isCountDownTimerIsFinished = true
                logOut()
                cancelVibrate()
                stopSound()

            }
        }.start()


    }

    private fun vibrate() {
        val pattern = longArrayOf(0, 100, 1000)
        vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator!!.vibrate(
                    pattern,
                    0
            )
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

                        }

                        override fun onFailure(message: String) {

                        }
                    })
        }

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
                                    AppConstants.ALL_TASKS_DATA=taskList
                                    prepareTasks(taskList)

//                                processTask()

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

    private fun prepareTasks(tasks:ArrayList<Task>) {
        adapter = TaskAdapter(this@TaskActivity, tasks)
        rvTasks.adapter = adapter
        rvTasks?.layoutManager =
                GridLayoutManager(
                        AppController.getContext(),
                        1,
                        GridLayoutManager.VERTICAL,
                        false
                )
    }

    private fun showProgress() {
        tvEmptyData.visibility = View.INVISIBLE
        avi.smoothToShow()
    }

    private fun hideProgress() {
        avi.smoothToHide()
        tvEmptyData.visibility = View.INVISIBLE
    }


    //endregion

    //region Events
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // recovering the instance state

        setContentView(R.layout.activity_task)


        myReceiver = MyReceiver()
        FirebaseManager.setUpFirebase()
        getCurrentActiveTask()
        init()
        loadTasks()
        getCurrentCourierLocation()
        forceUpdate()


        // Check that the user hasn't revoked permissions by going to Settings.
//        if (UserSessionManager.getInstance(this).requestingLocationUpdates()) {
//            if (!checkPermissions()) {
//                requestPermissions()
//            }
//        }
//        else {
//            if (!checkPermissions()) {
//                requestPermissions()
//            }
//        }


    }


    override fun onResume() {
        super.onResume()




        LocalBroadcastManager.getInstance(this).registerReceiver(
                myReceiver!!,
                IntentFilter(LocationUpdatesService.ACTION_BROADCAST)
        )


//
    }

    override fun onStart() {
        super.onStart()
        // Bind to the service. If the service is in foreground mode, this signals to the service
        // that since this activity is in the foreground, the service can exit foreground mode.

        if (UserSessionManager.getInstance(this).requestingLocationUpdates()) {
            if (!checkPermissions()) {
                requestPermissions()
            }
        }

        bindService(
                Intent(this, LocationUpdatesService::class.java), mServiceConnection,
                Context.BIND_AUTO_CREATE
        )


        if (AppConstants.endTask)
            AppConstants.endTask = false

        if(AppConstants.ALL_TASKS_DATA.size>0)
            prepareTasks(AppConstants.ALL_TASKS_DATA)

        if (AppConstants.FIRE_BASE_LOGOUT) {

            logOut()
        }
        if (AppConstants.FIRE_BASE_NEW_TASK) {
            AppConstants.FIRE_BASE_NEW_TASK = false
            processTask()//new task is arrived but not accepted and the view is minimized and maximized so show tier and accept to accept
//        if (!AppConstants.isCountDownTimerIsFinished) {
//            tvTimer.visibility = View.VISIBLE
//        }
        }

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
                if (isNewTaskReceived)
                    return
                AlertDialog.Builder(this)
                        .setTitle(getString(R.string.warning))
                        .setMessage(getString(R.string.exit))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(getString(R.string.ok)) { dialog, which ->
                            logOut()
                        }
                        .setNegativeButton(getString(R.string.cancel)) { dialog, which -> }
                        .show()
            }
            R.id.ivMenu -> {
                if (isNewTaskReceived)
                    return
                var currentLanguage = UserSessionManager.getInstance(this).getLanguage()
                languageMenu = PopupMenu(this, ivMenu)
                languageMenu.menuInflater.inflate(R.menu.menu_language, languageMenu.menu)
                languageMenu.setOnMenuItemClickListener {
                    if (it.itemId == R.id.arabic) {
                        if (currentLanguage != AppConstants.ARABIC) {
                            UserSessionManager.getInstance(AppController.getContext())
                                    .setLanguage(AppConstants.ARABIC)
//                        setNewLocale(this, AppConstants.ARABIC)
//
                            val intent = baseContext.packageManager.getLaunchIntentForPackage(
                                    baseContext.packageName
                            )
                            intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                        }


                    } else if (it.itemId == R.id.english) {
                        if (currentLanguage != AppConstants.ENGLISH) {
                            UserSessionManager.getInstance(AppController.getContext())
                                    .setLanguage(AppConstants.ENGLISH)
//                        setNewLocale(this, AppConstants.ENGLISH)
                            val intent = baseContext.packageManager.getLaunchIntentForPackage(
                                    baseContext.packageName
                            )
                            intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                        }


                    }
                    true
                }
                languageMenu.show()
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
                LocationUpdatesService.shared!!.requestLocationUpdates()
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
                    this@TaskActivity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    AppConstants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    private fun stopTracking() {
        if (LocationUpdatesService.shared != null)
            LocationUpdatesService.shared!!.removeLocationUpdates()
    }


    private fun forceUpdate() {
        showProgress()
        if (NetworkManager().isNetworkAvailable(this)) {
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.forceUpdate()
            NetworkManager().request(endPoint, object : INetworkCallBack<ApiResponse<String>> {
                override fun onFailed(error: String) {
                    hideProgress()
                    Alert.showMessage(
                            this@TaskActivity,
                            getString(R.string.no_internet)
                    )
                }

                override fun onSuccess(response: ApiResponse<String>) {
                    if (response.Status == AppConstants.STATUS_SUCCESS) {
                        //stop  tracking service
                        lastVerion = response.ResponseObj!!.toInt()
                        if (lastVerion > BuildConfig.VERSION_CODE)
                            showDilogUpdate()

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


    private fun showDilogUpdate() {
        val builder = AlertDialog.Builder(this@TaskActivity)
        builder.setTitle(getString(R.string.update))
        builder.setMessage(getString(R.string.please_update))
        builder.setPositiveButton(getString(R.string.update_now)) { dialogInterface, i ->
            val uri = Uri.parse("market://details?id=com.kadabra.courier")
            val goToMarket = Intent(Intent.ACTION_VIEW, uri)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                goToMarket.addFlags(
                        Intent.FLAG_ACTIVITY_NO_HISTORY or
                                Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                                Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                )
                startActivity(goToMarket)
            } else {
                startActivity(
                        Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://play.google.com/store/apps/details?id=com.kadabra.courier")
                        )
                )
            }
        }
        val alertDialog = builder.create()
        alertDialog.setCancelable(false)
        if (!this@TaskActivity.isFinishing) {
            alertDialog.show()
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

        }
    }


    //endregion


}

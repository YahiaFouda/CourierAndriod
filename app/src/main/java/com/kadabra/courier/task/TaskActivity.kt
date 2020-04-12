package com.kadabra.courier.task

import android.Manifest
import android.app.ActivityManager
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.*
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
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.*
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.kadabra.courier.BuildConfig
import com.kadabra.courier.base.BaseNewActivity
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.notifications.NotificationActivity
import com.kadabra.courier.services.LocationUpdatesService
import com.kadabra.courier.utilities.LocaleManager
import kotlinx.android.synthetic.main.activity_task.avi
import kotlinx.android.synthetic.main.activity_task.ivAccept
import kotlinx.android.synthetic.main.activity_task.ivNoInternet
import kotlinx.android.synthetic.main.activity_task.refresh
import kotlinx.android.synthetic.main.activity_task.rvTasks
import kotlinx.android.synthetic.main.activity_task.tvEmptyData
import kotlinx.android.synthetic.main.activity_task.tvTimer
import kotlin.collections.ArrayList


class TaskActivity : BaseNewActivity(), View.OnClickListener,
    NavigationView.OnNavigationItemSelectedListener {

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
    private var KEY_TASK_LIST_DATA = "taskList"
    private var mLocationPermissionGranted = false
    private var alertDialog: AlertDialog? = null
    private var serviceCostView: View? = null
    private var ivLanguageBack: ImageView? = null
    private var rbArabic: RadioButton? = null
    private var rbEnglish: RadioButton? = null
    private var lang = ""


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

        serviceCostView = View.inflate(this, R.layout.choose_language_layout, null)
        ivLanguageBack = serviceCostView!!.findViewById<ImageView>(R.id.ivBackLanguage)
        rbArabic = serviceCostView!!.findViewById<RadioButton>(R.id.rbArabic)
        rbEnglish = serviceCostView!!.findViewById<RadioButton>(R.id.rbEnglish)

        ivAccept.setOnClickListener(this)
        ivLanguageBack!!.setOnClickListener(this)
        rbArabic!!.setOnClickListener(this)
        rbEnglish!!.setOnClickListener(this)

        tvTimer.visibility = View.INVISIBLE

        chooseLanguageWindow()

        refresh.setOnRefreshListener {
            loadTasks()

        }



        setSupportActionBar(toolbar)
        this.title = ""//getString(R.string.beta_version)
        val toggle = ActionBarDrawerToggle(
            this, drawer, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        var headerView = navigationView.getHeaderView(0)
        var tvName = headerView.findViewById<TextView>(R.id.tvName)
        var tvPhoneNo = headerView.findViewById<TextView>(R.id.tvPhoneNo)
        tvName.text = AppConstants.CurrentLoginCourier.CourierName
        tvPhoneNo.text = AppConstants.CurrentLoginCourier.Mobile


        navigationView.setNavigationItemSelectedListener(this)

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

        ivAccept.visibility = View.GONE
        rippleBackground.visibility = View.INVISIBLE
        rippleBackground.stopRippleAnimation()
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
                            FirebaseManager.updateCourierActive(
                                AppConstants.CurrentLoginCourier.CourierId,
                                false
                            )
//                            FirebaseManager.logOut()

                            UserSessionManager.getInstance(this@TaskActivity).logout()
//                            UserSessionManager.getInstance(this@TaskActivity).setUserData(Courier())
//                            UserSessionManager.getInstance(this@TaskActivity).setIsLogined(false)
//                            UserSessionManager.getInstance(this@TaskActivity).setFirstTime(false)

                            startActivity(Intent(this@TaskActivity, LoginActivity::class.java))
                            if (countDownTimer != null)
                                countDownTimer!!.cancel()
                            cancelVibrate()
                            stopSound()
                            stopTracking()
                            hideProgress()
                            finish()

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
        ivAccept.visibility = View.VISIBLE
        rippleBackground.visibility = View.VISIBLE
        rippleBackground.startRippleAnimation()
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

                        prepareTasks(AppConstants.ALL_TASKS_DATA)
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
        Log.d(TAG, "loadTasks: Enter method")
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
                        Log.d(TAG, "onFailed: " + error)
                        refresh.isRefreshing = false
                        hideProgress()
                        Alert.showMessage(
                            this@TaskActivity,
                            getString(R.string.error_login_server_error)
                        )
                    }

                    override fun onSuccess(response: ApiResponse<ArrayList<Task>>) {
                        Log.d(TAG, "onSuccess: Enter method")
                        if (response.Status == AppConstants.STATUS_SUCCESS) {
                            Log.d(
                                TAG,
                                "onSuccess: AppConstants.STATUS_SUCCESS: " + AppConstants.STATUS_SUCCESS
                            )

                            taskList = response.ResponseObj!!

                            if (taskList.size > 0) {
                                Log.d(TAG, "onSuccess: taskList.size > 0: ")
                                AppConstants.ALL_TASKS_DATA = taskList
                                prepareTasks(taskList)

                                if (AppConstants.FIRE_BASE_NEW_TASK) {
                                    Log.d(
                                        TAG,
                                        "onSuccess: AppConstants.FIRE_BASE_NEW_TASK: " + AppConstants.FIRE_BASE_NEW_TASK
                                    )
                                    AppConstants.FIRE_BASE_NEW_TASK = false
                                    processTask()//new task is arrived but not accepted and the view is minimized and maximized so show tier and accept to accept
                                }

                                refresh.isRefreshing = false
                                tvEmptyData.visibility = View.INVISIBLE
                                hideProgress()
                            } else {//no tasks
                                Log.d(TAG, "no tasks: ")
                                refresh.isRefreshing = false
                                tvTimer.visibility = View.INVISIBLE
                                tvEmptyData.visibility = View.VISIBLE
                                taskList.clear()
                                prepareTasks(taskList)
                            }


                        } else {
                            Log.d(TAG, "onSuccess: Enter method")
                            refresh.isRefreshing = false
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

    private fun prepareTasks(tasks: ArrayList<Task>) {
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
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgress() {
        avi.smoothToHide()
        tvEmptyData.visibility = View.INVISIBLE
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
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

    //region Events
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // recovering the instance state
        setContentView(R.layout.activity_task)
//        Crashlytics.getInstance().crash();
        Log.d(TAG, "onCreate")
        myReceiver = MyReceiver()
        FirebaseManager.setUpFirebase()
        init()
//        loadTasks()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
        navigationView.menu.forEach { item -> item.isChecked = false }
        if (adapter != null)
            adapter!!.notifyDataSetChanged()

        if (checkMapServices()) {
            if (mLocationPermissionGranted) {
                Log.d(TAG, "onResume-mLocationPermissionGranted-loadTasks")
                loadTasks()
//                getCurrentActiveTask()
                getCurrentCourierLocation()
                forceUpdate()
            } else {
                Log.d(TAG, "onResume-No perission")
                getLocationPermission()
            }
        }


//        if (AppConstants.ALL_TASKS_DATA.size > 0)
//            prepareTasks(AppConstants.ALL_TASKS_DATA)


        LocalBroadcastManager.getInstance(this).registerReceiver(
            myReceiver!!,
            IntentFilter(LocationUpdatesService.ACTION_BROADCAST)
        )


        updateLanguage(AppConstants.CurrentLoginCourier.CourierId,UserSessionManager.getInstance(this).getLanguage())
    }

    override fun onStart() {
        super.onStart()


        Log.d(TAG, "onStart")
        bindService(

            Intent(this, LocationUpdatesService::class.java), mServiceConnection,
            Context.BIND_AUTO_CREATE

        )




        if (AppConstants.endTask)
            AppConstants.endTask = false

        if (AppConstants.FIRE_BASE_LOGOUT)
            logOut()


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

            R.id.ivAccept -> {
                if (taskList.size > 0) {
                    accept()
                } else
                    Alert.showMessage(
                        this,
                        getString(R.string.no_tasks_available)
                    )

            }

            R.id.ivBackLanguage -> {
                alertDialog!!.dismiss()
            }
            R.id.rbArabic -> {
                if (lang != AppConstants.ARABIC)
                    setNewLocale(this, AppConstants.ARABIC)
            }
            R.id.rbEnglish -> {
                if (lang != AppConstants.ENGLISH)
                    setNewLocale(this, AppConstants.ENGLISH)
            }


        }
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        mLocationPermissionGranted = false
        Log.i(TAG, "onRequestPermissionResult")
        if (requestCode == AppConstants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION && grantResults.isNotEmpty()
        ) {
//            if (grantResults.size <= 0) {
//                // If user interaction was interrupted, the permission request is cancelled and you
//                // receive empty arrays.
//                Log.i(TAG, "User interaction was cancelled.")
//            } else
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted.
                Log.d(TAG, "onRequestPermissionsResult-mLocationPermissionGranted-loadTasks")
//                if(!isLocationServiceRunning())
                LocationUpdatesService.shared!!.requestLocationUpdates()
                mLocationPermissionGranted = true
                loadTasks()
//                getCurrentActiveTask()
                getCurrentCourierLocation()
                forceUpdate()

            }
//                else {
//                if (UserSessionManager.getInstance(this).requestingLocationUpdates()) {
//                    if (!checkPermissions()) {
//                        requestPermissions()
//                    }
//                }
//            }
        }
//        if (!LocationHelper.shared.isGPSEnabled())
//            Snackbar.make(
//                    findViewById(R.id.rlParent),
//                    R.string.permission_denied_explanation,
//                    Snackbar.LENGTH_INDEFINITE
//            )
//                    .setAction(R.string.settings) {
//                        // Build intent that displays the App settings screen.
//                        val intent = Intent()
//                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
//                        val uri = Uri.fromParts(
//                                "package",
//                                BuildConfig.APPLICATION_ID, null
//                        )
//                        intent.data = uri
//                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                        startActivity(intent)
//                    }
//                    .show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
//            R.id.nav_tasks -> {
//                startActivity(Intent(this,NotificationActivity::class.java))
//            }
//            R.id.nav_Notification -> {
//                startActivity(Intent(this,NotificationActivity::class.java))
//            }
            R.id.nav_task_history -> {
                startActivity(Intent(this, TaskHistoryActivity::class.java))


            }
            R.id.nav_Settings -> {
                if (!isNewTaskReceived) {
//                    var currentLanguage = UserSessionManager.getInstance(this).getLanguage()
//
////                    var view=findViewById<item>( R.id.nav_Settings)
//                    languageMenu = PopupMenu(this, ivAccept)
//                    languageMenu.menuInflater.inflate(R.menu.menu_language, languageMenu.menu)
//
//                    languageMenu.setOnMenuItemClickListener {
//                        if (it.itemId == R.id.arabic) {
//                            if (currentLanguage != AppConstants.ARABIC) {
//                                UserSessionManager.getInstance(AppController.getContext())
//                                    .setLanguage(AppConstants.ARABIC)
////
//                                val intent = baseContext.packageManager.getLaunchIntentForPackage(
//                                    baseContext.packageName
//                                )
//                                intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                                startActivity(intent)
//                            }
//
//
//                        } else if (it.itemId == R.id.english) {
//                            if (currentLanguage != AppConstants.ENGLISH) {
//                                UserSessionManager.getInstance(AppController.getContext())
//                                    .setLanguage(AppConstants.ENGLISH)
////                        setNewLocale(this, AppConstants.ENGLISH)
//                                val intent = baseContext.packageManager.getLaunchIntentForPackage(
//                                    baseContext.packageName
//                                )
//                                intent!!.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//                                startActivity(intent)
//                            }
//
//
//                        }
//                        true
//                    }
//                    languageMenu.show()

                    alertDialog!!.show()
                }

            }
            R.id.nav_Call -> {
                val intent = Intent(Intent.ACTION_DIAL)
                startActivity(intent)
            }
            R.id.nav_logout -> {
                if (!isNewTaskReceived)
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

        }
        drawer.closeDrawer(GravityCompat.START)
        return true
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_option, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == R.id.action_notification) {
            if (drawer.isDrawerOpen(GravityCompat.START)) {
                drawer.closeDrawer(GravityCompat.START)
            }
            startActivity(Intent(this, NotificationActivity::class.java))
        }


        return true
        //open notification view
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


    private fun checkMapServices(): Boolean {
        if (isServicesOK()) {
            if (isMapsEnabled()) {
                return true
            }
        }
        return false
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
        builder.setMessage(getString(R.string.error_gps_required))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.ok)) { dialog, id ->
                val enableGpsIntent =
                    Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(enableGpsIntent, AppConstants.PERMISSIONS_REQUEST_ENABLE_GPS)
            }
        val alert = builder.create()
        alert.show()
    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) === PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "getLocationPermission-mLocationPermissionGranted-loadTasks")
            mLocationPermissionGranted = true
            loadTasks()
//            getCurrentActiveTask()
            getCurrentCourierLocation()
            forceUpdate()

        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                AppConstants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: called.")
        when (requestCode) {
            AppConstants.PERMISSIONS_REQUEST_ENABLE_GPS -> {
                if (mLocationPermissionGranted) {
                    Log.d(TAG, "onActivityResult-mLocationPermissionGranted-loadTasks")
//                    LocationUpdatesService.shared!!.requestLocationUpdates()
                    loadTasks()
//                    getCurrentActiveTask()
                    getCurrentCourierLocation()
                    forceUpdate()

                } else {
                    Log.d(TAG, "getLocationPermission-Request")
                    getLocationPermission()
                }
            }
        }
    }

    private fun isLocationServiceRunning(): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if ("com.kadabra.courier.services.LocationUpdatesService" == service.service.className) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.")
                return true
            }
        }
        Log.d(TAG, "isLocationServiceRunning: location service is not running.")
        return false
    }

    private fun chooseLanguageWindow() {

        var alert = AlertDialog.Builder(this)
        alertDialog = alert.create()


        lang = UserSessionManager.getInstance(AppController.getContext())
            .getLanguage()
        if (lang == AppConstants.ARABIC)
            rbArabic!!.isChecked = true
        else if (lang == AppConstants.ENGLISH)
            rbEnglish!!.isChecked = true

        alertDialog!!.setView(serviceCostView)


    }

    fun setNewLocale(mContext: AppCompatActivity, @LocaleManager.LocaleDef language: String) {
        UserSessionManager.getInstance(this).setLanguage(language)
        val intent = mContext.intent
        mContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))

    }

    private fun updateLanguage(courierId:Int,languageType:String) {

        if (NetworkManager().isNetworkAvailable(this)) {
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.updateCourierLanguage(courierId,languageType)
            NetworkManager().request(endPoint, object : INetworkCallBack<ApiResponse<Boolean?>> {
                override fun onFailed(error: String) {

                }

                override fun onSuccess(response: ApiResponse<Boolean?>) {
                    if (response.Status == AppConstants.STATUS_SUCCESS) {

                    } else {

                    }

                }
            })

        } else {

        }


    }

        //endregion


    }

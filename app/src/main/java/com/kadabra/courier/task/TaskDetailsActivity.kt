package com.kadabra.courier.task

import android.Manifest
import android.app.AlertDialog
import android.content.*
import android.content.pm.PackageManager
import android.location.Location
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.location.LocationResult
import com.google.android.material.snackbar.Snackbar
import com.kadabra.Networking.INetworkCallBack
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.BuildConfig
import com.kadabra.courier.R
import com.kadabra.courier.adapter.StopAdapter
import com.kadabra.courier.api.ApiResponse
import com.kadabra.courier.api.ApiServices
import com.kadabra.courier.base.BaseNewActivity
import com.kadabra.courier.callback.ILocationListener
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.location.LocationHelper
import com.kadabra.courier.login.LoginActivity
import com.kadabra.courier.model.*
import com.kadabra.courier.services.LocationUpdatesService
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.AppController
import com.kadabra.courier.utilities.UserSessionManager
import kotlinx.android.synthetic.main.activity_task_details.*


class TaskDetailsActivity : BaseNewActivity(), View.OnClickListener, ILocationListener {


    //region Members
    private var TAG = this.javaClass.simpleName
    private var task = Task()
    private var pickUpStops = ArrayList<Stop>()
    private var dropOffStops = ArrayList<Stop>()
    private var normalStops = ArrayList<Stop>()
    private var lastLocation: Location? = null
    private var mBound = false
    private var myReceiver: MyReceiver? = null
    var isStarted = false
    private var alertDialog: AlertDialog? = null
    private lateinit var rbCash: RadioButton
    private lateinit var rbCredit: RadioButton
    private lateinit var rbWallet: RadioButton
    private lateinit var rbNoCollection: RadioButton
    private lateinit var tvReceiptDetails: TextView

    private lateinit var ivPaymentBack: ImageView
    private lateinit var etAmount: EditText
    private lateinit var btnPaymentEndTask: Button
    private var paymentTypeView: View? = null
    private var totalReceiptValue = 0.0


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

        tvFrom.setOnClickListener(this)
        tvTo.setOnClickListener(this)
        btnEndTask.setOnClickListener(this)
        btnLocation.setOnClickListener(this)
        ivBack.setOnClickListener(this)
        ivMessage.setOnClickListener(this)

        paymentTypeView = View.inflate(this, R.layout.payment_type_layout, null)
        ivPaymentBack = paymentTypeView!!.findViewById(R.id.ivPaymentBack)
        rbCash = paymentTypeView!!.findViewById<RadioButton>(R.id.rbCash)
        rbCredit = paymentTypeView!!.findViewById(R.id.rbCredit)
        rbWallet = paymentTypeView!!.findViewById(R.id.rbWallet)
        rbNoCollection = paymentTypeView!!.findViewById(R.id.rbNoCollection)
        tvReceiptDetails = paymentTypeView!!.findViewById(R.id.tvReceiptDetails)
        etAmount = paymentTypeView!!.findViewById(R.id.etAmount)
        btnPaymentEndTask = paymentTypeView!!.findViewById(R.id.btnPaymentEndTask)

        ivPaymentBack.setOnClickListener(this)
        rbCash.setOnClickListener(this)
        rbCredit.setOnClickListener(this)
        rbWallet.setOnClickListener(this)
        rbNoCollection.setOnClickListener(this)
        tvReceiptDetails.setOnClickListener(this)
        btnPaymentEndTask.setOnClickListener(this)
        ivCall.setOnClickListener(this)

        task = AppConstants.CurrentSelectedTask
        loadTaskDetails(task)

        refresh.setOnRefreshListener {
            getTaskDetails(task.TaskId)
        }


//        if (AppConstants.CurrentSelectedTask.Status == inProgress)
//            btnEndTask.text = getString(R.string.end_task)
//
//        if (AppConstants.CurrentSelectedTask.IsStarted)
//            btnEndTask.text = getString(R.string.start_task)

    }

    private fun showPickUpLocation() {
        startActivity(Intent(this, TaskLocationsActivity::class.java))
    }

    private fun showDropOffLocation() {
        startActivity(Intent(this, TaskLocationsActivity::class.java))

    }


    private fun loadTaskStops(stopList: ArrayList<Stop>) {
        var adapter = StopAdapter(this@TaskDetailsActivity, stopList)
        rvStops.adapter = adapter
        adapter.notifyDataSetChanged()
        rvStops?.layoutManager =
            GridLayoutManager(
                AppController.getContext(),
                1,
                GridLayoutManager.VERTICAL,
                false
            )
        adapter.notifyDataSetChanged()


    }

    private fun loadTaskDetails(task: Task) {
        tvTask.text = task.TaskName
        tvAgentName.text = task.AgentName
        tvTaskDescription.text = task.TaskDescription

        when (task.Status) {
            AppConstants.NEW -> {
                tvStatus.text = getString(R.string.new_task)
                btnEndTask.text = getString(R.string.accept_task)
            }
            AppConstants.WAITING -> {
                tvStatus.text = getString(R.string.ready_to_start)
                btnEndTask.text = getString(R.string.start)
            }
            AppConstants.CANCELLED -> {
                tvStatus.text = getString(R.string.cancelled)
                btnEndTask.text = getString(R.string.start)
                btnEndTask.isEnabled = false
                btnEndTask.setBackgroundResource(R.drawable.rounded_button_disenaple)
            }
            AppConstants.IN_PROGRESS -> {

                tvStatus.text = getString(R.string.in_progress)
                btnEndTask.text = getString(R.string.end_task)

            }

        }



        if (task.Amount!! > 0)
            tvTaskAmount.text =
                task.Amount.toString() + " " + getString(R.string.le)

        if (task.stopsmodel.size > 0) {
            if (task.stopPickUp != null)
                tvFrom.text =
                    getString(R.string.from) + " " + task.stopPickUp.StopName
            if (task.stopDropOff != null)
                tvTo.text =
                    getString(R.string.to) + " " + task.stopDropOff.StopName

            if (task.stopsmodel.size > 0) {
                var stops = prepareTaskStops(task.stopsmodel)
                task.stopsmodel = stops
                loadTaskStops(task.stopsmodel)
//                getTaskDetails(task.TaskId)
            }

        } else {
            tvFrom.text =
                getString(R.string.from) + " " + getString(R.string.no_stop)
            tvTo.text =
                getString(R.string.to) + " " + getString(R.string.no_stop)
            tvStops.visibility = View.INVISIBLE
        }

        if (!AppConstants.CurrentEditedTask.TaskId.isNullOrEmpty())
            btnEndTask.text = getString(R.string.start)

    }

    private fun prepareTaskStops(stops: ArrayList<Stop>): ArrayList<Stop> {
        pickUpStops.clear()
        dropOffStops.clear()
        normalStops.clear()

        if (stops.size > 0) {
//            stops.sortBy { it.StopTypeID }

            stops.forEach {

                when (it.StopTypeID) {
                    1 -> { //pickup
                        pickUpStops.add(it)
                    }
                    2 -> { //dropOff
                        dropOffStops.add(it)
                    }
                    3 -> {
                        normalStops.add(it)
                    }

                }
            }

            stops.clear()

            if (pickUpStops.count() > 0)
                stops.addAll(pickUpStops)
            if (normalStops.count() > 0) {
//                normalStops.reverse()
                stops.addAll(normalStops)
            }

            if (dropOffStops.count() > 0)
                stops.addAll(dropOffStops)

        }

        return stops

    }

    private fun getTaskDetails(taskId: String) {
        refresh.isRefreshing = false
        showProgress()
        if (NetworkManager().isNetworkAvailable(this)) {
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.getTaskDetails(taskId)
            NetworkManager().request(
                endPoint,
                object : INetworkCallBack<ApiResponse<Task>> {
                    override fun onFailed(error: String) {
                        hideProgress()
                        Alert.showMessage(
                            this@TaskDetailsActivity,
                            getString(R.string.error_login_server_error)
                        )
                    }

                    override fun onSuccess(response: ApiResponse<Task>) {
                        if (response.Status == AppConstants.STATUS_SUCCESS) {
                            hideProgress()
                            task = response.ResponseObj!!
                            AppConstants.CurrentSelectedTask = task
                            AppConstants.CurrentEditedTask=task
                            loadTaskDetails(task)

                        } else {
                            hideProgress()
                            Alert.showMessage(
                                this@TaskDetailsActivity,
                                getString(R.string.error_network)
                            )
                        }

                    }
                })

        } else {
            hideProgress()
            Alert.showMessage(
                this@TaskDetailsActivity,
                getString(R.string.no_internet)
            )
        }


    }

    private fun acceptTask(taskId: String) {
        showProgress()
        if (NetworkManager().isNetworkAvailable(this)) {
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.acceptTask(taskId)
            NetworkManager().request(endPoint, object : INetworkCallBack<ApiResponse<Task>> {
                override fun onFailed(error: String) {
                    hideProgress()
                    Alert.showMessage(
                        this@TaskDetailsActivity,
                        getString(R.string.error_login_server_unknown_error)
                    )
                }

                override fun onSuccess(response: ApiResponse<Task>) {
                    if (response.Status == AppConstants.STATUS_SUCCESS) {
                        tvStatus.text = getString(R.string.ready_to_start)//task.Status//
                        task.Status = AppConstants.WAITING
                        AppConstants.CurrentAcceptedTask = task
                        AppConstants.CurrentSelectedTask = task
                        acceptTaskFirebase(task, AppConstants.CurrentLoginCourier.CourierId)
                        hideProgress()
                    } else {
                        hideProgress()
                        Alert.showMessage(
                            this@TaskDetailsActivity,
                            getString(R.string.error_network)
                        )
                    }

                }
            })

        } else {
            hideProgress()
            Alert.showMessage(
                this@TaskDetailsActivity,
                getString(R.string.no_internet)
            )
        }


    }

    private fun endTask(taskId: String) {
        showProgress()
        if (NetworkManager().isNetworkAvailable(this)) {
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.endTask(taskId)
            NetworkManager().request(endPoint, object : INetworkCallBack<ApiResponse<Task>> {
                override fun onFailed(error: String) {
                    hideProgress()
                    Alert.showMessage(
                        this@TaskDetailsActivity,
                        getString(R.string.error_login_server_unknown_error)
                    )
                }

                override fun onSuccess(response: ApiResponse<Task>) {
                    if (response.Status == AppConstants.STATUS_SUCCESS) {

                        FirebaseManager.endTask(task, AppConstants.CurrentLoginCourier.CourierId)
                        AppConstants.CurrentAcceptedTask = Task()
                        AppConstants.CurrentSelectedTask = Task()
                        AppConstants.ALL_TASKS_DATA.remove(task) //removed when life cycle
                        hideProgress()
                        AppConstants.endTask = true
                        //load new task or shoe empty tasks view
                        finish()

                    } else {
                        hideProgress()
                        Alert.showMessage(
                            this@TaskDetailsActivity,
                            getString(R.string.error_network)
                        )
                    }

                }
            })

        } else {
            hideProgress()
            Alert.showMessage(
                this@TaskDetailsActivity,
                getString(R.string.no_internet)
            )
        }


    }

    private fun endTask(taskId: String, paymentType: Int, amount: Double) {
        Alert.showProgress(this)
        if (NetworkManager().isNetworkAvailable(this)) {
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.endTask(taskId, paymentType, amount)
            NetworkManager().request(endPoint, object : INetworkCallBack<ApiResponse<Task>> {
                override fun onFailed(error: String) {
                    Alert.hideProgress()
                    Alert.showMessage(
                        this@TaskDetailsActivity,
                        getString(R.string.error_login_server_unknown_error)
                    )
                    btnPaymentEndTask.isEnabled = true
                }

                override fun onSuccess(response: ApiResponse<Task>) {
                    if (response.Status == AppConstants.STATUS_SUCCESS) {

                        FirebaseManager.endTask(task, AppConstants.CurrentLoginCourier.CourierId)
                        AppConstants.CurrentAcceptedTask = Task()
                        AppConstants.CurrentSelectedTask = Task()
                        AppConstants.ALL_TASKS_DATA.remove(task) //removed when life cycle
                        Alert.hideProgress()
                        AppConstants.endTask = true
                        //load new task or shoe empty tasks view
                        btnPaymentEndTask.isEnabled = true
                        finish()

                    } else if (response.Status == AppConstants.STATUS_FAILED)  // -1  "this task already ended before"
                    {
                        Alert.hideProgress()
                        Alert.showMessage(
                            this@TaskDetailsActivity,
                            getString(R.string.error_end)
                        )
                        btnPaymentEndTask.isEnabled = true
                    } else if (response.Status == AppConstants.STATUS_FAILED_2) //-2  An Error Occured
                    {
                        Alert.hideProgress()
                        Alert.showMessage(
                            this@TaskDetailsActivity,
                            getString(R.string.error_login_server_unknown_error)
                        )
                        btnPaymentEndTask.isEnabled = true
                    } else if (response.Status == AppConstants.STATUS_INCORRECT_DATA) //-3  you don't have enough balance in wallet
                    {
                        Alert.hideProgress()
                        Alert.showMessage(
                            this@TaskDetailsActivity,
                            getString(R.string.error_wallet)
                        )
                        btnPaymentEndTask.isEnabled = true
                    } else if (response.Status == AppConstants.STATUS_FAILED_4) //-4  you don't have Credit card in kadabra
                    {
                        Alert.hideProgress()
                        Alert.showMessage(
                            this@TaskDetailsActivity,
                            getString(R.string.error_credit)
                        )
                        btnPaymentEndTask.isEnabled = true
                    }


                }
            })

        } else {
            Alert.hideProgress()
            Alert.showMessage(
                this@TaskDetailsActivity,
                getString(R.string.no_internet)
            )
            btnPaymentEndTask.isEnabled = true
        }


    }


    private fun acceptTaskFirebase(task: Task, courierId: Int) {
        if (NetworkManager().isNetworkAvailable(this)) {
            FirebaseManager.createNewTask(task, courierId)
            startActivity(
                Intent(
                    this@TaskDetailsActivity,
                    TaskLocationsActivity::class.java
                ).putExtra("startTask", true)
            )

        } else {

            Alert.showMessage(
                this@TaskDetailsActivity,
                getString(R.string.no_internet)
            )
        }


    }

    private fun showProgress() {
        avi.visibility = View.VISIBLE
        avi.smoothToShow()
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgress() {
        avi.smoothToHide()
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    //endregion
    //region Events
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_details)
        myReceiver = MyReceiver()
        init()


    }


    override fun onStart() {
        super.onStart()

        if (!checkPermissions()) {
            requestPermissions()
        }

        bindService(
            Intent(this, LocationUpdatesService::class.java), mServiceConnection,
            Context.BIND_AUTO_CREATE
        )


    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(
            myReceiver!!,
            IntentFilter(LocationUpdatesService.ACTION_BROADCAST)
        )

        if (intent.hasExtra("editTAskId") || !AppConstants.CurrentEditedTask.TaskId.isNullOrEmpty()) {
            Log.d(TAG,"editTAskId:$AppConstants.CurrentEditedTask.TaskId")
            var taskId =
                AppConstants.CurrentEditedTask.TaskId// intent.getStringExtra("editTAskId")
            if (!taskId.isNullOrEmpty()) {
                getTaskDetails(taskId)
                // startActivity(Intent(this, TaskDetailsActivity::class.java).putExtra("editTaskId",AppConstants.CurrentSelectedMessage.taskId))

            }
        } else if (!AppConstants.CurrentSelectedTask.TaskId.isNullOrEmpty()) {
            getTaskDetails(AppConstants.CurrentSelectedTask.TaskId)
            Log.d(TAG, "ON RESUME")
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
            R.id.tvFrom -> {

                showPickUpLocation()
            }
            R.id.tvTo -> {
                showDropOffLocation()
            }
            R.id.btnEndTask -> {
                btnEndTask.isEnabled = false
                if (NetworkManager().isNetworkAvailable(this)) {
                    if (!checkPermissions()) {
                        requestPermissions()
                    } else {
                        if (AppConstants.CurrentSelectedTask.Status == AppConstants.IN_PROGRESS) //end task
                        {
                            isStarted =
                                AppConstants.CurrentSelectedTask.IsStarted

                            if (!isStarted || !AppConstants.CurrentEditedTask.TaskId.isNullOrEmpty()) {
                                startActivity(
                                    Intent(
                                        this@TaskDetailsActivity,
                                        TaskLocationsActivity::class.java
                                    ).putExtra("startTask", true)
                                )

                            } else {
//                                endTask(AppConstants.CurrentSelectedTask)
                                if (alertDialog != null)
                                    alertDialog?.show()
                                else {
                                    choosePaymentTypeWindow()
                                    alertDialog?.show()
                                }

                            }

                        } else //accept task
                        {
                            AppConstants.CurrentAcceptedTask = AppConstants.CurrentSelectedTask
                            AppConstants.CurrentAcceptedTask.isActive = true
                            AppConstants.CurrentAcceptedTask.CourierID =
                                AppConstants.CurrentLoginCourier.CourierId
                            var isGpsEnabled = LocationHelper.shared.isGPSEnabled()
                            if (AppConstants.CurrentLocation != null)
                                AppConstants.CurrentAcceptedTask.location =
                                    location(
                                        AppConstants.CurrentLocation!!.latitude.toString(),
                                        AppConstants.CurrentLocation!!.longitude.toString(),
                                        isGpsEnabled
                                    )
                            acceptTask(AppConstants.CurrentAcceptedTask.TaskId)
                        }


                    }

                } else {
                    Alert.showMessage(
                        this@TaskDetailsActivity,
                        getString(R.string.no_internet)
                    )
                }

                btnEndTask.isEnabled = true
            }

            R.id.ivBack -> {
                if (!AppConstants.CurrentEditedTask.TaskId.isNullOrEmpty()) {
                    Alert.showMessage(getString(R.string.error_confirm))
                } else
                    finish()
            }
            R.id.ivMessage -> {
                ivMessage.isEnabled = false
                var media = MediaPlayer()

                if (NetworkManager().isNetworkAvailable(this)) {
                    FirebaseManager.getTaskRecord(AppConstants.CurrentSelectedTask.TaskId)
                    { sucess, data ->
                        if (sucess) {
                            val mediaPlayer = MediaPlayer()
                            mediaPlayer.setDataSource(data.toString())
                            mediaPlayer.setOnPreparedListener { player ->
                                player.start()
                            }
                            mediaPlayer.prepareAsync()
                            mediaPlayer.setOnCompletionListener { ivMessage.isEnabled = true }

                        } else {
                            Alert.showMessage(this, getString(R.string.error_no_audio))
                            ivMessage.isEnabled = true
                        }


                    }

                } else
                    Alert.showMessage(getString(R.string.no_internet))

            }

            R.id.tvReceiptDetails -> {
                //get receipt Data
                getReceiptData(AppConstants.CurrentSelectedTask.TaskId)
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
                        Alert.showMessage(getString(R.string.error_total))
                    else
                        endTask(
                            AppConstants.CurrentSelectedTask.TaskId,
                            1,
                            amount
                        )

                } else if (rbWallet.isChecked) {
                    endTask(
                        AppConstants.CurrentSelectedTask.TaskId,
                        2,
                        0.0
                    )
                } else if (rbCredit.isChecked) {
                    endTask(
                        AppConstants.CurrentSelectedTask.TaskId,
                        3,
                        0.0
                    )
                } else if (rbNoCollection.isChecked) {
                    endTask(
                        AppConstants.CurrentSelectedTask.TaskId,
                        4,
                        0.0
                    )
                } else
                    Alert.showMessage(getString(R.string.error_enter_total))
                //endTask
            }
            R.id.rbCash -> {
                etAmount.visibility = View.VISIBLE
            }
            R.id.rbCredit, R.id.rbWallet, R.id.rbNoCollection -> {
                resetAmount()
            }

            R.id.ivCall -> {
                if (!AppConstants.CurrentSelectedTask.AgentMobile.isNullOrEmpty()) {
                    Log.d(TAG, AppConstants.CurrentSelectedTask.AgentMobile)
                    val intent = Intent()
                    intent.action = Intent.ACTION_DIAL // Action for what intent called for
                    intent.data =
                        Uri.parse("tel: ${AppConstants.CurrentSelectedTask.AgentMobile}") // Data with intent respective action on intent
                    startActivity(intent)
                    Log.d(TAG, "DONE")
                } else
                    Alert.showMessage("This agent has no phone no.")
            }

        }
    }

    override fun locationResponse(locationResult: LocationResult) {
        lastLocation = locationResult.lastLocation
    }

    override fun onBackPressed() {
        if (AppConstants.CurrentEditedTask != null && !AppConstants.CurrentEditedTask.TaskId.isNullOrEmpty()) {
            Alert.showMessage(getString(R.string.error_edit_task))
        } else
            finish()
    }

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
                        this@TaskDetailsActivity,
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
                this@TaskDetailsActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                AppConstants.PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
        }
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

    private fun logOut() {
//        if(AppConstants.CurrentAcceptedTask.TaskId.isNullOrEmpty())
//        {
        showProgress()
        if (NetworkManager().isNetworkAvailable(this)) {
            if (AppConstants.CurrentAcceptedTask.TaskId.isNullOrEmpty()) //no accepted task
            {
                var request = NetworkManager().create(ApiServices::class.java)
                var endPoint = request.logOut(AppConstants.CurrentLoginCourier.CourierId)
                NetworkManager().request(
                    endPoint,
                    object : INetworkCallBack<ApiResponse<Courier>> {
                        override fun onFailed(error: String) {
                            hideProgress()
                            Alert.showMessage(
                                this@TaskDetailsActivity,
                                getString(R.string.no_internet)
                            )
                        }

                        override fun onSuccess(response: ApiResponse<Courier>) {
                            if (response.Status == AppConstants.STATUS_SUCCESS) {
                                //stop  tracking service
                                stopTracking()
                                FirebaseManager.logOut()

                                UserSessionManager.getInstance(this@TaskDetailsActivity)
                                    .logout()
                                startActivity(
                                    Intent(
                                        this@TaskDetailsActivity,
                                        LoginActivity::class.java
                                    )
                                )


                                //remove location update
//                        LocationHelper.shared.stopUpdateLocation()
                                finish()
                                hideProgress()

                            } else {
                                hideProgress()
                                Alert.showMessage(
                                    this@TaskDetailsActivity,
                                    getString(R.string.error_network)
                                )
                            }

                        }
                    })
            } else //accepted task prevent logout
            {
                hideProgress()
                Alert.showMessage(
                    this@TaskDetailsActivity,
                    getString(R.string.end_first)
                )
            }
        } else {
            hideProgress()
            Alert.showMessage(
                this@TaskDetailsActivity,
                getString(R.string.no_internet)
            )
        }


    }

    private fun stopTracking() {
        if (LocationUpdatesService.shared != null)
            LocationUpdatesService.shared!!.removeLocationUpdates()
    }


    //endregion

    private inner class MyReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val location =
                intent.getParcelableExtra<Location>(LocationUpdatesService.EXTRA_LOCATION)

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
                        this@TaskDetailsActivity,
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
                        AppConstants.CurrentEditedTask = Task()
                        AppConstants.COURIERSTARTTASK = false
                        AppConstants.ALL_TASKS_DATA.remove(AppConstants.CurrentSelectedTask) //removed when life cycle
                        Alert.hideProgress()
                        AppConstants.endTask = true
                        //load new task or shoe empty tasks view
                        startActivity(
                            Intent(
                                this@TaskDetailsActivity,
                                TaskActivity::class.java
                            )
                        )
                        finish()


                    } else if (response.Status == AppConstants.STATUS_SUCCESS) {

                    } else {
                        Alert.hideProgress()
                        Alert.showMessage(
                            this@TaskDetailsActivity,
                            getString(R.string.error_network)
                        )
                    }

                }
            })

        } else {
            Alert.hideProgress()
            Alert.showMessage(
                this@TaskDetailsActivity,
                getString(R.string.no_internet)
            )
        }


    }

    private fun resetAmount() {
        etAmount.visibility = View.INVISIBLE
        etAmount.text.clear()
        etAmount.hint = getString(R.string.le)
    }

    private fun choosePaymentTypeWindow() {

        var alert = AlertDialog.Builder(this)
        alertDialog = alert.create()

        alertDialog!!.setView(paymentTypeView)


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
                            this@TaskDetailsActivity,
                            getString(R.string.error_login_server_error)
                        )
                    }

                    override fun onSuccess(response: ApiResponse<ReceiptData>) {
                        if (response.Status == AppConstants.STATUS_SUCCESS) {
                            hideProgress()
                            var data = response.ResponseObj!!
                            totalReceiptValue = data.Sum
                            prepareReciptData(data)
                            Alert.hideProgress()
                        } else {
                            Alert.hideProgress()
                            Alert.showMessage(
                                this@TaskDetailsActivity,
                                getString(R.string.error_network)
                            )
                        }

                    }
                })

        } else {
            Alert.hideProgress()
            Alert.showMessage(
                this@TaskDetailsActivity,
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

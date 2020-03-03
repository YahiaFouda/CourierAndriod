package com.kadabra.courier.task

import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.gms.location.LocationResult
import com.kadabra.Networking.INetworkCallBack
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.R
import com.kadabra.courier.adapter.StopAdapter
import com.kadabra.courier.api.ApiResponse
import com.kadabra.courier.api.ApiServices
import com.kadabra.courier.callback.ILocationListener
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.location.LocationHelper
import com.kadabra.courier.model.Stop
import com.kadabra.courier.model.Task
import com.kadabra.courier.model.location
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.AppController
import kotlinx.android.synthetic.main.activity_task_details.*


class TaskDetailsActivity : AppCompatActivity(), View.OnClickListener, ILocationListener {


    //region Members
    private var task = Task()
    private var pickUpStops = ArrayList<Stop>()
    private var dropOffStops = ArrayList<Stop>()
    private var normalStops = ArrayList<Stop>()
    private var lastLocation: Location? = null

    //endregion
    //region Helper Functions
    private fun init() {

        FirebaseManager.setUpFirebase()

        tvFrom.setOnClickListener(this)
        tvTo.setOnClickListener(this)
        btnEndTask.setOnClickListener(this)
        btnLocation.setOnClickListener(this)
        ivBack.setOnClickListener(this)

        task = AppConstants.CurrentSelectedTask
        loadTaskDetails(task)

        refresh.setOnRefreshListener {
            getTaskDetails(task.TaskId)
        }

        if (AppConstants.CurrentAcceptedTask.TaskId.isNullOrEmpty()) //
            btnEndTask.text = getString(R.string.accept_task)
        else {
            if (AppConstants.CurrentSelectedTask.TaskId != AppConstants.CurrentAcceptedTask.TaskId) {
                btnEndTask.isEnabled = false
            } else {
                btnEndTask.isEnabled = true
                btnEndTask.text = getString(R.string.end_task)
            }
        }

    }

    private fun showPickUpLocation() {
        startActivity(Intent(this, LocationDetailsActivity::class.java))
    }

    private fun showDropOffLocation() {
        startActivity(Intent(this, LocationDetailsActivity::class.java))

    }


    private fun loadTaskStops(stopList: ArrayList<Stop>) {
        var adapter = StopAdapter(this@TaskDetailsActivity, stopList)
        rvStops.adapter = adapter
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
        if (!task.Task.isNullOrEmpty())
            tvTask.text = task.Task

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
                task.stopsmodel = prepareTaskStops(task.stopsmodel)
                loadTaskStops(task.stopsmodel)
            }

        } else {
            tvFrom.text =
                getString(R.string.from) + " " + getString(R.string.no_stop)
            tvTo.text =
                getString(R.string.to) + " " + getString(R.string.no_stop)
            tvStops.visibility = View.INVISIBLE
        }


    }

    private fun prepareTaskStops(stops: ArrayList<Stop>): ArrayList<Stop> {
        pickUpStops.clear()
        dropOffStops.clear()
        normalStops.clear()

        if (stops.size > 0) {
            stops.sortBy { it.StopTypeID }

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
            if (normalStops.count() > 0)
                stops.addAll(normalStops)
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

                        FirebaseManager.endTask(task)
                        AppConstants.CurrentAcceptedTask = Task()
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

    private fun acceptTask(task: Task) {
        showProgress()
        if (NetworkManager().isNetworkAvailable(this)) {
            FirebaseManager.createNewTask(task)
            hideProgress()
        } else {
            hideProgress()
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
        init()
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

                if (AppConstants.CurrentAcceptedTask.TaskId.isNullOrEmpty()) // create new task on fb
                {
                    AppConstants.CurrentAcceptedTask = AppConstants.CurrentSelectedTask
                    AppConstants.CurrentAcceptedTask.isActive = true
                    AppConstants.CurrentAcceptedTask.courierId =
                        AppConstants.CurrentLoginCourier.CourierId
                    var isGpsEnabled = LocationHelper.shared.isLocationEnabled()
                    if (AppConstants.CurrentLocation != null)
                        AppConstants.CurrentAcceptedTask.location =
                            location(
                                AppConstants.CurrentLocation!!.latitude.toString(),
                                AppConstants.CurrentLocation!!.longitude.toString(),
                                isGpsEnabled
                            )
                    acceptTask(AppConstants.CurrentAcceptedTask!!)
                    btnEndTask.text = getString(R.string.end_task)
                } else {
                    if (AppConstants.CurrentSelectedTask.TaskId != AppConstants.CurrentAcceptedTask.TaskId) {//not the opened task prevent any actions
                        btnEndTask.isEnabled = false
                        btnEndTask.setBackgroundColor(Color.GRAY)
                    } else {//the same task end task
                        btnEndTask.isEnabled = true
                        btnEndTask.text = getString(R.string.end_task)
                        endTask(task.TaskId)
                    }
                }

            }
            R.id.btnLocation -> {
//                var isGpsEnabled = LocationHelper.shared.isLocationEnabled()
//                task.location= location(AppConstants.CurrentLocation!!.latitude.toString(),AppConstants!!.CurrentLocation!!.longitude.toString(),isGpsEnabled)
//                FirebaseManager.updateTaskLocation(task)
            }
            R.id.ivBack -> {
                finish()
            }


        }
    }

    override fun locationResponse(locationResult: LocationResult) {
        lastLocation = locationResult.lastLocation
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }


    //endregion


}

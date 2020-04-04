package com.kadabra.courier.task

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.kadabra.Networking.INetworkCallBack
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.R
import com.kadabra.courier.adapter.TaskAdapter
import com.kadabra.courier.adapter.TaskHistoryAdapter
import com.kadabra.courier.api.ApiResponse
import com.kadabra.courier.api.ApiServices
import com.kadabra.courier.base.BaseNewActivity
import com.kadabra.courier.model.Task
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.Alert.hideProgress
import com.kadabra.courier.utilities.Alert.showProgress
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.AppController
import kotlinx.android.synthetic.main.activity_task_history.*

import java.util.*
import kotlin.collections.ArrayList


class TaskHistoryActivity : BaseNewActivity() {

    
     var TAG=this.javaClass.simpleName
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_history)


//        loadTasks()

        refresh.setOnRefreshListener {
            loadTasks()
        }

        ivBack.setOnClickListener{
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTasks()
    }

    private fun loadTasks() {
        Log.d(TAG, "loadTasks: Enter method")
        ivNoInternet.visibility = View.INVISIBLE
        avi.show()
        if (NetworkManager().isNetworkAvailable(this)) {
            ivNoInternet.visibility = View.INVISIBLE
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.getAllCourierTasksHistory(AppConstants.CurrentLoginCourier.CourierId)
            NetworkManager().request(
                endPoint,
                object : INetworkCallBack<ApiResponse<ArrayList<Task>>> {
                    override fun onFailed(error: String) {
                        Log.d(TAG, "onFailed: " + error)
                        refresh.isRefreshing = false
                        avi.hide()
                        Alert.showMessage(
                            this@TaskHistoryActivity,
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
                            refresh.isRefreshing = false
                            tvEmptyData.visibility = View.INVISIBLE
                           var taskList = response.ResponseObj!!
                            Log.d(TAG, "onSuccess" + taskList.size.toString())


                            if (taskList.size > 0) {
                                Log.d(TAG, "onSuccess: taskList.size > 0: ")
                                tvTotalTasks.text=taskList.size.toString()
//                                val totalCompleted = Collections.frequency(taskList, "Completed")
//                                val totalInProgress = Collections.frequency(taskList, "Completed")
//                                tvTotalInProgress.text=totalInProgress.toString()
//                                tvTotalCompleted.text=totalCompleted.toString()

                                prepareTasks(taskList)

                            } else {//no tasks
                                Log.d(TAG, "no tasks: ")
                                refresh.isRefreshing = false
                                tvEmptyData.visibility = View.VISIBLE
                                taskList.clear()
                                prepareTasks(taskList)
                            }

                            avi.hide()

                        } else {
                            Log.d(TAG, "onSuccess: Enter method")
                            refresh.isRefreshing = false
                            avi.hide()
                            tvEmptyData.visibility = View.VISIBLE
                        }

                    }
                })

        } else {
            refresh.isRefreshing = false
            avi.hide()
            ivNoInternet.visibility = View.VISIBLE
            Alert.showMessage(
                this@TaskHistoryActivity,
                getString(R.string.no_internet)
            )
        }


    }

    private fun prepareTasks(tasks: ArrayList<Task>) {
       var adapter = TaskHistoryAdapter(this@TaskHistoryActivity, tasks)
        rvTasks.adapter = adapter
        rvTasks?.layoutManager =
            GridLayoutManager(
                AppController.getContext(),
                1,
                GridLayoutManager.VERTICAL,
                false
            )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

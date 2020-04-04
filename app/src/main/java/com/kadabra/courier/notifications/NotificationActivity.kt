package com.kadabra.courier.notifications

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.kadabra.Networking.INetworkCallBack
import com.kadabra.Networking.NetworkManager

import com.kadabra.courier.R
import com.kadabra.courier.adapter.NotificationAdapter
import com.kadabra.courier.api.ApiResponse
import com.kadabra.courier.api.ApiServices
import com.kadabra.courier.base.BaseNewActivity
import com.kadabra.courier.model.Notification
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.AppController
import kotlinx.android.synthetic.main.activity_notification.*
import kotlinx.android.synthetic.main.activity_notification.avi
import kotlinx.android.synthetic.main.activity_notification.ivNoInternet
import kotlinx.android.synthetic.main.activity_notification.refresh
import kotlinx.android.synthetic.main.activity_notification.tvEmptyData


class NotificationActivity : BaseNewActivity() {

    private var TAG = this.javaClass.simpleName
    private var notificationsList = ArrayList<Notification>()
    private var adapter: NotificationAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //        int flags = WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        //                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        //                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON;
        //
        //        getWindow().addFlags(flags);

        setContentView(R.layout.activity_notification)

//        loadAllNotifications()

        refresh.setOnRefreshListener {
            loadAllNotifications()
        }

        ivBack.setOnClickListener {
            finish()
        }
    }


    private fun loadAllNotifications() {
        Log.d(TAG, "loadNotifications: Enter method")
        ivNoInternet.visibility = View.INVISIBLE
        showProgress()
        if (NetworkManager().isNetworkAvailable(this)) {
            ivNoInternet.visibility = View.INVISIBLE
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint =
                request.getAllCourierNotification(AppConstants.CurrentLoginCourier.CourierId)
            NetworkManager().request(
                endPoint,
                object : INetworkCallBack<ApiResponse<ArrayList<Notification>>> {
                    override fun onFailed(error: String) {
                        Log.d(TAG, "onFailed: " + error)
                        refresh.isRefreshing = false
                        hideProgress()
                        Alert.showMessage(
                            this@NotificationActivity,
                            getString(R.string.error_login_server_error)
                        )
                    }

                    override fun onSuccess(response: ApiResponse<ArrayList<Notification>>) {
                        Log.d(TAG, "onSuccess: Enter method")
                        if (response.Status == AppConstants.STATUS_SUCCESS) {
                            Log.d(
                                TAG,
                                "onSuccess: AppConstants.STATUS_SUCCESS: " + AppConstants.STATUS_SUCCESS
                            )
                            refresh.isRefreshing = false
                            tvEmptyData.visibility = View.INVISIBLE
                            notificationsList = response.ResponseObj!!
                            Log.d(TAG, "onSuccess" + notificationsList.size.toString())


                            if (notificationsList.size > 0) {
                                Log.d(TAG, "onSuccess: notificationsList.size > 0: ")
                                var counter = 0
                                notificationsList.forEach {
                                    if (!it.isReaded) {
                                        counter++
                                    }
                                }
                                tvTotalNotifications.text = notificationsList.size.toString()
                                tvTotalUnread.text = counter.toString()

                                counter = 0
                                prepareNotifications(notificationsList)
                                hideProgress()
                            } else {//no notifications
                                Log.d(TAG, "no Notifications: ")
                                refresh.isRefreshing = false
                                tvEmptyData.visibility = View.VISIBLE
                                notificationsList.clear()
                                prepareNotifications(notificationsList)
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
                this@NotificationActivity,
                getString(R.string.no_internet)
            )
        }


    }

    private fun prepareNotifications(Notifications: ArrayList<Notification>) {
        adapter = NotificationAdapter(this@NotificationActivity, Notifications)
        rvNotifications.adapter = adapter
        rvNotifications?.layoutManager =
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


    override fun onResume() {
        super.onResume()
        loadAllNotifications()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

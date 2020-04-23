package com.kadabra.courier.notifications

import android.os.Bundle
import android.util.Log
import com.kadabra.Networking.INetworkCallBack
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.R
import com.kadabra.courier.api.ApiResponse
import com.kadabra.courier.api.ApiServices
import com.kadabra.courier.base.BaseNewActivity

import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.Alert.hideProgress
import com.kadabra.courier.utilities.Alert.showProgress
import com.kadabra.courier.utilities.AppConstants
import kotlinx.android.synthetic.main.activity_notification.ivBack
import kotlinx.android.synthetic.main.activity_notification.refresh
import kotlinx.android.synthetic.main.activity_notification_details.*

class NotificationDetailsActivity : BaseNewActivity() {


    private var TAG = this.javaClass.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_details)

        loadData()

        if (!AppConstants.CurrentSelectedNotification.isReaded)
            updateNotificationRead(AppConstants.CurrentSelectedNotification.notificationId)

        ivBack.setOnClickListener {
            finish()
        }
    }


    private fun updateNotificationRead(id: String) {
        Log.d(TAG, "loadNotifications: Enter method")

        showProgress(this)
        if (NetworkManager().isNetworkAvailable(this)) {

            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint =
                request.updateReadNotification(id)
            NetworkManager().request(
                endPoint,
                object : INetworkCallBack<ApiResponse<Boolean>> {
                    override fun onFailed(error: String) {
                        Log.d(TAG, "onFailed: " + error)
                        refresh.isRefreshing = false
                        hideProgress()
                        Alert.showMessage(
                            this@NotificationDetailsActivity,
                            getString(R.string.error_login_server_error)
                        )
                    }

                    override fun onSuccess(response: ApiResponse<Boolean>) {
                        Log.d(TAG, "onSuccess: Enter method")
                        if (response.Status == AppConstants.STATUS_SUCCESS) {
                            Log.d(
                                TAG,
                                "onSuccess: AppConstants.STATUS_SUCCESS: " + AppConstants.STATUS_SUCCESS
                            )


                            refresh.isRefreshing = false
                            hideProgress()


                        } else {
                            Log.d(TAG, "onSuccess: Enter method")
                            refresh.isRefreshing = false
                            hideProgress()

                        }

                    }
                })

        } else {
            refresh.isRefreshing = false
            hideProgress()
            Alert.showMessage(
                this@NotificationDetailsActivity,
                getString(R.string.no_internet)
            )
        }


    }


    fun loadData() {
        var notification = AppConstants.CurrentSelectedNotification
        tvTitle.text = notification.notificationTitle
        tvSubject.text = notification.notificationContent
        tvDate.text = notification.notificationDate

    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}

package com.kadabra.courier.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.reach.plus.admin.util.UserSessionManager
import com.kadabra.Networking.INetworkCallBack
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.R
import com.kadabra.courier.api.ApiResponse
import com.kadabra.courier.api.ApiServices
import com.kadabra.courier.model.Courier
import com.kadabra.courier.task.TaskActivity
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.AppConstants
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {

    //region Members
    private var courier: Courier = Courier()

    //endregion
    //region Helper Functions
    private fun init() {
        btnLogin.setOnClickListener {

            if (validateData()) {
                var userName = etUsername.text.toString()
                var password = etPassword.text.toString()

                logIn(userName, password)
            }
        }
    }

    private fun validateData(): Boolean {
        var userName = etUsername.text.toString().trim()
        if (userName.isNullOrEmpty()) {
            Alert.showMessage(this, getString(com.kadabra.courier.R.string.error_user_name))
            return false
        }
//        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(CourierName).matches()) {
//            Alert.showMessage(this, getString(R.string.error_email))
//            return false
//        }
        if (etPassword.text.toString().trim().isNullOrEmpty()) {
            Alert.showMessage(this, getString(com.kadabra.courier.R.string.error_password))
            return false
        }
        return true
    }

    private fun logIn(usreName: String, password: String): Courier {
        showProgress()
        if (NetworkManager().isNetworkAvailable(this)) {
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.logIn(usreName, password)
            NetworkManager().request(endPoint, object : INetworkCallBack<ApiResponse<Courier>> {
                override fun onFailed(error: String) {
                    hideProgress()
                    Alert.showMessage(
                        this@LoginActivity,
                        getString(R.string.error_login_server_error)
                    )
                }

                override fun onSuccess(response: ApiResponse<Courier>) {
                    if (response.Status == AppConstants.STATUS_SUCCESS && response.ResponseObj != null) {
                        hideProgress()
                        courier = response.ResponseObj!!
                        saveUserData(courier)
                    } else if (response.Status == AppConstants.STATUS_FAILED) {
                        hideProgress()
                        Alert.showMessage(
                            this@LoginActivity,
                            getString(R.string.error_incorrect_user_name)
                        )
                    } else if (response.Status == AppConstants.STATUS_INCORRECT_DATA) {
                        hideProgress()
                        Alert.showMessage(
                            this@LoginActivity,
                            getString(R.string.error_incorrect_password)
                        )
                    }

                }
            })

        } else {
            hideProgress()
            Alert.showMessage(this@LoginActivity, getString(R.string.no_internet))
        }
        return courier
    }


    private fun saveUserData(courier: Courier) {
        AppConstants.currentLoginCourier = courier
        UserSessionManager.getInstance(this).setUserData(courier)
        UserSessionManager.getInstance(this).setIsLogined(true)
        startActivity(Intent(this, TaskActivity::class.java))
        finish()

    }

    private fun showProgress() {
        avi.smoothToShow()
    }

    private fun hideProgress() {
        avi.smoothToHide()
    }
    //endregion

    //region Events
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        init()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        finish()
    }
    //endregion


}

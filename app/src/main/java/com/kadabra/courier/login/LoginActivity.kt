package com.kadabra.courier.login

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import com.google.android.gms.location.*
import com.reach.plus.admin.util.UserSessionManager
import com.kadabra.Networking.INetworkCallBack
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.R
import com.kadabra.courier.api.ApiResponse
import com.kadabra.courier.api.ApiServices
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.model.Courier
import com.kadabra.courier.task.TaskActivity
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.AppConstants
import kotlinx.android.synthetic.main.activity_login.*
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.kadabra.courier.BuildConfig
import com.kadabra.courier.base.BaseNewActivity
import com.kadabra.courier.callback.ILocationListener
import com.kadabra.courier.utilities.AppController


class LoginActivity : BaseNewActivity(), View.OnClickListener, ILocationListener {

    //region Members
    private var courier: Courier = Courier()
    private var lastLocation: Location? = null
    private var userCustomEmail = ""
    private val TAG = this.javaClass.simpleName
    //endregion

    //region Constructor
    companion object {
        private val TAG = LoginActivity::class.java.simpleName
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
    }
    //endregion

    //region Events
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        FirebaseManager.setUpFirebase()
        init()

    }

    override fun onClick(view: View?) {

        when (view!!.id) {
            R.id.btnLogin -> {
                if (validateData()) {
                    var userName = etUsername.text.toString()
                    userCustomEmail = ""
                    userCustomEmail = "$userName@gmail.com"
                    var password = etPassword.text.toString()
//                    etUsername.isFocusable=false
                    logIn(userName, password)
//                    LocationHelper.shared.stopUpdateLocation()
                }
            }
        }
    }

    override fun locationResponse(locationResult: LocationResult) {
        lastLocation = locationResult.lastLocation
//        Toast.makeText(this, locationResult.lastLocation.toString(), Toast.LENGTH_LONG).show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
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
                return
            }
        }// other 'case' lines to check for other
        // permissions this app might request.
    }


//endregion

    //region Helper Functions
    private fun init() {
        btnLogin.setOnClickListener(this)
    }

    private fun validateData(): Boolean {
        var userName = etUsername.text.toString().trim()
        if (userName.isNullOrEmpty()) {
            Alert.showMessage(this, getString(R.string.error_user_name))
            return false
        }

        if (etPassword.text.toString().trim().isNullOrEmpty()) {
            Alert.showMessage(this, getString(R.string.error_password))
            return false
        }
        return true
    }

    private fun logIn(userName: String, password: String): Courier {
        showProgress()
        if (NetworkManager().isNetworkAvailable(this)) {

            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.logIn(userName, password)
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
                        Log.d(TAG, "logIn - API - Success.$response")

                        courier = response.ResponseObj!!
                        AppConstants.CurrentLoginCourier = courier
                        if (FirebaseManager.getCurrentUser() == null) //create new user
                        {
                            FirebaseManager.logIn(
                                userCustomEmail,
                                password
                            )
                            { user, error ->
                                if (user != null) {
                                    Log.d(TAG, "AUTH- LOGIN - SUCCESS")
                                    prepareCourierData()
                                    sendUserToken(courier.CourierId,FirebaseManager.token)
                                    FirebaseManager.updateCourier(courier) {

                                            success ->
                                        if (success) {
                                            Log.d(TAG, "FBDB - UPDATE USER - SUCCESS")
                                            saveUserData(courier)

                                            startActivity(
                                                Intent(
                                                    this@LoginActivity,
                                                    TaskActivity::class.java
                                                )
                                            )
                                            finish()
                                            hideProgress()
                                        } else {
                                            Alert.showMessage(
                                                this@LoginActivity,
                                                error!!
                                            )
                                            hideProgress()
                                        }
                                    }

                                } else //user is new
                                {
                                    FirebaseManager.createAccount(
                                        userCustomEmail,
                                        password
                                    ) { user, error ->
                                        if (user != null) {

                                            Log.d(TAG, "AUTH- SIGNUP - SUCCESS")
                                            prepareCourierData()
                                            sendUserToken(courier.CourierId,FirebaseManager.token)
                                            FirebaseManager.createCourier(courier) { success ->
                                                if (success) {
                                                    Log.d(
                                                        TAG,
                                                        "FBDB - CREATE NEW USER - SUCCESS"
                                                    )
                                                    saveUserData(courier)
                                                    startActivity(
                                                        Intent(
                                                            this@LoginActivity,
                                                            TaskActivity::class.java
                                                        )
                                                    )
                                                    finish()
                                                    hideProgress()
                                                } else {
                                                    Alert.showMessage(
                                                        this@LoginActivity,
                                                        error!!
                                                    )
                                                    hideProgress()
                                                    Log.d(
                                                        TAG,
                                                        "Failed to connect to server - Error Code 2"
                                                    )
                                                }
                                            }

                                        } else {
                                            Alert.showMessage(
                                                this@LoginActivity,
                                                error!!
                                            )
                                            hideProgress()
                                        }
                                    }
                                }

                            }


                        } else  //login
                        {
                            FirebaseManager.logIn(
                                userCustomEmail,
                                password
                            )
                            { user, error ->
                                if (user != null) {
                                    Log.d(TAG, "AUTH- LOGIN - SUCCESS")
                                    prepareCourierData()
                                    FirebaseManager.updateCourier(courier) {

                                            success ->
                                        if (success) {
                                            Log.d(TAG, "FBDB - UPDATE USER - SUCCESS")
                                            saveUserData(courier)
                                            startActivity(
                                                Intent(
                                                    this@LoginActivity,
                                                    TaskActivity::class.java
                                                )
                                            )
                                            finish()
                                            hideProgress()
                                        } else {
                                            Alert.showMessage(
                                                this@LoginActivity,
                                                error!!
                                            )
                                            hideProgress()
                                        }
                                    }

                                } else //user is null
                                {
                                    Alert.showMessage(
                                        this@LoginActivity,
                                        error!!
                                    )
                                    hideProgress()
                                }

                            }
                        }


                    } else if (response.Status == AppConstants.STATUS_FAILED) {
                        Log.d(TAG, "logIn - API - Failed.${response.Message}")
                        hideProgress()
                        Alert.showMessage(
                            this@LoginActivity,
                            getString(R.string.error_incorrect_user_name)
                        )
                    } else if (response.Status == AppConstants.STATUS_INCORRECT_DATA) {
                        Log.d(TAG, "logIn - API - Failed.${response.Message}")
                        hideProgress()
                        Alert.showMessage(
                            this@LoginActivity,
                            getString(R.string.error_incorrect_password)
                        )
                    }

                }
            })

        } else {
            Log.d(TAG, "logIn - API - NO INTERNET.")
            hideProgress()
            Alert.showMessage(this@LoginActivity, getString(R.string.no_internet))
        }


        return courier
    }

    private fun prepareCourierData() {
        courier.name = courier.CourierName
        courier.token = FirebaseManager.getCurrentUser()!!.uid //token
        courier.isActive = true
        courier.isActive = true

    }


    private fun saveUserData(courier: Courier) {
        UserSessionManager.getInstance(this@LoginActivity)
            .setFirstTime(false)
        AppConstants.CurrentLoginCourier = courier
        UserSessionManager.getInstance(this).setUserData(courier)
        UserSessionManager.getInstance(this).setIsLogined(true)

    }

    private fun showProgress() {
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

    private fun sendUserToken(id: Int, token: String) {
        if (NetworkManager().isNetworkAvailable(this)) {
            var request = NetworkManager().create(ApiServices::class.java)
            var endPoint = request.setCourierToken(id, token)
            NetworkManager().request(endPoint, object : INetworkCallBack<ApiResponse<Boolean>> {
                override fun onSuccess(response: ApiResponse<Boolean>) {
                    Log.d(TAG, "SEND TOKEN - API - SUCCESSFULLY.")
                }

                override fun onFailed(error: String) {
                    Log.d(TAG, "SEND TOKEN - API - FAILED.")
                    Alert.showMessage(
                        this@LoginActivity,
                        getString(R.string.error_login_server_error)
                    )
                }
            })


        } else {
            Log.d(TAG, "SEND TOKEN - API - NO INTERNET.")
            Alert.showMessage(this@LoginActivity, getString(R.string.no_internet))
        }

    }


    //endregion

}
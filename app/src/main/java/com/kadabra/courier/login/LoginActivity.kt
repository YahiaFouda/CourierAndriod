package com.kadabra.courier.login

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.reach.plus.admin.util.UserSessionManager
import com.kadabra.Networking.INetworkCallBack
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.R
import com.kadabra.courier.api.ApiResponse
import com.kadabra.courier.api.ApiServices
import com.kadabra.courier.firebase.FirebaseHelper
import com.kadabra.courier.model.Courier
import com.kadabra.courier.task.TaskActivity
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.AppConstants
import kotlinx.android.synthetic.main.activity_login.*
import android.location.Geocoder
import android.view.View
import android.view.WindowManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.kadabra.courier.base.BActivity
import com.kadabra.courier.model.location
import java.util.*


class LoginActivity : BActivity(), View.OnClickListener {

    //region Members
    private var courier: Courier = Courier()
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var lastLocation: Location? = null
    private var mAuth: FirebaseAuth? = null
    private var userCustomEmail = "@gmail.com"
    private var firebaseAuthListener: FirebaseAuth.AuthStateListener? = null
    private var firebaseUser: FirebaseUser? = null

    //endregion

    //region Constructor
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val TAG = "LoginActivity"
    }
    //endregion

    //region Helper Functions
    private fun init() {
        btnLogin.setOnClickListener(this)
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
                        Log.d(TAG, "logIn - API - Success.$response")
                        hideProgress()
                        courier = response.ResponseObj!!

                        if (FirebaseHelper.getCurrentUser() == null)// 1 create new courier on auth 2 login
                        {
                            Log.d(TAG, "AUTH- FB - user NOt Exist")

                            FirebaseHelper.createAccount(
                                userCustomEmail,
                                password, object : FirebaseHelper.IFbOperation {
                                    override fun onSuccess(code: Int) {
                                        Log.d(TAG, "AUTH- SIGNUP - SUCCESS")
                                        prepareCourierData()
                                        FirebaseHelper.createCourier(courier)
                                        Log.d(TAG, "FBDB - CREATE NEW USER - SUCCESS")
                                        saveUserData(courier)
                                        startActivity(
                                            Intent(
                                                this@LoginActivity,
                                                TaskActivity::class.java
                                            )
                                        )
                                        finish()
                                    }

                                    override fun onFailure(message: String) {
                                        Alert.showMessage(
                                            this@LoginActivity,
                                            message
                                        )
                                    }
                                }
                            )

                        } else { //user signed in before
                            FirebaseHelper.logIn(
                                userCustomEmail,
                                password, object : FirebaseHelper.IFbOperation {
                                    override fun onSuccess(code: Int) {
                                        Log.d(TAG, "AUTH- LOGIN - SUCCESS")
                                        prepareCourierData()
                                        FirebaseHelper.updateCourier(courier)
                                        Log.d(TAG, "FBDB - UPDATE USER - SUCCESS")
                                        saveUserData(courier)
                                        startActivity(
                                            Intent(
                                                this@LoginActivity,
                                                TaskActivity::class.java
                                            )
                                        )
                                        finish()
                                    }

                                    override fun onFailure(message: String) {
                                        Alert.showMessage(
                                            this@LoginActivity,
                                            message
                                        )
                                    }
                                }
                            )
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
        courier.token = FirebaseHelper.getCurrentUser()!!.uid //token
        courier.isActive = true
        courier.location = location(
            lastLocation!!.latitude.toString(),
            lastLocation!!.longitude.toString()
        )

        courier.isActive = true


        val gcd = Geocoder(this@LoginActivity, Locale.getDefault())
        val addresses = gcd.getFromLocation(
            lastLocation!!.latitude,
            lastLocation!!.longitude,
            1
        )
        if (addresses.size > 0 && !addresses[0].locality.trim().isNullOrEmpty()) {
            courier.city = addresses[0].locality
        } else
            courier.city = "default"
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
        window.setFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        )
    }

    private fun hideProgress() {
        avi.smoothToHide()
        window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
    }

    private fun requestPermission() {
        if (NetworkManager().isNetworkAvailable(this)) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            if (checkPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                fusedLocationClient?.lastLocation?.addOnSuccessListener(
                    this
                ) { location: Location? ->
                    // Got last known location. In some rare
                    // situations this can be null.
                    if (location == null) {
                        //no data

                    } else location.apply {
                        // Handle location object
                        Log.e("LOG", location.toString())
                        lastLocation = location
                        AppConstants.CurrentLocation = location
                    }
                }
            }
        } else {
            Alert.showMessage(this@LoginActivity, getString(R.string.no_internet))
        }
    }

    private fun checkPermission(vararg perm: String): Boolean {
        val havePermissions = perm.toList().all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }
        if (!havePermissions) {
            if (perm.toList().any {
                    ActivityCompat.shouldShowRequestPermissionRationale(this, it)
                }
            ) {

                val dialog = AlertDialog.Builder(this)
                    .setTitle(getString(R.string.Permission))
                    .setMessage(getString(R.string.error_location_permission_required))
                    .setPositiveButton(getString(R.string.ok)) { id, v ->
                        ActivityCompat.requestPermissions(
                            this, perm, LOCATION_PERMISSION_REQUEST_CODE
                        )
                    }
                    .setNegativeButton(getString(R.string.no)) { _, _ -> }
                    .create()
                dialog.show()
            } else {
                ActivityCompat.requestPermissions(this, perm, LOCATION_PERMISSION_REQUEST_CODE)
            }
            return false
        }
        return true
    }

    //endregion

    //region Events
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        requestPermission()
        FirebaseHelper.setUpFirebase()
        FirebaseHelper.checkCourierExist()
        init()


    }


    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.btnLogin -> {
                if (validateData()) {
                    var userName = etUsername.text.toString()
                    userCustomEmail = userName + userCustomEmail
                    var password = etPassword.text.toString()
                    logIn(userName, password)
                }
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        finish()
    }

    override fun onStart() {
        super.onStart()
        FirebaseHelper.onstartAuthListener()
//        FirebaseHelper.auth()!!.currentUser?.let {
//            FirebaseHelper.setCurrentUser(it)
//        }
    }

    override fun onStop() {
        super.onStop()
        FirebaseHelper.stopAuthListener()

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
//
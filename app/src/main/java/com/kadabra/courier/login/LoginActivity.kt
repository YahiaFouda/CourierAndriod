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
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.model.Courier
import com.kadabra.courier.task.TaskActivity
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.AppConstants
import kotlinx.android.synthetic.main.activity_login.*
import android.location.Geocoder
import android.net.Uri
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.kadabra.courier.BuildConfig
import com.kadabra.courier.callback.ILocationListener
import com.kadabra.courier.location.LocationHelper
import java.util.*


class LoginActivity : AppCompatActivity(), View.OnClickListener, ILocationListener {

    //region Members
        private var courier: Courier = Courier()
    private var lastLocation: Location? = null
    private var userCustomEmail = "@gmail.com"
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


//        requestPermission()
//        if (!checkPermissions())
//            requestPermissions()

        FirebaseManager.setUpFirebase()
        init()

    }

    override fun onClick(view: View?) {

        when (view!!.id) {
            R.id.btnLogin -> {
                if (validateData()) {
                    var userName = etUsername.text.toString()
                    userCustomEmail = userName + userCustomEmail
                    var password = etPassword.text.toString()
                    etUsername.isFocusable=false
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
//        FirebaseManager.onstartAuthListener()
//        if (!checkPermissions())
//            requestPermissions()

            val currentUser = FirebaseManager.auth().currentUser
//        if (currentUser != null) {
//            Toast.makeText(this, "Current - User" + currentUser.displayName, Toast.LENGTH_LONG)
//                .show()
//        }

        }

        override fun onStop() {
            super.onStop()
//        FirebaseManager.stopAuthListener()

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
//                        requestPermission()
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
//        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(CourierName).matches()) {
//            Alert.showMessage(this, getString(R.string.error_email))
//            return false
//        }
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
                        etUsername.isFocusable=true
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
//            var isGpsEnabled = LocationHelper.shared.isLocationEnabled()
//            courier.location = location(
//                LocationHelper.shared.lastLocation!!.latitude.toString(),
//                LocationHelper.shared.lastLocation!!.longitude.toString(), isGpsEnabled
//            )

            courier.isActive = true


//            val gcd = Geocoder(this@LoginActivity, Locale.getDefault())
//            val addresses = gcd.getFromLocation(
//                LocationHelper.shared.lastLocation!!.latitude,
//                LocationHelper.shared.lastLocation!!.longitude,
//                1
//            )
//            if (addresses.size > 0) {
//                if (!addresses[0].subAdminArea.isNullOrEmpty())
//                    courier.city = addresses[0].subAdminArea
//                else if (!addresses[0].adminArea.isNullOrEmpty())
//                    courier.city = addresses[0].adminArea
//            } else
//                courier.city = addresses[0].featureName
        }


        private fun saveUserData(courier: Courier) {
            UserSessionManager.getInstance(this@LoginActivity)
                .setFirstTime(false)
            AppConstants.CurrentLoginCourier = courier
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
            LocationHelper.shared.initializeLocation(this)
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
                        this@LoginActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.i(TAG, "Requesting permission")
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(
                this@LoginActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }


        //endregion

    }
package com.kadabra.courier.intro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.reach.plus.admin.util.UserSessionManager
import com.kadabra.courier.exception.CrashActivity
import com.kadabra.courier.exception.CrashHandeller
import com.kadabra.courier.login.LoginActivity
import com.kadabra.courier.task.TaskActivity
import com.kadabra.courier.utilities.AppConstants
import kotlinx.android.synthetic.main.activity_splash.*


class SplashActivity : AppCompatActivity() {


    //region Members
    private var startTime: Long = 2000

    //endregion
//region Events
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.kadabra.courier.R.layout.activity_splash)

        init()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    checkLocationPermission()
                }
                return
            }
        }
    }

    //endregion
//region Helper Functions
    private fun init() {

        CrashHandeller.deploy(this, CrashActivity::class.java)
        rippleBackground.startRippleAnimation()

        Handler().postDelayed({
            runOnUiThread {
                var user = UserSessionManager.getInstance(this).getUserData()
                if (user != null) {
                    AppConstants.currentLoginCourier = user
                    startActivity(Intent(this@SplashActivity, TaskActivity::class.java))
                    finish()

                } else {
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    finish()
                }


            }

        }, startTime)


    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) !== PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        } else {
//            callHandler()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
    //endregion


}
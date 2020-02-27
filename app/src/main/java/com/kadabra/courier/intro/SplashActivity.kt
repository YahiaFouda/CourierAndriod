package com.kadabra.courier.intro

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import com.reach.plus.admin.util.UserSessionManager
import com.kadabra.courier.exception.CrashActivity
import com.kadabra.courier.exception.CrashHandeller
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.login.LoginActivity
import com.kadabra.courier.main.MainActivity
import com.kadabra.courier.model.Task
import com.kadabra.courier.task.TaskActivity
import com.kadabra.courier.utilities.AppConstants
import kotlinx.android.synthetic.main.activity_splash.*


class SplashActivity : AppCompatActivity() {


    //region Members
    private var startTime: Long = 1500


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
//                    checkLocationPermission()
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
                var first = UserSessionManager.getInstance(this).isFirstTime()
                var courier = UserSessionManager.getInstance(
                    this
                ).getUserData()

                if(courier!=null&&courier.CourierId>0)
                    AppConstants.currentLoginCourier=courier!!

                if (!UserSessionManager.getInstance(this).isFirstTime() || UserSessionManager.getInstance(
                        this
                    ).getUserData() == null
                ) {
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    finish()

                } else {
                    startActivity(Intent(this@SplashActivity, TaskActivity::class.java))
                    finish()
                }

            }

        }, startTime)


    }



    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
    //endregion


}
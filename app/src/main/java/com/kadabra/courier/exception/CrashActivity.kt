package com.kadabra.courier.exception

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kadabra.courier.R
import com.kadabra.courier.base.BaseNewActivity
import com.kadabra.courier.intro.SplashActivity
import kotlinx.android.synthetic.main.activity_crash.*

class CrashActivity : BaseNewActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash)

        btnRestart?.setOnClickListener {
            startActivity(Intent(this@CrashActivity, SplashActivity::class.java))
            finish()
        }
    }
}

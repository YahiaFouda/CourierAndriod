package com.kadabra.courier.base

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kadabra.courier.utilities.LocalizationHelper


open class BaseNewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocalizationHelper.onAttach(base))
//        super.attachBaseContext(base)
    }



}
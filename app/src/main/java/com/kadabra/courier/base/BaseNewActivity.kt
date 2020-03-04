package com.kadabra.courier.base

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.kadabra.courier.utilities.LocaleManager

import android.content.pm.PackageManager.GET_META_DATA
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.MyContextWrapper
import com.reach.plus.admin.util.UserSessionManager
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper
import java.util.*

open class BaseNewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        resetTitles()
    }

    override fun attachBaseContext(base: Context) {
        var lang: String
        try {
            if (UserSessionManager.getInstance(this).getLanguage() != AppConstants.ARABIC) {
                UserSessionManager.getInstance(this).setLanguage(AppConstants.ENGLISH)
                lang = AppConstants.ENGLISH
            } else {
                lang = AppConstants.ARABIC
            }
        } catch (e: Exception) {
            lang = AppConstants.ENGLISH
        }

        val langContext = MyContextWrapper.wrap(base, Locale(lang))
        super.attachBaseContext(CalligraphyContextWrapper.wrap(langContext))
    }

    protected fun resetTitles() {
        try {
            val info = packageManager.getActivityInfo(componentName, GET_META_DATA)
            if (info.labelRes != 0) {
                setTitle(info.labelRes)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

    }

    fun setNewLocale(mContext: AppCompatActivity, @LocaleManager.LocaleDef language: String) {
        LocaleManager.setNewLocale(this, language)
        val intent = mContext.intent
        mContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))

    }
}
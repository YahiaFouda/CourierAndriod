package com.kadabra.courier.base

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

import com.kadabra.courier.utilities.LocaleManager

import android.content.pm.PackageManager.GET_META_DATA
import android.content.res.Resources
import android.os.Build
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.AppController
import com.kadabra.courier.utilities.MyContextWrapper
import com.reach.plus.admin.util.UserSessionManager
import java.util.*
import android.content.ComponentName
import android.app.ActivityManager



open class BaseNewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        resetTitles()
    }

    override fun attachBaseContext(base: Context) {
        var lang = ""
        try {
//            var sss=UserSessionManager.getInstance(AppController.getContext()).getLanguage()
//
//            if (UserSessionManager.getInstance(AppController.getContext()).getLanguage() != AppConstants.ARABIC) {
//                UserSessionManager.getInstance(AppController.getContext()).setLanguage(AppConstants.ENGLISH)
//                lang = AppConstants.ENGLISH
//            } else {
//                lang = AppConstants.ARABIC
//            }

            var currentLanguage =
                UserSessionManager.getInstance(AppController.getContext()).getLanguage()
            when {
                currentLanguage.isNullOrEmpty() -> {
                    var locale = getLocale(this.resources)
                    lang = locale.language

//                    UserSessionManager.getInstance(AppController.getContext()).setLanguage(AppConstants.ARABIC)
                }
                currentLanguage == AppConstants.ARABIC -> lang = AppConstants.ARABIC
                currentLanguage == AppConstants.ENGLISH -> lang = AppConstants.ENGLISH
            }

        } catch (e: Exception) {
            lang=AppConstants.ARABIC
            UserSessionManager.getInstance(AppController.getContext())
                    .setLanguage(AppConstants.ARABIC)
        }

        val langContext = MyContextWrapper.wrap(base, Locale(lang))
        super.attachBaseContext(langContext)
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

//    fun setNewLocale(mContext: AppCompatActivity, @LocaleManager.LocaleDef language: String) {
//        LocaleManager.setNewLocale(this, language)
//        val intent = mContext.intent
//        mContext.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK))
//
//    }

    fun getLocale(res: Resources): Locale {
        val config = res.configuration
        return if (Build.VERSION.SDK_INT >= 24) config.locales.get(0) else config.locale
    }

    @SuppressLint("NewApi")
    private fun isApplicationBroughtToBackground(): Boolean {
        val am = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val tasks = am.getRunningTasks(1)
        if (!tasks.isEmpty()) {
            val topActivity = tasks[0].topActivity
            if (topActivity!!.packageName != this.packageName) {
                return true
            }
        }
        return false
    }
}
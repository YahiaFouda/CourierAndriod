package com.kadabra.courier.utilities

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.preference.PreferenceManager
import com.reach.plus.admin.util.UserSessionManager
import java.util.*

object LocalizationHelper {

    fun onAttach(context: Context): Context {
//        var  lang=""
//        try {
//            lang = UserSessionManager.getInstance(AppController.getContext())
//                .getLanguage()
//        }
//        catch(ex:Exception)
//        {
//            lang=AppConstants.ARABIC
//        }
        var lang = UserSessionManager.getInstance(AppController.getContext())
            .getLanguage()

        return setLocale(context, lang)
//        return setLocale(context, Locale.getDefault().language)
    }


    fun setLocale(
        context: Context,
        language: String?
    ): Context {
        UserSessionManager.getInstance(context).setLanguage(language!!)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, language)
        } else updateResourcesLegacy(context, language)
    }


    private fun updateResources(
        context: Context,
        language: String?
    ): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val configuration =
            context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)
        return context.createConfigurationContext(configuration)
    }

    private fun updateResourcesLegacy(
        context: Context,
        language: String?
    ): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration
        configuration.locale = locale
        configuration.setLayoutDirection(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return context
    }
}
package com.reach.plus.admin.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.model.Courier
import com.kadabra.courier.model.Notification
import com.kadabra.courier.model.Task
import com.kadabra.courier.utilities.AppConstants


class UserSessionManager(val context: Context) {
    val LANGUAGE = "lang"
    private val editor: SharedPreferences.Editor
    private val sharedPreferences: SharedPreferences
    private val sharedPrefName = UserSessionManager::class.java.name + "_shared_preferences"
    val KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates"
    var acceptedList=ArrayList<Task>()

    init {
        sharedPreferences = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        editor.apply()
    }

    companion object {
        var sUserSessionManager: UserSessionManager? = null
        private val USER_OBJECT = UserSessionManager::class.java.name + "_user_object"
        private val USER_NOTIFICATION = UserSessionManager::class.java.name + "_user_notification"
        private val USER_ACCEPTEDTASKS = UserSessionManager::class.java.name + "_user_acceptedtasks"





        @Synchronized
        fun getInstance(context: Context): UserSessionManager {
            if (sUserSessionManager == null) {
                sUserSessionManager = UserSessionManager(context)
            }
            return sUserSessionManager as UserSessionManager
        }
    }

    fun isFirstTime(): Boolean {
        return sharedPreferences.getBoolean(AppConstants.IS_FIRST, true)
    }

    fun setFirstTime(isFirst: Boolean) {
        editor.putBoolean(AppConstants.IS_FIRST, isFirst).commit()
    }

    fun isLogined(): Boolean {
        return sharedPreferences.getBoolean(AppConstants.IS_LOGIN, false)
    }

    fun setIsLogined(isFirst: Boolean) {
        editor.putBoolean(AppConstants.IS_LOGIN, isFirst).commit()
    }

    fun getLanguage(): String {
        return sharedPreferences.getString(AppConstants.LANGUAGE,"")!!
    }

    fun setLanguage(language: String) {
        editor.putString(AppConstants.LANGUAGE, language).commit()
    }

    fun getToken(): String {
        return sharedPreferences.getString(AppConstants.TOKEN, "")!!
    }

    fun setToken(token: String) {
        editor.putString(AppConstants.TOKEN, token).commit()
    }
 fun requestingLocationUpdates(): Boolean {
        return sharedPreferences.getBoolean(KEY_REQUESTING_LOCATION_UPDATES, false)
    }

    fun setRequestingLocationUpdates(requestingLocationUpdates: Boolean) {
        editor.putBoolean(KEY_REQUESTING_LOCATION_UPDATES, requestingLocationUpdates).commit()
    }


    fun setUserData(courierModel: Courier?) {
        val gson = Gson()
        val json = gson.toJson(courierModel)
        editor.putString(USER_OBJECT, json)
        editor.commit()
    }

    fun getUserData(): Courier? {
        val gson = Gson()
        val json = sharedPreferences.getString(USER_OBJECT, null) ?: return null

        val type = object : TypeToken<Courier>() {
        }.type

        return gson.fromJson(json, type)
    }

    fun setNotification(notificationList: ArrayList<Notification>) {
        val gson = Gson()
        val json = gson.toJson(notificationList)
        editor.putString(USER_NOTIFICATION, json)
        editor.commit()
    }

    fun getNotification(): ArrayList<Notification> {
        val gson = Gson()
        val json = sharedPreferences.getString(USER_NOTIFICATION, null)

        val type = object : TypeToken<ArrayList<Notification>>() {
        }.type

        return gson.fromJson(json, type)
    }
    fun setTotalNotification(total: Int) {
        val gson = Gson()
        val json = gson.toJson(total)
        editor.putString(USER_NOTIFICATION, json)
        editor.commit()
    }

    fun getTotalNotification(): Int {
        val gson = Gson()
        val json = sharedPreferences.getString(USER_NOTIFICATION, "0")

        val type = object : TypeToken<Int>() {
        }.type

        return gson.fromJson(json, type)
    }

    fun setStartedTasks(taskList: ArrayList<Task>?) {
        val gson = Gson()
        val json = gson.toJson(taskList)
        editor.putString(USER_ACCEPTEDTASKS, json)
        editor.commit()
    }

    fun getStartedTasks(): ArrayList<Task>? {
        val gson = Gson()
        val json = sharedPreferences.getString(USER_ACCEPTEDTASKS, null)

        val type = object : TypeToken<ArrayList<Task>>() {
        }.type

        return gson.fromJson(json, type)
    }

    fun isAccepted(): Boolean {
        return sharedPreferences.getBoolean(AppConstants.IS_ACCEPTED, false)
    }

    fun setIsAccepted(isAccepted: Boolean) {
        editor.putBoolean(AppConstants.IS_ACCEPTED, isAccepted).commit()
    }

    fun logout() {
        var currentLang=getLanguage()
        setFirstTime(true)
        setIsLogined(false)
        setUserData(Courier())
        setRequestingLocationUpdates(false)
        FirebaseManager.updateCourierActive(AppConstants.CurrentLoginCourier.CourierId,false)
        editor.clear()
        editor.commit()
        setLanguage(currentLang)


    }


}

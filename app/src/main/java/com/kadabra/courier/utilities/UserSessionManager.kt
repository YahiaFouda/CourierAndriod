package com.reach.plus.admin.util

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kadabra.courier.model.Courier
import com.kadabra.courier.model.Task
import com.kadabra.courier.utilities.AppConstants


class UserSessionManager(val context: Context) {
    val LANGUAGE = "lang"
    private val editor: SharedPreferences.Editor
    private val sharedPreferences: SharedPreferences
    private val sharedPrefName = UserSessionManager::class.java.name + "_shared_preferences"
    val KEY_REQUESTING_LOCATION_UPDATES = "requesting_locaction_updates"

    init {
        sharedPreferences = context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
        editor.apply()
    }

    companion object {
        var sUserSessionManager: UserSessionManager? = null
        private val USER_OBJECT = UserSessionManager::class.java.name + "_user_object"
        private val USER_Task = UserSessionManager::class.java.name + "_user_task"



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
        editor.putBoolean(AppConstants.IS_FIRST, isFirst).commit()
    }

    fun getLanguage(): String {
        return sharedPreferences.getString(AppConstants.LANGUAGE, AppConstants.ENGLISH)!!
    }

    fun setLanguage(language: String) {
        editor.putString(AppConstants.LANGUAGE, language).commit()
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

    fun setAcceptedTask(task: Task?) {
        val gson = Gson()
        val json = gson.toJson(task)
        editor.putString(USER_Task, json)
        editor.commit()
    }

    fun getAcceptedTask(): Task? {
        val gson = Gson()
        val json = sharedPreferences.getString(USER_Task, null) ?: return null

        val type = object : TypeToken<Task>() {
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
        var isFirst = isFirstTime()
        editor.clear()
        editor.commit()
        setFirstTime(isFirst)
        setIsLogined(false)
    }


}

package com.kadabra.courier.utilities

import android.location.Location
import com.kadabra.courier.model.*


/**
 * Created by Mokhtar on 6/18/2019.
 */
object AppConstants {


    const val BASE_URL = "https://courier.kadabraservices.com/api/Account/"

    const val APP_DATABASE_NAME = "courier"
    // region BASaIC RESPONSE
    val STATUS = "status"
    val MESSAGE = "Message"
    val DATA = "data"
    val STATUS_SUCCESS = 1
    val STATUS_FAILED = -1
    val STATUS_INCORRECT_DATA = -3

    //endregion
    // region CODE RESPONSE
    val CODE_204 = 204
    val CODE_201 = 201
    val CODE_200 = 1
    val CODE_1 = 200
    val CODE_444 = 444
    //endregion
    //region languages
    val TOKEN = "token"
    val LANGUAGE = "lang"
    val ARABIC = "ar"
    val ENGLISH = "en"
    val ALERT = "Alert"
    val ERROR = "Error"
    val INFO = "Info"
    val WARNING = "Warning"
    val OK = "ok"
    val CANCEL = "cancel"
    val IS_FIRST = "is_first"
    val IS_LOGIN = "logIn"
    val ADMIN_CHANNEL_ID = "123456"

    //endregion
    //region TOKEN
    var token = ""
    //endregion
    //region Auth
    const val URL_LOGIN = "Login"
    const val URL_LOG_OUT = "Logout"
    const val URL_SET_USER_TOKEN = "SetCourierToken"
    const val URL_UPDATE_LANGUAGE = "UpdateCourierLanguage"
    const val URL_GET_AVALIABLE_TAKS = "GetAvailableTask"
    const val URL_GET_NOTIFICATION = "GetAllCourierNotification"
    const val URL_GET_TASKS_HOSTORY = "GetHistoryOfCourier"
    const val URL_UPDATE_TASK_COURIER_FEES = "UpdateTaskCourierFees"
    const val URL_ACCEPT_TAKS = "UpdateAcceptTask"
    const val URL_END_TAKS = "UpdateEndTask"
    const val URL_UPDATE_NOTIFICATION = "UpdateReadedNotification"
    const val URL_START_TASK = "StartTask"
    const val URL_GET_TAKS_DETAILS = "GetTaskByID"
    const val URL_GET_VERSION_CODE = "GetVersionCode"


    //endregion

    //region  app variables
    const val TEST_MODE = "testMode"
    const val DEFAULT_LANGUAGE = 2
    const val TRUE = "true"
    const val FALSE = "false"
    const val IS_ACCEPTED = "accepted"
    var endTask = false
    var CURRENT_DEVICE_TOKEN = ""

    var CurrentLoginCourier: Courier = Courier()
    var CurrentSelectedTask: Task = Task()
    var CurrentAcceptedTask: Task = Task()
    var CurrentEditedTask: Task = Task()

    var CurrentCourierLocation: location = location()
    var currentSelectedStop: Stop = Stop()
    var CurrentLocation: Location? = null
    var ALL_TASKS_DATA = ArrayList<Task>()
    var CurrentSelectedNotification = Notification()
    var COURIERSTARTTASK = false
    var CURRENTTOTALNOTIFICATIONS = 0

    //endregion

    //region Fire base
    const val FIREBASE_TOKEN = "token"
    const val FIREBASE_NAME = "name"
    const val FIREBASE_CITY = "city"
    const val FIREBASE_LOCATION = "location"
    const val FIREBASE_LOCATION_LAT = "lat"
    const val FIREBASE_LOCATION_LONG = "long"
    const val FIREBASE_IS_ACTIVE = "active"
    const val FIREBASE_HAVE_TASK = "haveTask"
    const val FIREBASE_START_TASK = "startTask"

    var IN_PROGRESS = "In progress"
    var NEW = "New"
    var COMPLETED = "Completed"
    var WAITING = "Waiting For Pickup"
    var CANCELLED = "Cancelled"
    //endregion

    val ERROR_DIALOG_REQUEST = 9001
    val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 9002
    val PERMISSIONS_REQUEST_ENABLE_GPS = 9003
    var FIRE_BASE_LOGOUT = false
    var FIRE_BASE_NEW_TASK = false
//    var FIRE_BASE_EDIT_TASK = false
    var FIRE_BASE_DELETE_TASK = false
    var FIRE_BASE_CANCEL_TASK = false
    var FIRE_BASE_REASSIGN_TASK = false
    var isCountDownTimerIsFinished = false


}
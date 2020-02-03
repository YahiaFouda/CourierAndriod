package com.kadabra.courier.utilities

import com.kadabra.courier.model.Courier
import com.kadabra.courier.model.Stop
import com.kadabra.courier.model.Task


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
    val IS_LOGIN = "login"



    //endregion
    //region TOKEN
    var token = ""
    //endregion
    //region Auth

    const val URL_LOGIN = "Login"
    const val URL_LOG_OUT = "Logout"
    const val URL_GET_AVALIABLE_TAKS = "GetAvailableTask"
    const val URL_END_TAKS = "UpdateEndTask"
    const val URL_GET_TAKS_DETAILS = "GetTaskByID"


    //endregion

    //region  app variables
    const val TEST_MODE = "testMode"
    const val DEFAULT_LANGUAGE = 2
    const val TRUE = "true"
    const val FALSE = "false"
    const val IS_ACCEPTED = "accepted"
    var endTask=false

    var currentLoginCourier: Courier = Courier()
    var CurrentSelectedTask: Task = Task()
    var currentSelectedStop: Stop = Stop()
    //endregion


}
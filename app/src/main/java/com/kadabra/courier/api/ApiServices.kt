package com.kadabra.courier.api


import com.kadabra.courier.googleDirection.Directions
import com.kadabra.courier.model.*
import com.kadabra.courier.utilities.AppConstants
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


/**
 * Created by Mokhtar on 1/5/2020.
 */
interface ApiServices {

    @GET(AppConstants.URL_LOGIN)
    fun logIn(
        @Query("userName") userName: String,
        @Query("password") password: String,
        @Query("deviceType") deviceType: Int
    )
            : Call<ApiResponse<Courier>>

    @POST(AppConstants.URL_UPDATE_LANGUAGE)
    fun updateCourierLanguage(@Query("courierId") courierId: Int, @Query("Lang") lang: String)
            : Call<ApiResponse<Boolean?>>


    @POST(AppConstants.URL_LOG_OUT)
    fun logOut(@Query("ID") id: Int)
            : Call<ApiResponse<Courier>>

    @POST(AppConstants.URL_SET_USER_TOKEN)
    fun setCourierToken(@Query("CourierId") CourierId: Int, @Query("Token") token: String)
            : Call<ApiResponse<Boolean>>


    @GET(AppConstants.URL_GET_AVALIABLE_TAKS)
    fun getAvaliableTasks(@Query("CourierID") courierId: Int)
            : Call<ApiResponse<TaskData>>

    @GET(AppConstants.URL_GET_NOTIFICATION)
    fun getAllCourierNotification(@Query("courierId") courierId: Int)
            : Call<ApiResponse<NotificationData>>

    @GET(AppConstants.URL_GET_TASKS_HOSTORY)
    fun getAllCourierTasksHistory(@Query("courierId") courierId: Int)
            : Call<ApiResponse<ArrayList<Task>>>

    @POST(AppConstants.URL_ACCEPT_TAKS)
    fun acceptTask(@Query("taskId") taskID: String)
            : Call<ApiResponse<Task>>


    @POST(AppConstants.URL_UPDATE_NOTIFICATION)
    fun updateReadNotification(@Query("notificationId") notificationId: String)
            : Call<ApiResponse<Boolean>>

    @POST(AppConstants.URL_END_TAKS)
    fun endTask(@Query("taskId") taskID: String)
            : Call<ApiResponse<Task>>

    @POST(AppConstants.URL_END_TAKS)
    fun endTask(
        @Query("taskId") taskID: String,
        @Query("paymentmethod") paymentType: Int,
        @Query("treasury") amount: Double
    )
            : Call<ApiResponse<Task>>

    @GET(AppConstants.URL_GET_RECEIPT_DATA)
    fun displayReceiptData(@Query("taskId") taskID: String)
            : Call<ApiResponse<ReceiptData>>


    @GET(AppConstants.URL_GET_TAKS_DETAILS)
    fun getTaskDetails(@Query("taskId") taskID: String)
            : Call<ApiResponse<Task>>

    @GET(AppConstants.URL_GET_VERSION_CODE)
    fun forceUpdate(): Call<ApiResponse<String>>

    @POST(AppConstants.URL_UPDATE_TASK_COURIER_FEES)
    fun updateTaskCourierFees(
        @Query("taskId") taskId: String, @Query("Kilometers") kilometers: Float
    )
            : Call<ApiResponse<Task>>

    @POST(AppConstants.URL_START_TASK)
    fun startTask(
        @Query("taskId") taskId: String, @Query("Kilometeres") kilometers: Float
    )
            : Call<ApiResponse<Task>>


    @GET("maps/api/directions/json")
    fun getFullJson(
        @Query("origin") origin: String, @Query("destination") destination: String,
        @Query("waypoints") waypoints: String?, @Query("key") key: String

    ): Call<Directions?>


    @GET(AppConstants.URL_GET_COURIER_TRESAURY)
    fun getCourierTreasury(@Query("courierId") courierId: Int)
            : Call<ApiResponse<Double?>>

}


package com.kadabra.courier.api


import com.kadabra.courier.model.Courier
import com.kadabra.courier.model.Task
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
    fun logIn(@Query("userName") userName: String, @Query("password") password: String)
            : Call<ApiResponse<Courier>>


    @POST(AppConstants.URL_LOG_OUT)
    fun logOut(@Query("ID") id: Int)
            : Call<ApiResponse<Courier>>

    @POST(AppConstants.URL_SET_USER_TOKEN)
    fun setCourierToken(@Query("CourierId") CourierId:Int,@Query("Token") token:String)
            : Call<ApiResponse<Boolean>>


    @GET(AppConstants.URL_GET_AVALIABLE_TAKS)
    fun getAvaliableTasks(@Query("CourierID") courierId: Int)
            : Call<ApiResponse<ArrayList<Task>>>

    @POST(AppConstants.URL_END_TAKS)
    fun acceptTask(@Query("taskId") taskID: String)
            : Call<ApiResponse<Task>>


    @POST(AppConstants.URL_END_TAKS)
    fun endTask(@Query("taskId") taskID: String)
            : Call<ApiResponse<Task>>


    @GET(AppConstants.URL_GET_TAKS_DETAILS)
    fun getTaskDetails(@Query("taskId") taskID: String)
            : Call<ApiResponse<Task>>

    @GET(AppConstants.URL_GET_VERSION_CODE)
    fun forceUpdate(): Call<ApiResponse<String>>


}


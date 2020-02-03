package com.kadabra.Networking


import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.kadabra.courier.R.string
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.AppController
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException


class NetworkManager {

    var context = AppController.getContext()

    // create request
    fun <T> create(service: Class<T>): T {
        // using interceptor for adding custom header
        //region for chain
//        var okHttpBuilder = OkHttpClient.Builder()
//        val interceptor = Interceptor { chain ->
//            val request = chain?.request()?.newBuilder()
//                    ?.addHeader(AppConstants.token,"")?.build()
//            chain?.proceed(request)
//        }
//        okHttpBuilder.networkInterceptors().add(interceptor)
//
//        var builder = Retrofit.Builder()
//                .baseUrl(AppConstants.BASE_URL!!)
//                .addConverterFactory(GsonConverterFactory.create())
//                .client(okHttpBuilder.build())
//        var retrofit = builder.build()
        //endregion

//        var okHttpClient = OkHttpClient.Builder()
//            .connectTimeout(2, TimeUnit.MINUTES)
//            .readTimeout(2, TimeUnit.MINUTES)
//            .writeTimeout(2, TimeUnit.MINUTES)
//            //.sslSocketFactory(sslSocketFactory, trustManager)
//            .followRedirects(false)
//            .followSslRedirects(false)
//            .retryOnConnectionFailure(false)
//            .cache(null)//new Cache(sContext.getCacheDir(),10*1024*1024)
//            .build()

//this lines is to print the response and request in the console
        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor).build()
//        val client = OkHttpClient.Builder()
//            .addInterceptor(ResponseInterceptor())
//            .build()
/////
        var builder = Retrofit.Builder()
            .baseUrl(AppConstants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
        var retrofit = builder.build()


        return retrofit.create(service)

    }

    // request functionality
    fun <U> request(endPoint: Call<U>, callback: INetworkCallBack<U>) {
        var s = endPoint.request().url
        endPoint.enqueue(object : Callback<U> {
            override fun onResponse(call: Call<U>?, response: retrofit2.Response<U>?) {
                print(response?.body().toString())
                Log.d("Mokhtar", response?.body().toString())
                if (response!!.isSuccessful) {
                    callback.onSuccess(response.body()!!)
                } else {
                    when (response.code()) {
                        404 -> {
                            // invalid DATA
                            callback.onFailed(context.getString(string.no_results))
                        }
                        500 -> {
                            // SERVER IS BROKEN
                            callback.onFailed(context.getString(string.error_login_server_error))
                        }
                        else -> {
                            // UNKNOWN ERROR
                            callback.onFailed(context.getString(string.error_login_server_unknown_error))
                        }
                    }
                }
            }

            override fun onFailure(call: Call<U>?, t: Throwable?) = if (t is IOException) {

                callback.onFailed(context!!.getString(string.error_login_server_error))

            } else {
                callback.onFailed(context!!.getString(string.error_login_server_error))
            }
        })

    }

    // check internet connectivity
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    class ResponseInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            val modified = response.newBuilder()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .build()

            return modified
        }
    }

}

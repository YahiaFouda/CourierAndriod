package com.kadabra.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kadabra.courier.R
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.intro.SplashActivity
import com.kadabra.courier.login.LoginActivity
import com.kadabra.courier.task.TaskActivity
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.AppController
import com.reach.plus.admin.util.UserSessionManager
import java.util.Objects

class FirebaseMessages : FirebaseMessagingService() {


    private var notificationManager: NotificationManager? = null

    private var isBackground: Boolean = false


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        try {
            if (remoteMessage.notification != null) {
                isBackground = false
                Log.i(
                    "onMessageReceived",
                    remoteMessage.notification!!.imageUrl.toString() + "---" + remoteMessage.notification!!.body
                )
            } else if (remoteMessage.data != null) {
                isBackground = true
                Log.i("onMessageReceived", "getData " + remoteMessage.data)
            }
            val map = remoteMessage.data
            val notification = remoteMessage.notification
            var title: String? = ""
            var body: String? = ""
            var titleData = ""
            var bodyData = ""
            if (notification != null) {
                if (notification.title != null && !notification.title!!.isEmpty())
                    title = notification.title
                if (notification.body != null && !notification.body!!.isEmpty())
                    body = notification.body
            } else if (map != null && map.size == 2) {
                if (map.containsKey("Title") || map.containsKey("title"))
                    titleData = (getElementByIndex(map, 1) as String?).toString()
                if (map.containsKey("Body") || map.containsKey("body"))
                    bodyData = (getElementByIndex(map, 0) as String?).toString()
            }
            Log.i("onMessageReceived", "getData " + remoteMessage.data)
            when (body) {
                "LogOut" -> {
                    Log.d("Notification Logout", "test")
//                    sendNotification(
//                        title!!,
//                        applicationContext.resources.getString(R.string.login_another_device)
//                    )
                    FirebaseManager.logOut()
                    UserSessionManager.getInstance(this).setUserData(null)
                    UserSessionManager.getInstance(this).setIsLogined(false)
                    startActivity(Intent(this, LoginActivity::class.java)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setAction(Intent.ACTION_MAIN)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK ))
                }
                "kadabra" -> {
                    Log.d("Notification New Task", "New Task Added")
                    isBackground=true
                    sendNotification(titleData, bodyData)
                }
                else -> sendNotification(titleData, bodyData)
            }
        } catch (ignored: Exception) {
               Log.e("notificationException", ignored.message)
        }

    }


    private fun sendNotification(title: String, body: String?) {
        val intent =
            Intent(this, if (isBackground) SplashActivity::class.java else TaskActivity::class.java)
        intent.putExtra("notificationTitle", title)
        intent.putExtra("notificationBody", body)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent =
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val sound =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.notification)
        val notificationBuilder = NotificationCompat.Builder(this, AppConstants.ADMIN_CHANNEL_ID)
            .setSmallIcon(R.drawable.placeholder)
            .setContentTitle(title)
            .setContentText(if (body != null && !body.isEmpty()) body else "")
            .setAutoCancel(true)
            .setSound(sound)
            .setContentIntent(pendingIntent)
            .setPriority(Notification.PRIORITY_HIGH)
            .setWhen(System.currentTimeMillis())
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupChannels(title, body)
        }

        assert(notificationManager != null)
        notificationManager!!.notify(0, notificationBuilder.build())


    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun setupChannels(adminChannelName: CharSequence, adminChannelDescription: String?) {
        val adminChannel: NotificationChannel = NotificationChannel(
            AppConstants.ADMIN_CHANNEL_ID,
            adminChannelName,
            NotificationManager.IMPORTANCE_HIGH
        )
        val sound =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.notification)
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .build()
        adminChannel.description = adminChannelDescription
        adminChannel.enableLights(true)
        adminChannel.setSound(sound, attributes)
        adminChannel.lightColor = Color.WHITE
        adminChannel.enableVibration(true)
        if (notificationManager != null) {
            notificationManager!!.createNotificationChannel(adminChannel)
        }
    }

    fun getElementByIndex(map: Map<*, *>, index: Int): Any {
        return map[Objects.requireNonNull(map.keys.toTypedArray())[index]]!!
    }

}

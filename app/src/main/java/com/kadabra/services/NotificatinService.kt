package com.kadabra.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kadabra.courier.R
import com.kadabra.courier.task.TaskActivity
import com.kadabra.courier.utilities.AppConstants
import android.annotation.SuppressLint
import android.app.Notification
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.login.LoginActivity
import com.kadabra.courier.task.NotificationActivity
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.AppController
import com.reach.plus.admin.util.UserSessionManager
import java.util.*


class NotificatinService : FirebaseMessagingService() {

    var TAG = "NotificatinService"
    override fun onNewToken(s: String) {
        super.onNewToken(s)
        sendToken(s)
    }

    fun sendToken(token: String) {

//        val data = hashMapOf("token" to token)
//        FirebaseFirestore.getInstance()
//            .document("users/" + FirebaseAuth.getInstance().currentUser?.uid).set(data)
//            .addOnSuccessListener {
//            }.addOnFailureListener {
//                Log.w(ContentValues.TAG, "Error sending token", it)
//            }
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val map = remoteMessage.data
        var title = ""
        var message = ""

        if (map != null) {
            title = map["title"].toString()
            message = map["body"].toString()
        }

        Log.d(
            TAG, "onMessageReceived: Message Received: \n" +
                    "Title: " + title + "\n" +
                    "Message: " + message
        )
        if (message == "LogOut") {
            AppConstants.FIRE_BASE_LOGOUT=true
            FirebaseManager.logOut()
            UserSessionManager.getInstance(this).setUserData(null)
            UserSessionManager.getInstance(this).setIsLogined(false)
            UserSessionManager.getInstance(this).setFirstTime(false)
            LocationUpdatesService.shared.removeLocationUpdates()
            startActivity(
                Intent(
                    AppController.getContext(),
                    LoginActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        } else if (title == "Kadabra") {
            AppConstants.FIRE_BASE_NEW_TASK=true
            var intent= Intent(
                AppController.getContext(),
                TaskActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER )
            startActivity(intent
            )
            sendNotification(title!!, message, intent)
        } else {
            val intent = Intent(this, TaskActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            sendNotification(title!!, message, intent)

        }


    }

    private fun sendNotification(title: String?, message: String?, intent: Intent) {
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val NOTIFICATION_CHANNEL_ID = "123456"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                AppConstants.ADMIN_CHANNEL_ID,
                AppConstants.ADMIN_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            // Configure the notification channel.
            notificationChannel.description = "Channel description"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED

            notificationChannel.vibrationPattern = longArrayOf(
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500
            )
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // assuming your main activity
        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
//        val intent = Intent(this, TaskActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        notificationBuilder.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setOngoing(true)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("COURIER")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentTitle(title)
            .setContentText(message)
            .setFullScreenIntent(pendingIntent, true)
            .setContentInfo("Info")
            .setUsesChronometer(false)


        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }


    private fun newTaskNotification(title: String, message: String, intent: Intent) {
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val NOTIFICATION_CHANNEL_ID = "123456"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                AppConstants.ADMIN_CHANNEL_ID,
                AppConstants.ADMIN_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            // Configure the notification channel.
            notificationChannel.description = "Channel description"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED

            notificationChannel.vibrationPattern = longArrayOf(
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500,
                500
            )
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // assuming your main activity
        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
//        val intent = Intent(this, TaskActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        notificationBuilder.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setOngoing(true)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("COURIER")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentTitle(title)
            .setContentText(message)
            .setFullScreenIntent(pendingIntent, true)
            .setContentInfo("Info")
            .setUsesChronometer(false)


        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())

    }

    fun getElementByIndex(map: Map<*, *>, index: Int): Any {
        return map[Objects.requireNonNull(map.keys.toTypedArray())[index]]!!
    }

    companion object {

        private val TAG = NotificatinService::class.java.name
    }
}

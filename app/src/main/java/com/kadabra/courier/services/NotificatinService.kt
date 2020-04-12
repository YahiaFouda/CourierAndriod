package com.kadabra.courier.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kadabra.courier.R
import com.kadabra.courier.task.TaskActivity
import com.kadabra.courier.utilities.AppConstants
import android.app.Notification
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.login.LoginActivity
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



    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        var courier = UserSessionManager.getInstance(
                this
        ).getUserData()

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
            FirebaseManager.updateCourierActive(AppConstants.CurrentLoginCourier.CourierId,false)

            UserSessionManager.getInstance(AppController.getContext()).logout()
            LocationUpdatesService.shared.removeLocationUpdates()
            startActivity(
                Intent(
                    AppController.getContext(),
                    LoginActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        }
        else if (title == "NewTask" ) {
            AppConstants.FIRE_BASE_NEW_TASK=true

            Log.d(
                    TAG, "$title: \n" +
                    AppConstants.FIRE_BASE_NEW_TASK + "\n" +
                    "Courier: " + courier?.CourierId
            )
            if(courier!=null&&courier.CourierId>0)
            {
                var intent= Intent(
                        AppController.getContext(),
                        TaskActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER )
                startActivity(intent
                )
                sendNotification(title!!, message, intent)

                Log.d(TAG, "Sended")
            }
            else // user didn't log in yet
            {
                Log.d(TAG, "Not Login yet")
                var intent= Intent(
                        AppController.getContext(),
                        LoginActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER )
                startActivity(intent
                )
                sendNotification(title!!, message, intent)
            }


        }
        else if (title == "EditTask" ) {
            AppConstants.FIRE_BASE_EDIT_TASK=true

            Log.d(
                    TAG, "$title: \n" +
                    AppConstants.FIRE_BASE_EDIT_TASK + "\n" +
                    "Courier: " + courier?.CourierId
            )
            if(courier!=null&&courier.CourierId>0)
            {
                var intent= Intent(
                        AppController.getContext(),
                        TaskActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER )
                startActivity(intent
                )
                sendNotification(title!!, message, intent)

                Log.d(TAG, "Sended")
            }
            else // user didn't log in yet
            {
                Log.d(TAG, "Not Login yet")
                var intent= Intent(
                        AppController.getContext(),
                        LoginActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER )
                startActivity(intent
                )
                sendNotification(title!!, message, intent)
            }


        }


        else if (title == "deleteTask") {
            AppConstants.FIRE_BASE_DELETE_TASK=true

            Log.d(
                    TAG, "$title: \n" +
                    AppConstants.FIRE_BASE_DELETE_TASK + "\n" +
                    "Courier: " + courier?.CourierId
            )
            if(courier!=null&&courier.CourierId>0)
            {
                var intent= Intent(
                        AppController.getContext(),
                        TaskActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER )
                startActivity(intent
                )
                sendNotification(title!!, message, intent)

                Log.d(TAG, "Sended")
            }
            else // user didn't log in yet
            {
                Log.d(TAG, "Not Login yet")
                var intent= Intent(
                        AppController.getContext(),
                        LoginActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        .setAction(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER )
                startActivity(intent
                )
                sendNotification(title!!, message, intent)
            }


        }
        else if (title == "ReassignTask") {
            AppConstants.FIRE_BASE_REASSIGN_TASK=true

            Log.d(
                TAG, "$title: \n" +
                        AppConstants.FIRE_BASE_REASSIGN_TASK + "\n" +
                        "Courier: " + courier?.CourierId
            )
            if(courier!=null&&courier.CourierId>0)
            {
                var intent= Intent(
                    AppController.getContext(),
                    TaskActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .setAction(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER )
                startActivity(intent
                )
                sendNotification(title!!, message, intent)

                Log.d(TAG, "Sended")
            }
            else // user didn't log in yet
            {
                Log.d(TAG, "Not Login yet")
                var intent= Intent(
                    AppController.getContext(),
                    LoginActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    .setAction(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER )
                startActivity(intent
                )
                sendNotification(title!!, message, intent)
            }


        }

        else {
            Log.d(TAG, "DEFAULT")
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

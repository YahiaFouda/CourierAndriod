package com.kadabra.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kadabra.courier.R
import com.kadabra.courier.task.TaskActivity
import com.kadabra.courier.utilities.AppConstants
import android.annotation.SuppressLint
import android.app.Notification
import android.media.AudioManager
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.login.LoginActivity
import com.kadabra.courier.task.NotificationActivity
import com.reach.plus.admin.util.UserSessionManager


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
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            val i = Intent(this, TaskActivity::class.java).putExtra(
                "request",
                remoteMessage.toIntent().extras!!.get("gcm.notification.requestId").toString()
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
//            sendNotification(
//                    remoteMessage.notification!!.title,
//                    remoteMessage.notification!!.body, i
//            )
            val title = remoteMessage.notification!!.title
            val message = remoteMessage.notification!!.body!!
            Log.d(
                TAG, "onMessageReceived: Message Received: \n" +
                        "Title: " + title + "\n" +
                        "Message: " + message
            )
//
            if (message == "LogOut") {
                FirebaseManager.logOut()
                UserSessionManager.getInstance(this).setUserData(null)
                UserSessionManager.getInstance(this).setIsLogined(false)
                startActivity(
                    Intent(this, LoginActivity::class.java)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setAction(Intent.ACTION_MAIN)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                )
            }
            else if (message == "Kadabra") {
//                Log.d("Notification New Task", "New Task Added")
//                sendNotification(title, message,i)

                val intent = Intent(this, NotificationActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

                startActivity(intent)
            }
            else
            {
                sendFullSCreenNotification(
                    title!!,
                    message
                )
            }

        }


    }

    private fun sendNotification(title: String?, messageBody: String?, intent: Intent) {
//        val intent = Intent(this, TaskActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val defaultSoundUri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val sound =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + com.kadabra.courier.R.raw.notification)
        val channelId = AppConstants.ADMIN_CHANNEL_ID
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.kadabra.courier.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(sound)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                AppConstants.ADMIN_CHANNEL_ID,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val sound =
                Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + com.kadabra.courier.R.raw.notification)
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            channel.description = messageBody
            channel.enableLights(true)
            channel.setSound(sound, attributes)
            channel.lightColor = Color.WHITE
            channel.enableVibration(true)
            if (notificationManager != null) {
                notificationManager!!.createNotificationChannel(channel)
            }
        }

        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun sendNotification(title: String?, messageBody: String?) {
        val intent = Intent(this, TaskActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT
        )


        val sound =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + com.kadabra.courier.R.raw.notification)
        val notificationBuilder = NotificationCompat.Builder(this)
            .setSmallIcon(com.kadabra.courier.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(sound)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    @SuppressLint("WrongConstant")
    private fun sendFullSCreenNotification(title: String, messageBody: String) {
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val NOTIFICATION_CHANNEL_ID = "123456"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "My Notifications",
                NotificationManager.IMPORTANCE_MAX
            )

            // Configure the notification channel.
            notificationChannel.description = "Channel description"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // assuming your main activity
        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val intent = Intent(this, NotificationActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        notificationBuilder.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setOngoing(true)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Hearty365")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setFullScreenIntent(pendingIntent, true)
            .setContentInfo("Info")

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    @SuppressLint("WrongConstant")
    private fun notifyMe() {
        val notificationManager =
            this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val NOTIFICATION_CHANNEL_ID = "123456"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "My Notifications",
                NotificationManager.IMPORTANCE_MAX
            )

            // Configure the notification channel.
            notificationChannel.description = "Channel description"
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.vibrationPattern = longArrayOf(0, 1000, 500, 1000)
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // assuming your main activity
        val notificationBuilder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
        val intent = Intent(this, TaskActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        notificationBuilder.setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_ALL)
            .setOngoing(true)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Hearty365")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentTitle("Default notification")
            .setContentText("Lorem ipsum dolor sit amet, consectetur adipiscing elit.")
            .setFullScreenIntent(pendingIntent, true)
            .setContentInfo("Info")


        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())

    }

    @SuppressLint("WrongConstant")
    private fun newsd() {
        val notificationBuilder =
            NotificationCompat.Builder(this, "123456")
        val intent = Intent(this, NotificationActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(this, 0, intent, 0)
        notificationBuilder
            .setContentIntent(pendingIntent)
            .setContentText("MEDSKJHVHGJHB<MNB<")
            .setUsesChronometer(false)
            .setContentTitle("HI")
            .setCategory(Notification.CATEGORY_CALL)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                AudioManager.STREAM_RING
            )
            .setVibrate(
                longArrayOf(
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
            )
            .setTicker("Hearty365")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)

    }

    companion object {

        private val TAG = NotificatinService::class.java.name
    }
}

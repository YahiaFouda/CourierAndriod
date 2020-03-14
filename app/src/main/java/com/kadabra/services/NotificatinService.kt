package com.kadabra.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
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
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.login.LoginActivity
import com.kadabra.courier.task.TaskActivity
import com.reach.plus.admin.util.UserSessionManager


class NotificatinService : FirebaseMessagingService() {

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        sendToken(s)
    }

    fun sendToken(token: String) {

        val data = hashMapOf("token" to token)
        FirebaseFirestore.getInstance()
            .document("users/" + FirebaseAuth.getInstance().currentUser?.uid).set(data)
            .addOnSuccessListener {
            }.addOnFailureListener {
            Log.w(ContentValues.TAG, "Error sending token", it)
        }
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
            )
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

            if(title.equals("log out"))
            {
                FirebaseManager.logOut()
                UserSessionManager.getInstance(this).setUserData(null)
                UserSessionManager.getInstance(this).setIsLogined(false)
                startActivity(Intent(this, LoginActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
            else if(title.equals("kadabra"))
            {
                startActivity(Intent(this, TaskActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
            }
            else
            {
                sendNotification(
                    title,
                    message
                )
            }


        }

    }

    private fun sendNotification(title: String?, messageBody: String?, intent: Intent) {
        val intent = Intent(this, TaskActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val defaultSoundUri =
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val sound =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.notification)
        val channelId = "Default"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
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
                "Default channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
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

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val sound =
            Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + applicationContext.packageName + "/" + R.raw.notification)
        val notificationBuilder = NotificationCompat.Builder(this)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(sound)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }

    companion object {

        private val TAG = NotificatinService::class.java.name
    }
}

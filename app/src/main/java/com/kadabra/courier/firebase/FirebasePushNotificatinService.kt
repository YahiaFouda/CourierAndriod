package com.kadabra.courier.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

import com.kadabra.courier.R
import com.kadabra.courier.task.TaskActivity


class FirebasePushNotificatinService : FirebaseMessagingService() {

    override fun onNewToken(s: String) {
        super.onNewToken(s)
        sendToken(s)
    }

    fun sendToken(token: String) {
        val data = hashMapOf("token" to token)
        FirebaseFirestore.getInstance().document("users/" + FirebaseAuth.getInstance().currentUser?.uid).set(data).addOnSuccessListener {
        }.addOnFailureListener {
            Log.w(ContentValues.TAG, "Error sending token", it)
        }
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.size > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            val i = Intent(this, TaskActivity::class.java).putExtra("request", remoteMessage.toIntent().extras!!.get("gcm.notification.requestId").toString())
            sendNotification(
                    remoteMessage.notification!!.title,
                    remoteMessage.notification!!.body, i
            )

        }

    }

    private fun sendNotification(title: String?, messageBody: String?, intent: Intent) {
        val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT
        )

        val defaultSoundUri =
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channelId = "Default"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(messageBody)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
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

    companion object {

        private val TAG = FirebasePushNotificatinService::class.java.name
    }
}

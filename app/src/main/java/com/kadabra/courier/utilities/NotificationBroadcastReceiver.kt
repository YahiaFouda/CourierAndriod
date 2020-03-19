package com.kadabra.courier.utilities

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

import com.kadabra.courier.task.NotificationActivity


class NotificationBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, p1: Intent?) {
        Log.d("BroadCastData", "BroadCastData")
        val newAct = Intent(context, NotificationActivity::class.java)
        newAct.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context?.startActivity(newAct)


    }
}
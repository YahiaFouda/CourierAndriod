package com.kadabra.courier.services

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.kadabra.courier.firebase.FirebaseManager
import com.kadabra.courier.utilities.AppConstants

class ExitAppService : Service() {
    override fun onBind(intent: Intent): IBinder? {

        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d("ClearFromRecentService", "Service Started")
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ClearFromRecentService", "Service Destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        Log.e("ClearFromRecentService", "END")
        //Code here
        FirebaseManager.updateCourierActive(AppConstants.CurrentLoginCourier.CourierId,false);
        stopSelf()
    }
}
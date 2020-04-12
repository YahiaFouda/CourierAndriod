package com.kadabra.courier.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


class NotificationData {
    var courierNotificationModels: ArrayList<Notification>? = null
    var NoOfUnreadedNotifications:Int=0

}

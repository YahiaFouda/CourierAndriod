package com.kadabra.courier.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName


class Notification {
    @SerializedName("NotificationId")
    @Expose
    var notificationId = ""
    @SerializedName("NotificationContent")
    @Expose
     var notificationContent = ""
    @SerializedName("isReaded")
    @Expose
     var isReaded = false
    @SerializedName("NotificationDate")
    @Expose
     var notificationDate = ""
    @SerializedName("NotificationTitle")
    @Expose
     var notificationTitle = ""


}

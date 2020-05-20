package com.kadabra.courier.model

import android.app.PendingIntent
import android.content.Context


class NotificationServiceData {
    private var mContext: Context? = null
    var title = ""
    var message = ""
    var taskId = ""

    constructor() {

    }

    constructor(context: Context) {
        mContext = context
    }

    constructor(title: String, message: String, taskId: String) {
        this.title = title
        this.message = message
        this.taskId = taskId
    }


}
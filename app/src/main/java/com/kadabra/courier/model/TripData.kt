package com.kadabra.courier.model

import com.kadabra.courier.R
import com.kadabra.courier.utilities.AppController
import kotlinx.android.synthetic.main.activity_crash.view.*

class TripData {
    var days = 0L
    var hours = 0L
    var minutes = 0L
    var seconds = 0L
    var distance = 0.0

    constructor()
    constructor(days: Long, hours: Long, minutes: Long, seconds: Long, distance: Double) {
        this.days = days
        this.hours = hours
        this.minutes = minutes
        this.seconds = seconds
        this.distance = distance
    }

    override fun toString(): String {
        var data = ""
        if (days > 0)
            data = days.toString() +AppController.getContext().resources.getString(R.string.day) +" "
        if (hours > 0)
            data += " $hours "+AppController.getContext().resources.getString(R.string.hour)+" "
        if (minutes > 0)
                data += " $minutes "+AppController.getContext().resources.getString(R.string.minutes)+"."
        return data
    }
}
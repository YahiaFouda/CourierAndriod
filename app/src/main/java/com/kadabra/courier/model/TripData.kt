package com.kadabra.courier.model

class TripData {
    var days = 0L
    var hours = 0L
    var minutes = 0L
    var seconds = 0L
    var distance = 0.0

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
            data = days.toString() + "Day "
        if (hours > 0)
            data += "$hours hr "
        if (minutes > 0)
            data += "$minutes mins"
        return data
    }
}
package com.kadabra.courier.model

import com.google.firebase.database.Exclude

class Task {
    @Exclude @set:Exclude @get:Exclude
    var TaskId: String = ""
    @Exclude @set:Exclude @get:Exclude
    var TicketId: String = ""
    @Exclude @set:Exclude @get:Exclude
    var Task: String = ""
    @Exclude @set:Exclude @get:Exclude
    var TaskDescription: String = ""
    var courierId: Int ? = null
    @Exclude @set:Exclude @get:Exclude
    var CourierName: String = ""
    @Exclude @set:Exclude @get:Exclude
    var Amount = 0.0
    @Exclude @set:Exclude @get:Exclude
    var title: String? = null
    @Exclude @set:Exclude @get:Exclude
    var stopsmodel = ArrayList<Stop>()
    @Exclude @set:Exclude @get:Exclude
    var stopPickUp = Stop()
    @Exclude @set:Exclude @get:Exclude
    var stopDropOff = Stop()
    @Exclude @set:Exclude @get:Exclude
    var defaultStops = ArrayList<Stop>()
    @Exclude @set:Exclude @get:Exclude
    var AddedBy: String = ""
    @Exclude @set:Exclude @get:Exclude
    lateinit var location: location
    var isActive: Boolean = false


    constructor() {}

    constructor(
        userName: String,
        title: String,
        description: String,
        stopList: ArrayList<Stop>
    ) {
        this.CourierName = userName
        this.title = title
        this.TaskDescription = description
        this.stopsmodel = stopList
    }

}

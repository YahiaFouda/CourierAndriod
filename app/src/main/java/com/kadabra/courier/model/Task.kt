package com.kadabra.courier.model

class Task {
    var TaskId: String = ""
    var TicketId: String = ""
    var Task: String = ""
    var TaskDescription: String = ""
    var CourierID: Int ? = null
    var CourierName: String = ""
    var Amount = 0.0
    var title: String? = null
    var stopsmodel = ArrayList<Stop>()
    var stopPickUp = Stop()
    var stopDropOff = Stop()
    var defaultStops = ArrayList<Stop>()
    var AddedBy: String = ""


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

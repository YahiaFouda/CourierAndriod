package com.kadabra.courier.model



class TaskInfo {
    var taskId: String = ""
    var courierId: Int ? = null
     var locations= HashMap<String,location>()
    var active: Boolean = false


    constructor() {}

    constructor(
        taskId: String,
        courierId: Int,
        locations: HashMap<String,location>,
        active: Boolean
    ) {
        this.taskId = taskId
        this.courierId = courierId
        this.locations = locations
        this.active = active
    }

}

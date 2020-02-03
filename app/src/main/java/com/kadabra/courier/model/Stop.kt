package com.kadabra.courier.model

class Stop {
    var id: String = ""
    var StopName: String = ""
    var Latitude: Double? = null
    var Longitude: Double? = null
    var StopTypeID: Int = 0 //1 pickup 2 dropoff 3 stop "default"
    var StopType = ""
    var CreationDate = ""
    var TaskId = ""
    var addedBy = ""
    var address = ""
    var city = ""
    var state = ""
    var country = ""
    var postalCode = ""
    var knownName = ""
}
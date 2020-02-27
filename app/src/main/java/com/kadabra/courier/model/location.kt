package com.kadabra.courier.model

class location {


    var lat = ""
    var long = ""
    var isGpsEnabled=false

    constructor()

    constructor(lat: String, long: String,isGpsEnabled:Boolean) {
        this.lat = lat
        this.long = long
        this.isGpsEnabled=isGpsEnabled
    }
}
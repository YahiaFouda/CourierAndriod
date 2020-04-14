package com.kadabra.courier.model

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude


/**
 * Created by Mokhtar on 1/5/2020.
 */
class Courier {

    var CourierId: Int = 0
    @Exclude
    @set:Exclude
    @get:Exclude
    var ResponseStatus: Int = 0
    @Exclude
    @set:Exclude
    @get:Exclude
    var CourierName= ""
    @Exclude
    @set:Exclude
    @get:Exclude
    var Mobile= ""
    var name= ""
    @Exclude
    @set:Exclude
    @get:Exclude
    var email= ""
    @Exclude
    @set:Exclude
    @get:Exclude
    var password= ""
    @Exclude
    @set:Exclude
    @get:Exclude
    var phone= ""
    @Exclude
    @set:Exclude
    @get:Exclude
    var birthdate= ""
    var token= ""
    @Exclude
    @set:Exclude
    @get:Exclude
    var rating= ""
    @Exclude
    @set:Exclude
    @get:Exclude
    var created_at= ""
    @Exclude
    @set:Exclude
    @get:Exclude
    var updated_at= ""
    @Exclude
    @set:Exclude
    @get:Exclude
    var fullImagePath: String? = null
    var location: location? = null
    var isActive = false
    var haveTask=false
    var startTask=false

    var city = ""


    constructor()


    constructor(

        token: String,
        name: String,
        city: String,
        location: location,
        isActive: Boolean
    ) {

        this.token = token
        this.name = name
        this.city = city
        this.location = location
        this.isActive = isActive
    }


    constructor(
        id: Int,
        token: String,
        name: String,
        city: String,
        location: location,
        isActive: Boolean
    ) {
        this.CourierId = id
        this.token = token
        this.name = name
        this.city = city
        this.location = location
        this.isActive = isActive
    }


}
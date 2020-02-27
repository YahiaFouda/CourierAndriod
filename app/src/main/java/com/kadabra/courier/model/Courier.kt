package com.kadabra.courier.model

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.Exclude


/**
 * Created by Mokhtar on 1/5/2020.
 */
class Courier {

    var CourierId: Int = 0
    @Exclude @set:Exclude @get:Exclude
    var ResponseStatus: Int = 0
    @Exclude @set:Exclude @get:Exclude
    var CourierName: String = ""
    @Exclude @set:Exclude @get:Exclude
    var Mobile: String = ""
    var name: String = ""
    @Exclude @set:Exclude @get:Exclude
    var email: String = ""
    @Exclude @set:Exclude @get:Exclude
    var password: String = ""
    @Exclude @set:Exclude @get:Exclude
    var phone: String = ""
    @Exclude @set:Exclude @get:Exclude
    var birthdate: String = ""
    var token: String = ""
    @Exclude @set:Exclude @get:Exclude
    var rating: String = ""
    @Exclude @set:Exclude @get:Exclude
    var created_at: String = ""
    @Exclude @set:Exclude @get:Exclude
    var updated_at: String = ""
    @Exclude @set:Exclude @get:Exclude
    var fullImagePath: String? = null
    lateinit var location: location
    var isActive: Boolean = false
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
        id:Int,
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
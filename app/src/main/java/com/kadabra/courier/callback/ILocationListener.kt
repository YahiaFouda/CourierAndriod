package com.kadabra.courier.callback

import com.google.android.gms.location.LocationResult

interface ILocationListener {
    fun locationResponse(locationResult: LocationResult)
}
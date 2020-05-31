package com.kadabra.courier.model

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ServiceCost(
    @SerializedName("SeviceCostName")
    @Expose
    val serviceCostName: String,
    @SerializedName("Cost")
    @Expose
    val cost: Double)
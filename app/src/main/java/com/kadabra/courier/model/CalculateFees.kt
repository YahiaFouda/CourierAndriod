package com.kadabra.courier.model

import com.google.gson.annotations.Expose

import com.google.gson.annotations.SerializedName


class CalculateFees {
    @SerializedName("Status")
    @Expose
    var status: Int? = null
    @SerializedName("Message")
    @Expose
    var message: String? = null
    @SerializedName("ResponseObj")
    @Expose
    var responseObj: Task? = null

}
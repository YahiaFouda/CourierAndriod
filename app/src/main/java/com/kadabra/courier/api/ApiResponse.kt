package com.kadabra.courier.api

/**
 * Created by Mokhtar on 6/19/2019.
 */
class ApiResponse<T> {

    var ResponseObj: T?
    var Message: String = ""
    var Status: Int = 0

    constructor(ResponseObj: T?, status: Int, messaage: String) {
        this.ResponseObj = ResponseObj
        this.Message = messaage
        this.Status = status

    }
}
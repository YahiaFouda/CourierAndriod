package com.kadabra.courier.main


import android.util.Log

import com.kadabra.courier.base.BaseViewModel
import com.kadabra.courier.model.Point
import com.kadabra.courier.model.PointList
import com.kadabra.courier.firebase.MapFactory
import com.kadabra.courier.firebase.MapRequestBody
import com.google.gson.Gson
import com.google.gson.JsonObject
import java.util.ArrayList
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainViewModel : BaseViewModel() {

    private val TAG = javaClass.simpleName

    private var mMap: GoogleMap? = null
    private var mapFactory: MapFactory? = null

    private val markers = MutableLiveData<List<Marker>>()
    val points = MutableLiveData<List<Point>>()

    private val tempMarkerList = ArrayList<Marker>()

    override fun init() {}

    fun init(mMap: GoogleMap) {
        this.mMap = mMap
        this.mapFactory = MapFactory()
    }

    //MainActivity observe
    fun getMarkers(zoom: Double?): LiveData<List<Marker>> {

        setMarkers(zoom)
        return this.markers
    }

    fun setMarkers(zoom: Double?) {
//        for (i in tempMarkerList) {
//            i.map = null
//        }
        tempMarkerList.clear()

//        fetchList(tempMarkerList, markers, zoom)
    }

    // MainActivity is observing ths markers
    private fun TfetchList(markers: ArrayList<Marker>, liveDataMarkers: MutableLiveData<List<Marker>>?, zoom: Double?) {

        val api = mapFactory!!.create()

        // Make Callback Method
//        val req = api.getPoints(
//                MapRequestBody(

//
//                mMap!!.cameraPosition.target.longitude,
//                mMap!!.cameraPosition.target.latitude,
//
//                mMap!!.contentBounds.southWest.longitude,
//                mMap!!.contentBounds.southWest.latitude,
//
//                mMap!!.contentBounds.southEast.longitude,
//                mMap!!.contentBounds.southEast.latitude,
//
//                mMap!!.contentBounds.northEast.longitude,
//                mMap!!.contentBounds.northEast.latitude,
//
//                mMap!!.contentBounds.northWest.longitude,
//                mMap!!.contentBounds.northWest.latitude
//        )
//        )

        // insert callback queue
        var req=api.getPoints(MapRequestBody(1.01541, 1.034, 1.0213, 1.0354, 1.0454, 1.02125, 1.0315, 21.021, 1.02145, 1.031))
        req.enqueue(object : Callback<JsonObject> {
            override fun onResponse(call: Call<JsonObject>, res: Response<JsonObject>) {
                if (res.isSuccessful) {
                    Log.d(TAG, "result : " + res.body()!!)
                    val list = Gson().fromJson(res.body(), PointList::class.java)  // 파싱


                    for (i in 0 until list.list.size) {
                        val PointAt = list.list[i]
//                        val marker = Marker(LatLng(PointAt.g.Latitude, PointAt.g.lng))
//                        marker.icon = MarkerIcons.YELLOW
//                        marker.captionText = PointAt.id.toString()
//                        marker.captionTextSize = 14f
//                        marker.width = zoom!!.toInt() * 6
//                        marker.height = zoom.toInt() * 8

//                        markers.add(marker)
                    }

                    points!!.value = list.list // RecyclerView List<Point>
                    liveDataMarkers!!.setValue(markers)

                } else {
                    Log.d(TAG, "result : data")
                    Log.d(TAG, "result.res.body() : " + res.body()!!)
                }

            }

            override fun onFailure(call: Call<JsonObject>, t: Throwable) {

                Log.e(TAG, "result : data")
                t.printStackTrace()
                t.message
            }
        })
    }

    fun getPoints(): LiveData<List<Point>> {
        return this.points
    }
}

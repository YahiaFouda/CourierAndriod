package com.kadabra.courier.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.vipulasri.timelineview.TimelineView
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.R
import com.kadabra.courier.callback.IBottomSheetCallback
import com.kadabra.courier.model.Stop
import com.kadabra.courier.task.LocationDetailsActivity
import com.kadabra.courier.utilities.Alert
import com.kadabra.courier.utilities.AppConstants
import com.kadabra.courier.utilities.VectorDrawableUtils
import java.util.*
import android.net.Uri
import android.util.Log


/**
 * Created by Mokhtar on 1/8/2020.
 */

class StopAdapter(private val context: Context, private val stopList: ArrayList<Stop>) :
    RecyclerView.Adapter<StopAdapter.MyViewHolder>() {

    private lateinit var mLayoutInflater: LayoutInflater
    private var stop: Stop = Stop()
    private var listener: IBottomSheetCallback? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopAdapter.MyViewHolder {

//        val view = inflater.inflate(R.layout.stop_layout, parent, false)
//        return MyViewHolder(view, viewType)
        if (!::mLayoutInflater.isInitialized) {
            mLayoutInflater = LayoutInflater.from(parent.context)
        }


        val view = mLayoutInflater.inflate(R.layout.stop_layout, parent, false)

        return MyViewHolder(view, viewType)

    }

    override fun onBindViewHolder(holder: StopAdapter.MyViewHolder, position: Int) {
        stop = stopList[position]

        holder.tvStopName.text = stop.StopName

        when {
            stop.StopTypeID == 1 -> { //pick up
                holder.timeline.marker = VectorDrawableUtils.getDrawable(
                    holder.itemView.context,
                    R.drawable.ic_marker_active
                )
            }
            stop.StopTypeID == 3 -> { //default stop
                holder.timeline.marker = VectorDrawableUtils.getDrawable(
                    holder.itemView.context,
                    R.drawable.ic_marker_inactive
                )
            }
            stop.StopTypeID == 2 -> {  //drop off
                holder.timeline.marker = VectorDrawableUtils.getDrawable(
                    holder.itemView.context,
                    R.drawable.ic_marker
                )

            }

        }
    }

    override fun getItemViewType(position: Int): Int {
        return TimelineView.getTimeLineViewType(position, itemCount)
    }

    override fun getItemCount(): Int {
        return stopList.size
    }

//    override fun getItemId(position: Int): Long {
//        return super.getItemId(position)
//    }


    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

//    override fun getItemViewType(position: Int): Int {
//        return position
//    }


    inner class MyViewHolder(itemView: View, viewType: Int) : RecyclerView.ViewHolder(itemView) {

        var tvStopName: TextView = itemView.findViewById(R.id.tvStopName)
        val timeline: TimelineView = itemView.findViewById(R.id.timeline)

        init {
            timeline.initLine(viewType)


            itemView.setOnClickListener {
                if (NetworkManager().isNetworkAvailable(context)) {
                    val pos = adapterPosition
                    stop = stopList[pos]
                    AppConstants.currentSelectedStop = stop
                    showMapAlternatives(stop.Latitude.toString(), stop.Longitude.toString())
                } else
                    Alert.showMessage(
                        context,
                        context.getString(R.string.no_internet)
                    )

            }

        }
    }

    private fun showMapAlternatives(latitude: String, longitude: String) {
        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.map_option))
            .setMessage(context.getString(R.string.map_title))
            .setIcon(android.R.drawable.ic_dialog_map)
            .setPositiveButton(context.getString(R.string.app_name)) { dialog, which ->
                context.startActivity(Intent(context, LocationDetailsActivity::class.java))
            }
            .setNegativeButton(context.getString(R.string.google_map)) { dialog, which ->

                val builder = Uri.Builder()
                builder.scheme("https")
                    .authority("www.google.com")
                    .appendPath("maps")
                    .appendPath("dir")
                    .appendPath("")
                    .appendQueryParameter("api", "1")
                    .appendQueryParameter("destination", "$latitude,$longitude")
                val url = builder.build().toString()
                Log.d("Directions", url)
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                context.startActivity(i)

            }
            .show()
    }
}

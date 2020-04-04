package com.kadabra.courier.adapter

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kadabra.courier.model.Stop
import com.kadabra.courier.model.Notification
import com.kadabra.courier.utilities.AppConstants
import android.widget.FrameLayout
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityCompat
import com.kadabra.Networking.NetworkManager
import com.kadabra.courier.R
import com.kadabra.courier.location.LocationHelper
import com.kadabra.courier.notifications.NotificationDetailsActivity
import com.kadabra.courier.utilities.Alert


/**
 * Created by Mokhtar on 1/8/2020.
 */

class NotificationAdapter(
    private val context: Context,
    private val NotificationsList: ArrayList<Notification>
) :
    RecyclerView.Adapter<NotificationAdapter.MyViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var notification: Notification = Notification()


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NotificationAdapter.MyViewHolder {


        if (viewType == 1)//default layout
        {
            val view =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.notification_layout, parent, false)
            return MyViewHolder(view)
        } else {
            val view1 =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.notification_layout_readed, parent, false)
            return MyViewHolder(view1)
        }
    }


    override fun onBindViewHolder(holder: NotificationAdapter.MyViewHolder, position: Int) {
        notification = NotificationsList[position]


        holder.tvTitle.text = notification.notificationTitle
        holder.tvSubject.text = notification.notificationContent
        holder.tvDate.text = notification.notificationDate

    }

    override fun getItemCount(): Int {
        return NotificationsList.size
    }

    override fun getItemId(position: Int): Long {
        return super.getItemId(position)
    }


    override fun getItemViewType(position: Int): Int {
//        return position
var notification=NotificationsList[position]
        return if (notification.isReaded) {
            2 // READED

        } else
            1//UNREADED

    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var tvTitle: TextView = itemView.findViewById(com.kadabra.courier.R.id.tvTitle)
        var tvSubject: TextView = itemView.findViewById(com.kadabra.courier.R.id.tvSubject)
        var tvDate: TextView =
            itemView.findViewById(R.id.tvDate)


        init {


            itemView.setOnClickListener {
                //                if (NetworkManager().isNetworkAvailable(context)) {
                val pos = adapterPosition
                notification = NotificationsList[pos]
               AppConstants.CurrentSelecedNotification=notification
                context.startActivity(Intent(context, NotificationDetailsActivity::class.java))


            }


        }



    }
}

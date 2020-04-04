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
import com.kadabra.courier.model.Task
import com.kadabra.courier.task.TaskDetailsActivity
import com.kadabra.courier.utilities.AppConstants
import android.widget.FrameLayout
import android.view.animation.AnimationUtils
import androidx.core.app.ActivityCompat
import com.kadabra.courier.R
import com.kadabra.courier.location.Location
import com.kadabra.courier.location.LocationHelper
import com.kadabra.courier.utilities.Alert
import com.reach.plus.admin.util.UserSessionManager


/**
 * Created by Mokhtar on 1/8/2020.
 */

class TaskHistoryAdapter(private val context: Context, private val tasksList: ArrayList<Task>) :
    RecyclerView.Adapter<TaskHistoryAdapter.MyViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var task: Task = Task()


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TaskHistoryAdapter.MyViewHolder {

        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.task_layout, parent, false)
        return MyViewHolder(view)
    }


    override fun onBindViewHolder(holder: TaskHistoryAdapter.MyViewHolder, position: Int) {
        task = tasksList[position]

        if (!task.TaskName.isNullOrEmpty())
            holder.tvTaskName.text = task.TaskName


        if (task.Status == "In progress")
            holder.tvStatus.text =context.getString(R.string.in_progress)

        else if (task.Status == "Completed")
            holder.tvStatus.text =context.getString(R.string.completed)

//        holder.tvStatus.text = task.Status//context.getString(R.string.completed)
        task.stopsmodel.forEach {

            when (it.StopTypeID) {
                1 -> { //pickup
                    task.stopPickUp = it
                    holder.tvPickupLocation.text =
                        context.getString(com.kadabra.courier.R.string.from) + " " + it.StopName
                }
                2 -> { //dropOff
                    task.stopDropOff = it
                    holder.tvDropOffLocation.text =
                        context.getString(com.kadabra.courier.R.string.to) + " " + it.StopName
                }

            }

        }



        if (task.Amount!! > 0)
            holder.tvTaskAmount.text =
                task.Amount.toString() + " " + context.getString(com.kadabra.courier.R.string.le)
        else
            holder.tvTaskAmount.text = "0 " + context.getString(com.kadabra.courier.R.string.le)


    }

    override fun getItemCount(): Int {
        return tasksList.size
    }

    override fun getItemId(position: Int): Long {
        return super.getItemId(position)
    }


    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var tvTaskName: TextView = itemView.findViewById(com.kadabra.courier.R.id.tvTaskName)
        var tvTaskAmount: TextView = itemView.findViewById(com.kadabra.courier.R.id.tvTaskAmount)
        var tvPickupLocation: TextView =
            itemView.findViewById(com.kadabra.courier.R.id.tvPickupLocation)
        var tvDropOffLocation: TextView =
            itemView.findViewById(com.kadabra.courier.R.id.tvDropOffLocation)
        var tvStatus: TextView = itemView.findViewById(com.kadabra.courier.R.id.tvStatus)


        init {

        }


    }

    private fun checkPermissions(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }


}

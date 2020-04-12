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
import androidx.core.app.ActivityCompat
import com.kadabra.courier.R
import com.kadabra.courier.location.LocationHelper
import com.kadabra.courier.utilities.Alert
import kotlinx.android.synthetic.main.activity_task_details.*


/**
 * Created by Mokhtar on 1/8/2020.
 */

class TaskAdapter(private val context: Context, private var tasksList: ArrayList<Task>) :
    RecyclerView.Adapter<TaskAdapter.MyViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var task: Task = Task()
    private var pickUpStops = ArrayList<Stop>()
    private var dropOffStops = ArrayList<Stop>()
    private var normalStops = ArrayList<Stop>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskAdapter.MyViewHolder {

//        val view = inflater.inflate(R.layout.task_layout, parent, false)

//        if (viewType == R.layout.task_layout)
//            view = LayoutInflater.from(parent.context).inflate(R.layout.task_layout, parent, false)
//        else if(viewType == R.layout.task_layout_accepted)
//            view = LayoutInflater.from(parent.context).inflate(R.layout.task_layout_accepted, parent, false)

        if (viewType == 1)//default layout
        {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.task_layout, parent, false)
            return MyViewHolder(view)
        } else {
            val view1 =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.task_layout_accepted, parent, false)
            return MyViewHolder(view1)
        }


    }


    override fun onBindViewHolder(holder: TaskAdapter.MyViewHolder, position: Int) {
        task = tasksList[position]

        if (!task.TaskName.isNullOrEmpty())
            holder.tvTaskName.text = task.TaskName

        if (task.Status == "In progress")
            holder.tvStatus.text = context.getString(R.string.in_progress)
        else
            holder.tvStatus.text = context.getString(R.string.new_task)


        task.stopsmodel.forEach {

            when (it.StopTypeID) {
                1 -> { //pickup
                    task.stopPickUp = it
                    holder.tvPickupLocation.text =
                        context.getString(R.string.from) + " " + it.StopName
                }
                2 -> { //dropOff
                    task.stopDropOff = it
                    holder.tvDropOffLocation.text =
                        context.getString(R.string.to) + " " + it.StopName
                }
                3 -> {
                    task.defaultStops.add(it)
                }
            }

        }



        if (task.Amount!! > 0)
            holder.tvTaskAmount.text =
                task.Amount.toString() + " " + context.getString(R.string.le)
        else
            holder.tvTaskAmount.text = "0 " + context.getString(R.string.le)


    }

    override fun getItemCount(): Int {
        return tasksList.size
    }

    override fun getItemId(position: Int): Long {
        return super.getItemId(position)
    }


    override fun getItemViewType(position: Int): Int {

//        return position
//        return if (AppConstants.CurrentAcceptedTask.TaskId == tasksList[position].TaskId) {
        return if (tasksList[position].Status == "In progress") {
            Log.d("task", AppConstants.CurrentAcceptedTask.TaskId)
            AppConstants.CurrentAcceptedTask = tasksList[position]
            2 // R.layout.task_layout_accepted

        } else
            1//R.layout.task_layout

//        var task = tasksList[position]
//        return if (tasksList[position].Status == "In progress") {
//            AppConstants.CurrentAcceptedTask=task
//            Log.d("task", AppConstants.CurrentAcceptedTask.TaskId)
//            2 // R.layout.task_layout_accepted
//
//        } else
//            1//R.layout.task_layout

//        notifyDataSetChanged()
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        var tvTaskAmount: TextView = itemView.findViewById(R.id.tvTaskAmount)
        var tvPickupLocation: TextView =
            itemView.findViewById(R.id.tvPickupLocation)
        var tvDropOffLocation: TextView =
            itemView.findViewById(R.id.tvDropOffLocation)
        var tvStatus: TextView = itemView.findViewById(R.id.tvStatus)


        init {


            itemView.setOnClickListener {
                //                if (NetworkManager().isNetworkAvailable(context)) {
                val pos = adapterPosition
                task = tasksList[pos]
//                var stops = prepareTaskStops(task.stopsmodel)
//                task.stopsmodel = stops


                if (checkPermissions() && LocationHelper.shared.isGPSEnabled()) {
                    AppConstants.CurrentSelectedTask = task
                    context.startActivity(Intent(context, TaskDetailsActivity::class.java))
                } else {
                    if (!checkPermissions())
                        Alert.showMessage(
                            context,
                            context.getString(R.string.permission_rationale)
                        )
                    else if (!LocationHelper.shared.isGPSEnabled())
                        Alert.showMessage(
                            context,
                            context.getString(R.string.error_gps)
                        )
                }
//                } else
//                    Alert.showMessage(
//                        context,
//                        context.getString(R.string.no_internet)
//                    )
            }


        }


    }

    private fun checkPermissions(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun prepareTaskStops(stops: ArrayList<Stop>): ArrayList<Stop> {
        pickUpStops.clear()
        dropOffStops.clear()
        normalStops.clear()

        if (stops.size > 0) {
            stops.sortBy { it.StopTypeID }

            stops.forEach {

                when (it.StopTypeID) {
                    1 -> { //pickup
                        pickUpStops.add(it)
                    }
                    2 -> { //dropOff
                        dropOffStops.add(it)
                    }
                    3 -> {
                        normalStops.add(it)
                    }

                }
            }

            stops.clear()

            if (pickUpStops.count() > 0)
                stops.addAll(pickUpStops)
            if (normalStops.count() > 0)
                stops.addAll(normalStops)
            if (dropOffStops.count() > 0)
                stops.addAll(dropOffStops)


        }

        return stops

    }

    fun updateData(data: ArrayList<Task>) {
        this.tasksList = data
        notifyDataSetChanged()
    }
}

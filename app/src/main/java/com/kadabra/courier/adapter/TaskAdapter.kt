package com.kadabra.courier.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kadabra.courier.R
import com.kadabra.courier.model.Stop
import com.kadabra.courier.model.Task
import com.kadabra.courier.task.TaskDetailsActivity
import com.kadabra.courier.utilities.AppConstants


/**
 * Created by Mokhtar on 1/8/2020.
 */

class TaskAdapter(private val context: Context, private val tasksList: ArrayList<Task>) :
    RecyclerView.Adapter<TaskAdapter.MyViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var task: Task = Task()
    private var pickUpStops = ArrayList<Stop>()
    private var dropOffStops = ArrayList<Stop>()
    private var normalStops = ArrayList<Stop>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskAdapter.MyViewHolder {

        val view = inflater.inflate(R.layout.task_layout, parent, false)

        return MyViewHolder(view)

    }

    override fun onBindViewHolder(holder: TaskAdapter.MyViewHolder, position: Int) {
        task = tasksList[position]

        if (!task.Task.isNullOrEmpty())
            holder.tvTaskName.text = task.Task

//        if (task.stopsmodel.size > 0) {
//            task.stopsmodel.sortBy { it.StopTypeID }
//
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

//
        }
//
//            task.stopsmodel.clear()
//
//            if (pickUpStops.count() > 0)
//                task.stopsmodel.addAll(pickUpStops)
//            if (normalStops.count() > 0)
//                task.stopsmodel.addAll(normalStops)
//            if (dropOffStops.count() > 0)
//                task.stopsmodel.addAll(dropOffStops)
//
//
//        }


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
        return position
    }

    inner class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var tvTaskName: TextView = itemView.findViewById(R.id.tvTaskName)
        var tvTaskAmount: TextView = itemView.findViewById(R.id.tvTaskAmount)
        var tvPickupLocation: TextView = itemView.findViewById(R.id.tvPickupLocation)
        var tvDropOffLocation: TextView = itemView.findViewById(R.id.tvDropOffLocation)


        init {


            itemView.setOnClickListener {
                val pos = adapterPosition
                task = tasksList[pos]
//                var stops = prepareTaskStops(task.stopsmodel)
//                task.stopsmodel = stops
                AppConstants.CurrentSelectedTask = task
                context.startActivity(Intent(context, TaskDetailsActivity::class.java))

            }


        }


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
}

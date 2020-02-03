package com.kadabra.courier.recyclerview

import android.view.LayoutInflater
import android.view.ViewGroup

import com.kadabra.courier.model.Point

import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.databinding.library.baseAdapters.BR
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewAdapter(@param:LayoutRes private val layoutID: Int,
                          private val recyclerViewModel: RecyclerViewModel
) : RecyclerView.Adapter<RecyclerViewAdapter.RecyclerViewHolder>() {


    var points: List<Point>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, viewType, parent, false)

        return RecyclerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerViewHolder, position: Int) {
        holder.bind(recyclerViewModel, position)
    }

    override fun getItemCount(): Int {
        return if (points == null) 0 else points!!.size
    }

    override fun getItemViewType(position: Int): Int {
        return getLayoutIdForPosition(position)
    }

    private fun getLayoutIdForPosition(position: Int): Int {
        return this.layoutID
    }

    fun getPointAtPosition(position: Int): Point {
        return this.points!![position]
    }

    inner class RecyclerViewHolder(private val binding: ViewDataBinding) : RecyclerView.ViewHolder(binding.root) {

        internal fun bind(viewModel: RecyclerViewModel, position: Int?) {

            /** Very Important!!  */
            // re-usable ViewHolder
            viewModel.setImages(position)
            binding.setVariable(BR.viewmodel, viewModel)
//            binding.setVariable(BR.position, position)
            binding.executePendingBindings()
        }
    }
}


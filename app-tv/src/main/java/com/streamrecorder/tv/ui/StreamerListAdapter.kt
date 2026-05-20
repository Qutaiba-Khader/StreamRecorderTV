package com.streamrecorder.tv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.streamrecorder.core.model.Target
import com.streamrecorder.tv.R
import java.text.SimpleDateFormat
import java.util.*

class StreamerListAdapter(
    private val onStreamerClick: (Target) -> Unit,
) : ListAdapter<Target, StreamerListAdapter.ViewHolder>(DIFF) {

    private var selectedPosition = -1

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Target>() {
            override fun areItemsTheSame(a: Target, b: Target) = a.id == b.id
            override fun areContentsTheSame(a: Target, b: Target) = a == b
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatar: ImageView = view.findViewById(R.id.avatar)
        val name: TextView = view.findViewById(R.id.name)
        val count: TextView = view.findViewById(R.id.count)
        val latestDate: TextView = view.findViewById(R.id.latestDate)
        val liveBadge: TextView = view.findViewById(R.id.liveBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_streamer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val target = getItem(position)

        holder.name.text = target.name
        holder.count.text = "${target.countTotal} videos"
        holder.liveBadge.visibility = if (target.isLive) View.VISIBLE else View.GONE
        holder.itemView.isSelected = position == selectedPosition

        if (target.latestTs > 0) {
            val date = Date(target.latestTs * 1000)
            val fmt = SimpleDateFormat("MMM d", Locale.US)
            holder.latestDate.text = fmt.format(date)
            holder.latestDate.visibility = View.VISIBLE
        } else {
            holder.latestDate.visibility = View.GONE
        }

        Glide.with(holder.itemView.context)
            .load(target.avatarUrl)
            .placeholder(R.drawable.avatar_placeholder)
            .circleCrop()
            .into(holder.avatar)

        holder.itemView.setOnClickListener {
            val oldPos = selectedPosition
            selectedPosition = holder.bindingAdapterPosition
            if (oldPos >= 0) notifyItemChanged(oldPos)
            notifyItemChanged(selectedPosition)
            onStreamerClick(target)
        }
    }

    fun clearSelection() {
        val old = selectedPosition
        selectedPosition = -1
        if (old >= 0) notifyItemChanged(old)
    }
}

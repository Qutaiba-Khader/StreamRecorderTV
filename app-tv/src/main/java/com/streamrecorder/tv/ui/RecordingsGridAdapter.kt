package com.streamrecorder.tv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.streamrecorder.core.model.Recording
import com.streamrecorder.tv.R

class RecordingsGridAdapter(
    private val onRecordingClick: (Recording) -> Unit,
    private val onRecordingLongClick: (Recording) -> Unit,
) : ListAdapter<Recording, RecordingsGridAdapter.ViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Recording>() {
            override fun areItemsTheSame(a: Recording, b: Recording) = a.id == b.id
            override fun areContentsTheSame(a: Recording, b: Recording) = a == b
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.thumbnail)
        val title: TextView = view.findViewById(R.id.title)
        val date: TextView = view.findViewById(R.id.date)
        val duration: TextView = view.findViewById(R.id.duration)
        val resolution: TextView = view.findViewById(R.id.resolution)
        val favIcon: TextView = view.findViewById(R.id.favIcon)
        val watchProgress: ProgressBar = view.findViewById(R.id.watchProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recording, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val rec = getItem(position)

        holder.title.text = rec.displayTitle
        holder.date.text = rec.dateTime
        holder.duration.text = rec.durationHr

        val best = rec.bestSource
        if (best != null) {
            holder.resolution.text = "${best.resolution}p"
            holder.resolution.visibility = View.VISIBLE
        } else {
            holder.resolution.visibility = View.GONE
        }

        // Fav icon
        if (rec.isFav) {
            holder.favIcon.text = "💚" // green heart
            holder.favIcon.visibility = View.VISIBLE
        } else {
            holder.favIcon.visibility = View.GONE
        }

        // Fav card background tint
        if (rec.isFav) {
            holder.itemView.setBackgroundColor(
                holder.itemView.context.getColor(R.color.bg_card_fav)
            )
        }

        // Thumbnail
        val thumbUrl = rec.thumbnailUrl
        if (thumbUrl != null) {
            Glide.with(holder.itemView.context)
                .load(thumbUrl)
                .placeholder(R.drawable.thumb_placeholder)
                .centerCrop()
                .into(holder.thumbnail)
        } else {
            holder.thumbnail.setImageResource(R.drawable.thumb_placeholder)
        }

        // Watch progress bar
        val bestRes = best?.resolution
        if (bestRes != null) {
            val progress = rec.watchProgress(bestRes)
            if (progress != null && progress > 0.01f) {
                holder.watchProgress.visibility = View.VISIBLE
                holder.watchProgress.progress = (progress * 1000).toInt()
            } else {
                holder.watchProgress.visibility = View.GONE
            }
        } else {
            holder.watchProgress.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onRecordingClick(rec) }
        holder.itemView.setOnLongClickListener { onRecordingLongClick(rec); true }
    }
}

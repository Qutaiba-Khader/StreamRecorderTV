package com.streamrecorder.tvapp

import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.leanback.widget.PageRow
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.RowHeaderPresenter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class HeaderPresenter(private val onLongClick: ((Int) -> Unit)? = null) : RowHeaderPresenter() {
    private var unselectedAlpha = 0.5f

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val ctx = parent.context
        val dp = ctx.resources.displayMetrics.density

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((16 * dp).toInt(), (4 * dp).toInt(), (16 * dp).toInt(), (4 * dp).toInt())
        }
        val icon = ImageView(ctx).apply {
            val size = (36 * dp).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = (10 * dp).toInt() }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        val label = TextView(ctx).apply {
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            maxLines = 1
        }
        row.addView(icon)
        row.addView(label)
        row.alpha = unselectedAlpha
        return ViewHolder(row)
    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder, item: Any?) {
        val pageRow = item as? PageRow ?: return
        val headerItem = pageRow.headerItem

        val row = viewHolder.view as LinearLayout
        val icon = row.getChildAt(0) as ImageView
        val label = row.getChildAt(1) as TextView

        if (headerItem is StreamerHeaderItem) {
            when {
                headerItem.isLive -> {
                    label.text = "🔴 ${headerItem.name}"
                    label.setTextColor(Color.WHITE)
                }
                headerItem.isPostprocessing -> {
                    label.text = "🟡 ${headerItem.name}"
                    label.setTextColor(Color.parseColor("#FFB300"))
                }
                else -> {
                    label.text = headerItem.name
                    label.setTextColor(Color.WHITE)
                }
            }
            icon.visibility = View.VISIBLE
            val logoUrl = headerItem.logoUrl?.replace("250x250", "56x56")
            if (!logoUrl.isNullOrEmpty() && logoUrl != "null") {
                Glide.with(icon.context)
                    .asBitmap()
                    .load(logoUrl)
                    .circleCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(icon)
            } else {
                icon.setImageDrawable(null)
            }
            row.setOnLongClickListener {
                onLongClick?.invoke(headerItem.id.toInt())
                true
            }
        } else if (headerItem.id == MainFragment.HIDDEN_ID) {
            val count = AppPreferences.getHiddenSources().size
            label.text = "🚫 Hidden ($count)"
            label.setTextColor(Color.parseColor("#FF8866"))
            icon.visibility = View.GONE
            row.setOnLongClickListener(null)
        } else {
            label.text = "⚙  ${headerItem.name}"
            label.setTextColor(Color.WHITE)
            icon.visibility = View.GONE
            row.setOnLongClickListener(null)
        }
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder) {
        val row = viewHolder.view as LinearLayout
        val icon = row.getChildAt(0) as ImageView
        Glide.with(icon.context).clear(icon)
    }

    override fun onSelectLevelChanged(holder: ViewHolder) {
        holder.view.alpha = unselectedAlpha + holder.selectLevel * (1f - unselectedAlpha)
    }
}

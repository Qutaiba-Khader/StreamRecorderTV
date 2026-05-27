package com.streamrecorder.tvapp

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class RecoCardPresenter(private val onLongClick: ((RecoFile) -> Unit)? = null) : Presenter() {
    private var cardWidth = 0
    private var cardHeight = 0

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val ctx = parent.context
        val dm = ctx.resources.displayMetrics
        val d = dm.density
        val t = AppPreferences.currentTheme()

        val frac = AppPreferences.cardWidthFraction()
        val screenWidth = maxOf(dm.widthPixels, dm.heightPixels)
        cardWidth = (screenWidth * frac / 3).toInt()
        cardHeight = cardWidth * 9 / 16

        val cardBg = GradientDrawable().apply {
            setColor(t.cardBg)
            cornerRadius = 8 * d
        }

        val totalHeight = cardHeight + (56 * d).toInt()

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = cardBg
            clipToOutline = true
            outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
            isFocusable = true
            isFocusableInTouchMode = true
            layoutParams = ViewGroup.MarginLayoutParams(cardWidth, totalHeight).apply {
                val m = (4 * d).toInt()
                setMargins(m, m, m, m)
            }
            tag = cardBg
        }

        val thumbContainer = FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(cardWidth, cardHeight)
        }

        val thumbImage = ImageView(ctx).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(Color.parseColor("#1A1A2E"))
        }
        thumbContainer.addView(thumbImage)

        root.addView(thumbContainer)

        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val ph = (10 * d).toInt()
            val pv = (6 * d).toInt()
            setPadding(ph, pv, ph, pv)
        }

        val dateText = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(t.textPrimary)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 1
        }
        body.addView(dateText)

        val metaText = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(t.textSecondary)
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (2 * d).toInt() }
        }
        body.addView(metaText)

        root.addView(body)

        return ViewHolder(root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val file = item as RecoFile
        val root = viewHolder.view as LinearLayout
        val ctx = root.context
        val d = ctx.resources.displayMetrics.density
        val t = AppPreferences.currentTheme()
        val bg = root.tag as? GradientDrawable

        val thumbContainer = root.getChildAt(0) as FrameLayout
        val thumbImage = thumbContainer.getChildAt(0) as ImageView
        val body = root.getChildAt(1) as LinearLayout
        val dateView = body.getChildAt(0) as TextView
        val metaView = body.getChildAt(1) as TextView

        dateView.text = file.date ?: "Unknown date"
        dateView.setTextColor(t.textPrimary)

        val meta = StringBuilder()
        file.resolution?.let { meta.append("${it}p") }
        val sizeStr = formatSize(file.size)
        if (sizeStr.isNotEmpty()) {
            if (meta.isNotEmpty()) meta.append("  ·  ")
            meta.append(sizeStr)
        }
        metaView.text = if (meta.isNotEmpty()) meta.toString() else file.filename
        metaView.setTextColor(t.textSecondary)

        bg?.setColor(t.cardBg)

        // Load thumbnail
        val thumbUrl = file.thumbUrl?.let { ApiClient.recoThumbUrl(it) }
        if (!thumbUrl.isNullOrEmpty()) {
            Glide.with(ctx)
                .load(thumbUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(cardWidth, cardHeight)
                .centerCrop()
                .into(thumbImage)
        } else {
            thumbImage.setImageDrawable(null)
            thumbImage.setBackgroundColor(Color.parseColor("#1A1A2E"))
        }

        val focusBg = Color.parseColor("#2A2A4A")
        root.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            val scale = if (hasFocus) 1.12f else 1.0f
            root.elevation = if (hasFocus) 8 * d else 0f
            root.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
            bg?.setColor(if (hasFocus) focusBg else t.cardBg)
        }

        root.setOnLongClickListener {
            onLongClick?.invoke(file)
            true
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val root = viewHolder.view as LinearLayout
        val thumbContainer = root.getChildAt(0) as FrameLayout
        val thumbImage = thumbContainer.getChildAt(0) as ImageView
        Glide.with(root.context).clear(thumbImage)
        thumbImage.setImageDrawable(null)
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) "%.2f GB".format(mb / 1024) else "%.1f MB".format(mb)
    }
}

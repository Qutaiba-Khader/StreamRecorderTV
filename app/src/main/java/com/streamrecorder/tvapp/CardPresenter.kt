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

class CardPresenter(private val onLongClick: ((Any) -> Unit)? = null) : Presenter() {
    private var cardWidth = 0
    private var cardHeight = 0

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val ctx = parent.context
        val dm = ctx.resources.displayMetrics

        val frac = AppPreferences.cardWidthFraction()
        val screenWidth = maxOf(dm.widthPixels, dm.heightPixels)
        cardWidth = (screenWidth * frac / 3).toInt()
        cardHeight = cardWidth * 9 / 16

        val d = dm.density
        val t = AppPreferences.currentTheme()

        val cardBg = GradientDrawable().apply {
            setColor(t.cardBg)
            cornerRadius = 8 * d
        }

        val totalHeight = cardHeight + (64 * d).toInt()

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

        val durBg = GradientDrawable().apply {
            setColor(Color.parseColor("#CC000000"))
            cornerRadius = 4 * d
        }
        val durText = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(Color.parseColor("#BBBBBB"))
            background = durBg
            val ph = (6 * d).toInt()
            val pv = (2 * d).toInt()
            setPadding(ph, pv, ph, pv)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.BOTTOM or Gravity.END
                val m = (6 * d).toInt()
                setMargins(m, m, m, m)
            }
        }
        thumbContainer.addView(durText)

        root.addView(thumbContainer)

        val body = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val ph = (10 * d).toInt()
            val pv = (8 * d).toInt()
            setPadding(ph, pv, ph, pv)
        }

        val titleText = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(t.textPrimary)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 1
        }
        body.addView(titleText)

        val metaRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * d).toInt() }
        }
        body.addView(metaRow)

        root.addView(body)

        return ViewHolder(root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val root = findRoot(viewHolder.view)
        val ctx = root.context
        val d = ctx.resources.displayMetrics.density
        val t = AppPreferences.currentTheme()

        val bg = root.tag as? GradientDrawable
        val thumbContainer = root.getChildAt(0) as FrameLayout
        val thumbImage = thumbContainer.getChildAt(0) as ImageView
        val durText = thumbContainer.getChildAt(1) as TextView
        val body = root.getChildAt(1) as LinearLayout
        val titleText = body.getChildAt(0) as TextView
        val metaRow = body.getChildAt(1) as LinearLayout

        when (item) {
            is LiveStreamCard -> bindLiveCard(root, bg, thumbContainer, thumbImage, durText, titleText, metaRow, item, d, t)
            is Recording -> bindRecording(root, bg, thumbImage, durText, titleText, metaRow, item, d, t)
        }

        val isFav = item is Recording && item.isFav
        val normalBg = if (isFav) Color.parseColor("#1A2E1A") else t.cardBg
        val focusBg = if (isFav) Color.parseColor("#2A4A2A") else Color.parseColor("#2A2A4A")
        bg?.setColor(normalBg)

        root.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            val scale = if (hasFocus) 1.12f else 1.0f
            root.elevation = if (hasFocus) 8 * d else 0f
            root.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
            bg?.setColor(if (hasFocus) focusBg else normalBg)
        }

        root.setOnLongClickListener {
            onLongClick?.invoke(item)
            true
        }
    }

    private fun bindLiveCard(
        root: LinearLayout, bg: GradientDrawable?,
        thumbContainer: FrameLayout, thumbImage: ImageView, durText: TextView,
        titleText: TextView, metaRow: LinearLayout,
        card: LiveStreamCard, d: Float, t: ThemeColors
    ) {
        val ctx = root.context
        bg?.setColor(Color.parseColor("#CC1A0000"))

        thumbImage.setImageDrawable(null)
        thumbImage.setBackgroundColor(Color.parseColor("#331A0000"))

        val liveOverlay = TextView(ctx).apply {
            text = "LIVE"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTextColor(Color.parseColor("#FF4444"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        if (thumbContainer.childCount > 2) thumbContainer.removeViewAt(2)
        thumbContainer.addView(liveOverlay)

        durText.visibility = View.GONE

        titleText.text = card.title.ifEmpty { "${card.streamerName} LIVE" }
        titleText.setTextColor(Color.parseColor("#FF6666"))

        metaRow.removeAllViews()
        card.streams.keys.take(3).forEach { quality ->
            val pillBg = GradientDrawable().apply {
                setColor(Color.parseColor("#33FF4444"))
                cornerRadius = 3 * d
            }
            val pill = TextView(ctx).apply {
                text = quality
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setTextColor(Color.parseColor("#FF6666"))
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                background = pillBg
                val ph = (5 * d).toInt()
                val pv = (1 * d).toInt()
                setPadding(ph, pv, ph, pv)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (5 * d).toInt() }
            }
            metaRow.addView(pill)
        }
    }

    private fun bindRecording(
        root: LinearLayout, bg: GradientDrawable?,
        thumbImage: ImageView, durText: TextView,
        titleText: TextView, metaRow: LinearLayout,
        rec: Recording, d: Float, t: ThemeColors
    ) {
        val ctx = root.context

        bg?.setColor(t.cardBg)

        titleText.text = rec.displayDate
        titleText.setTextColor(t.textPrimary)

        durText.text = rec.durationHr
        durText.visibility = if (rec.durationHr.isNotEmpty()) View.VISIBLE else View.GONE

        val thumbContainer = root.getChildAt(0) as FrameLayout
        while (thumbContainer.childCount > 2) thumbContainer.removeViewAt(thumbContainer.childCount - 1)

        if (rec.isFav) {
            val favBg = GradientDrawable().apply {
                setColor(Color.parseColor("#CC000000"))
                cornerRadius = 4 * d
            }
            val favIcon = TextView(ctx).apply {
                text = "💚"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                background = favBg
                val ph = (4 * d).toInt()
                setPadding(ph, (2 * d).toInt(), ph, (2 * d).toInt())
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.END
                    val m = (6 * d).toInt()
                    setMargins(m, m, m, m)
                }
            }
            thumbContainer.addView(favIcon)
        }

        val watchPct = if (rec.watchPercentage > 0) rec.watchPercentage else AppPreferences.getWatchPercent(rec.id)
        if (watchPct > 0) {
            val barHeight = (3 * d).toInt()
            val barBg = View(ctx).apply {
                setBackgroundColor(Color.parseColor("#4DFFFFFF"))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, barHeight
                ).apply { gravity = Gravity.BOTTOM }
            }
            thumbContainer.addView(barBg)
            val barFill = View(ctx).apply {
                setBackgroundColor(Color.parseColor("#FF0000"))
                layoutParams = FrameLayout.LayoutParams(
                    0, barHeight
                ).apply { gravity = Gravity.BOTTOM }
            }
            thumbContainer.addView(barFill)
            barFill.post {
                val totalWidth = thumbContainer.width
                val fillWidth = (totalWidth * watchPct / 100f).toInt()
                barFill.layoutParams = (barFill.layoutParams as FrameLayout.LayoutParams).apply {
                    width = fillWidth
                }
            }
        }

        metaRow.removeAllViews()
        val sortedSources = rec.sources.sortedByDescending { it.resolution }
        for (src in sortedSources) {
            val isHigh = src.resolution >= 1080
            val pillBgColor = if (isHigh) Color.parseColor("#1A7B9FFF") else Color.parseColor("#1AFFAA4A")
            val pillTextColor = if (isHigh) Color.parseColor("#7B9FFF") else Color.parseColor("#FFAA44")

            val pillBg = GradientDrawable().apply {
                setColor(pillBgColor)
                cornerRadius = 3 * d
            }
            val pill = TextView(ctx).apply {
                text = "${src.resolution}p"
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
                setTextColor(pillTextColor)
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                background = pillBg
                val ph = (5 * d).toInt()
                val pv = (1 * d).toInt()
                setPadding(ph, pv, ph, pv)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = (5 * d).toInt() }
            }
            metaRow.addView(pill)
        }

        val size = rec.bestSource?.let {
            val mb = it.filesize / (1024.0 * 1024.0)
            if (mb >= 1024) "%.2f GB".format(mb / 1024) else "%.1f MB".format(mb)
        } ?: ""
        if (size.isNotEmpty()) {
            val sizeText = TextView(ctx).apply {
                text = size
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                setTextColor(t.textSecondary)
            }
            metaRow.addView(sizeText)
        }

        rec.thumbnail?.let { url ->
            Glide.with(ctx)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(cardWidth, cardHeight)
                .centerCrop()
                .into(thumbImage)
        } ?: run {
            thumbImage.setImageDrawable(null)
            thumbImage.setBackgroundColor(Color.parseColor("#1A1A2E"))
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val root = findRoot(viewHolder.view)
        val thumbContainer = root.getChildAt(0) as FrameLayout
        while (thumbContainer.childCount > 2) thumbContainer.removeViewAt(thumbContainer.childCount - 1)
        val thumbImage = thumbContainer.getChildAt(0) as ImageView
        Glide.with(root.context).clear(thumbImage)
        thumbImage.setImageDrawable(null)
    }

    private fun findRoot(view: View): LinearLayout {
        if (view is LinearLayout) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is LinearLayout) return child
                if (child is ViewGroup) {
                    val found = findRoot(child)
                    if (found is LinearLayout) return found
                }
            }
        }
        return view as LinearLayout
    }
}

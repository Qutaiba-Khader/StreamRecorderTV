package com.streamrecorder.tvapp

import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

class CardPresenter : Presenter() {
    private var cardWidth = 0
    private var cardHeight = 0

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val ctx = parent.context
        if (cardWidth == 0) {
            val dm = ctx.resources.displayMetrics
            val screenWidth = maxOf(dm.widthPixels, dm.heightPixels)
            cardWidth = (screenWidth * 0.85f / 3).toInt()
            cardHeight = cardWidth * 9 / 16
        }
        val card = ImageCardView(ctx).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(cardWidth, cardHeight)
        }
        return ViewHolder(card)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val rec = item as Recording
        val card = viewHolder.view as ImageCardView

        card.titleText = rec.displayDate
        val res = rec.sources.sortedByDescending { it.resolution }
            .joinToString(" · ") { "${it.resolution}p" }
        val size = rec.bestSource?.let {
            val mb = it.filesize / (1024.0 * 1024.0)
            if (mb >= 1024) "%.1f GB".format(mb / 1024) else "%.0f MB".format(mb)
        } ?: ""
        card.contentText = "$res  $size  ${rec.durationHr}"

        rec.thumbnail?.let { url ->
            Glide.with(card.context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(cardWidth, cardHeight)
                .centerCrop()
                .into(card.mainImageView!!)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val card = viewHolder.view as ImageCardView
        card.mainImage = null
    }
}

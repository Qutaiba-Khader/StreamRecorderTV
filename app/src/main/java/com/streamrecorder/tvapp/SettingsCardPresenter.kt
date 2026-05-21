package com.streamrecorder.tvapp

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.leanback.widget.Presenter

class SettingsCardPresenter : Presenter() {
    private var cardWidth = 0
    private var cardHeight = 0

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val ctx = parent.context
        val dm = ctx.resources.displayMetrics

        if (cardWidth == 0) {
            val screenWidth = maxOf(dm.widthPixels, dm.heightPixels)
            cardWidth = (screenWidth * 0.80f / 3).toInt()
            cardHeight = (80 * dm.density).toInt()
        }

        val t = AppPreferences.currentTheme()

        val bg = GradientDrawable().apply {
            setColor(t.cardBg)
            cornerRadius = 12 * dm.density
        }

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((16 * dm.density).toInt(), (12 * dm.density).toInt(), (16 * dm.density).toInt(), (12 * dm.density).toInt())
            background = bg
            isFocusable = true
            isFocusableInTouchMode = true
            layoutParams = ViewGroup.MarginLayoutParams(cardWidth, cardHeight).apply {
                val m = (6 * dm.density).toInt()
                setMargins(m, m, m, m)
            }
        }

        val title = TextView(ctx).apply {
            setTextColor(t.textSecondary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            gravity = Gravity.CENTER
            maxLines = 1
        }

        val value = TextView(ctx).apply {
            setTextColor(t.textPrimary)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            maxLines = 1
            setPadding(0, (4 * dm.density).toInt(), 0, 0)
        }

        card.addView(title)
        card.addView(value)
        card.tag = bg

        return ViewHolder(card)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val setting = item as SettingsItem
        val card = findCard(viewHolder.view)
        val titleView = card.getChildAt(0) as TextView
        val valueView = card.getChildAt(1) as TextView
        titleView.text = setting.title
        valueView.text = setting.value

        val t = AppPreferences.currentTheme()
        val bg = card.tag as? GradientDrawable

        bg?.setColor(t.cardBg)
        titleView.setTextColor(t.textSecondary)
        valueView.setTextColor(t.textPrimary)

        card.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                bg?.setColor(t.accent)
                titleView.setTextColor(Color.parseColor("#111111"))
                valueView.setTextColor(Color.parseColor("#111111"))
            } else {
                bg?.setColor(t.cardBg)
                titleView.setTextColor(t.textSecondary)
                valueView.setTextColor(t.textPrimary)
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {}

    private fun findCard(view: View): LinearLayout {
        if (view is LinearLayout) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is LinearLayout) return child
                if (child is ViewGroup) {
                    val found = findCard(child)
                    if (found is LinearLayout) return found
                }
            }
        }
        return view as LinearLayout
    }
}

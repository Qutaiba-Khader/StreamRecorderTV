package com.streamrecorder.tvapp

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class HiddenFragment : VerticalGridSupportFragment(),
    BrowseSupportFragment.MainFragmentAdapterProvider {

    private val mainAdapter = BrowseSupportFragment.MainFragmentAdapter(this)
    private val gridAdapter = ArrayObjectAdapter(HiddenCardPresenter())

    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> = mainAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val presenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_SMALL, false)
        presenter.numberOfColumns = 3
        gridPresenter = presenter
        adapter = gridAdapter

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            val hidden = item as? HiddenSource ?: return@OnItemViewClickedListener
            unhide(hidden)
        }

        loadHidden()
        mainAdapter.fragmentHost?.notifyDataReady(mainAdapter)
    }

    private fun loadHidden() {
        gridAdapter.clear()
        val items = AppPreferences.getHiddenSources()
        if (items.isEmpty()) {
            gridAdapter.add(HiddenSource(recId = -1, res = 0, date = "", streamer = "No hidden sources"))
        } else {
            gridAdapter.addAll(0, items)
        }
    }

    private fun unhide(hidden: HiddenSource) {
        if (hidden.recId < 0) return
        lifecycleScope.launch {
            try {
                ApiClient.unhideSource(hidden.recId, hidden.res)
                if (!isAdded) return@launch
                AppPreferences.removeHidden(hidden.recId, hidden.res)
                loadHidden()
                Toast.makeText(requireContext(), "Unhidden ${hidden.res}p", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                if (!isAdded) return@launch
                Toast.makeText(requireContext(), "Error unhiding source", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class HiddenCardPresenter : Presenter() {
    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val ctx = parent.context
        val d = ctx.resources.displayMetrics.density
        val t = AppPreferences.currentTheme()

        val frac = AppPreferences.cardWidthFraction()
        val screenWidth = maxOf(ctx.resources.displayMetrics.widthPixels, ctx.resources.displayMetrics.heightPixels)
        val cardWidth = (screenWidth * frac / 3).toInt()

        val bg = GradientDrawable().apply {
            setColor(Color.parseColor("#1A1A0A"))
            cornerRadius = 8 * d
            setStroke((1 * d).toInt(), Color.parseColor("#44FF6644"))
        }

        val root = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = bg
            isFocusable = true
            isFocusableInTouchMode = true
            val ph = (16 * d).toInt()
            val pv = (14 * d).toInt()
            setPadding(ph, pv, ph, pv)
            layoutParams = ViewGroup.MarginLayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                val m = (4 * d).toInt()
                setMargins(m, m, m, m)
            }
            tag = bg
        }

        val streamer = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#FF8866"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 1
        }
        root.addView(streamer)

        val date = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(t.textPrimary)
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * d).toInt() }
        }
        root.addView(date)

        val meta = TextView(ctx).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(t.textSecondary)
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (4 * d).toInt() }
        }
        root.addView(meta)

        val action = TextView(ctx).apply {
            text = "Click to unhide"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            setTextColor(Color.parseColor("#66FF6644"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (6 * d).toInt() }
        }
        root.addView(action)

        return ViewHolder(root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val hidden = item as HiddenSource
        val root = viewHolder.view as LinearLayout
        val d = root.context.resources.displayMetrics.density
        val bg = root.tag as? GradientDrawable

        val streamer = root.getChildAt(0) as TextView
        val date = root.getChildAt(1) as TextView
        val meta = root.getChildAt(2) as TextView
        val action = root.getChildAt(3) as TextView

        if (hidden.recId < 0) {
            streamer.text = hidden.streamer
            date.text = ""
            meta.text = ""
            action.text = ""
            bg?.setStroke(0, Color.TRANSPARENT)
        } else {
            streamer.text = hidden.streamer
            date.text = hidden.date
            meta.text = "${hidden.res}p · ID ${hidden.recId}"
            action.text = "Click to unhide"
            bg?.setStroke((1 * d).toInt(), Color.parseColor("#44FF6644"))
        }

        root.onFocusChangeListener = android.view.View.OnFocusChangeListener { _, hasFocus ->
            val scale = if (hasFocus) 1.08f else 1.0f
            root.elevation = if (hasFocus) 8 * d else 0f
            root.animate().scaleX(scale).scaleY(scale).setDuration(150).start()
            bg?.setColor(if (hasFocus) Color.parseColor("#2A2A1A") else Color.parseColor("#1A1A0A"))
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
}

package com.streamrecorder.tvapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class VideoGridFragment : VerticalGridSupportFragment(),
    BrowseSupportFragment.MainFragmentAdapterProvider {

    private val mainAdapter = BrowseSupportFragment.MainFragmentAdapter(this)
    private val gridAdapter = ArrayObjectAdapter(CardPresenter { item -> onItemLongClicked(item) })
    private var headerView: LinearLayout? = null
    private var statusText: TextView? = null
    private var allRecordings: List<Recording> = emptyList()
    private var liveCard: LiveStreamCard? = null
    private var postProcessingCard: PostProcessingCard? = null
    private var favOnly = false
    private var dataLoaded = false
    private var favButton: TextView? = null
    private var playingRecId: Int = -1
    private var playingDuration: Int = 0
    private var playingRes: Int = 0
    private lateinit var playerLauncher: ActivityResultLauncher<Intent>

    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> = mainAdapter

    companion object {
        fun newInstance(
            targetId: Int, isLive: Boolean = false,
            isPostprocessing: Boolean = false,
            streamerName: String = "", logoUrl: String = "",
            platform: String = "tiktok", countTotal: Int = 0
        ) = VideoGridFragment().apply {
            arguments = Bundle().apply {
                putInt("targetId", targetId)
                putBoolean("isLive", isLive)
                putBoolean("isPostprocessing", isPostprocessing)
                putString("streamerName", streamerName)
                putString("logoUrl", logoUrl)
                putString("platform", platform)
                putInt("countTotal", countTotal)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val gridView = super.onCreateView(inflater, container, savedInstanceState)!!

        val verticalGrid = gridView.findViewById<androidx.leanback.widget.VerticalGridView>(
            androidx.leanback.R.id.browse_grid
        )
        verticalGrid?.setGravity(Gravity.START)

        val ctx = requireContext()
        val d = ctx.resources.displayMetrics.density

        val wrapper = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        headerView = createHeader()
        wrapper.addView(headerView)

        val t = AppPreferences.currentTheme()
        statusText = TextView(ctx).apply {
            text = "Loading..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(t.textSecondary)
            gravity = Gravity.CENTER
            visibility = if (dataLoaded) View.GONE else View.VISIBLE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (24 * d).toInt() }
        }
        wrapper.addView(statusText)

        wrapper.addView(gridView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        return wrapper
    }

    private fun createHeader(): LinearLayout {
        val ctx = requireContext()
        val d = ctx.resources.displayMetrics.density
        val t = AppPreferences.currentTheme()

        val streamerName = arguments?.getString("streamerName") ?: ""
        val logoUrl = arguments?.getString("logoUrl") ?: ""
        val platform = arguments?.getString("platform") ?: "tiktok"
        val countTotal = arguments?.getInt("countTotal", 0) ?: 0
        val isLive = arguments?.getBoolean("isLive", false) ?: false
        val isPostprocessing = arguments?.getBoolean("isPostprocessing", false) ?: false

        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val ph = (24 * d).toInt()
            val pv = (12 * d).toInt()
            setPadding(ph, pv, ph, pv)
        }

        val avatarSize = (36 * d).toInt()
        val avatarBg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#2A2A3E"))
        }
        val avatar = ImageView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(avatarSize, avatarSize).apply {
                marginEnd = (10 * d).toInt()
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
            background = avatarBg
        }
        header.addView(avatar)

        val actualLogoUrl = logoUrl.replace("250x250", "56x56")
        if (actualLogoUrl.isNotEmpty() && actualLogoUrl != "null") {
            Glide.with(ctx)
                .asBitmap()
                .load(actualLogoUrl)
                .circleCrop()
                .into(avatar)
        }

        val info = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val nameText = TextView(ctx).apply {
            text = streamerName
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextColor(t.textPrimary)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 1
        }
        info.addView(nameText)

        val metaBuilder = StringBuilder("$platform · $countTotal recordings")
        if (isLive) metaBuilder.append(" · LIVE")
        if (isPostprocessing) metaBuilder.append(" · PROCESSING")
        val metaText = TextView(ctx).apply {
            text = metaBuilder.toString()
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTextColor(when {
                isLive -> Color.parseColor("#FF5555")
                isPostprocessing -> Color.parseColor("#FFB300")
                else -> t.textSecondary
            })
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (2 * d).toInt() }
        }
        info.addView(metaText)

        header.addView(info)

        val btnBg = GradientDrawable().apply {
            setColor(Color.parseColor("#1A7B9FFF"))
            cornerRadius = 6 * d
        }
        val favBtn = TextView(ctx).apply {
            text = "💚 Favs"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(Color.parseColor("#7B9FFF"))
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            background = btnBg
            val ph = (14 * d).toInt()
            val pv = (6 * d).toInt()
            setPadding(ph, pv, ph, pv)
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = true
            setOnClickListener { toggleFavFilter() }
        }
        favButton = favBtn
        header.addView(favBtn)

        return header
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onPlayerResult(result.data)
        }
        val presenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_SMALL, false)
        presenter.numberOfColumns = 3
        gridPresenter = presenter
        adapter = gridAdapter

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            when (item) {
                is LiveStreamCard -> openLiveStream(item)
                is PostProcessingCard -> { /* not clickable */ }
                is Recording -> openRecording(item)
            }
        }

        loadRecordings()
    }

    private fun onItemLongClicked(item: Any) {
        when (item) {
            is Recording -> showRecordingContextMenu(item)
            is LiveStreamCard -> showLiveContextMenu(item)
            is PostProcessingCard -> { /* not clickable */ }
        }
    }

    private fun showRecordingContextMenu(rec: Recording) {
        val ctx = requireContext()
        val sortedSources = rec.sources.sortedByDescending { it.resolution }
        val items = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        for (src in sortedSources) {
            val sz = formatSize(src.filesize)
            items.add("▶  Play ${src.resolution}p  ($sz)")
            actions.add { launchPlayer(ApiClient.playUrl(rec.id, src.resolution)) }
            items.add("📲  Open with... ${src.resolution}p")
            actions.add { launchSystemPicker(ApiClient.playUrl(rec.id, src.resolution)) }
        }

        items.add(if (rec.isFav) "💚  Unfavorite" else "🤍  Favorite")
        actions.add { toggleFav(rec) }

        for (src in sortedSources) {
            items.add("❌  Hide ${src.resolution}p")
            actions.add { hideSource(rec, src.resolution) }
        }

        AlertDialog.Builder(ctx)
            .setTitle(rec.displayDate)
            .setItems(items.toTypedArray()) { _, which -> actions[which]() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLiveContextMenu(card: LiveStreamCard) {
        if (card.streams.isEmpty()) {
            Toast.makeText(requireContext(), "Loading stream...", Toast.LENGTH_SHORT).show()
            return
        }
        val ctx = requireContext()
        val items = mutableListOf<String>()
        val urls = mutableListOf<String>()

        for ((quality, url) in card.streams) {
            items.add("▶  $quality")
            urls.add(url)
            items.add("📲  $quality — Open with...")
            urls.add(url)
        }

        AlertDialog.Builder(ctx)
            .setTitle("${card.streamerName} LIVE")
            .setItems(items.toTypedArray()) { _, which ->
                val url = urls[which]
                if (which % 2 == 0) launchPlayer(url) else launchSystemPicker(url)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleFav(rec: Recording) {
        lifecycleScope.launch {
            try {
                val newFav = ApiClient.toggleFav(rec.id)
                val updated = rec.copy(isFav = newFav)
                allRecordings = allRecordings.map { if (it.id == rec.id) updated else it }
                val pos = (0 until gridAdapter.size()).firstOrNull {
                    (gridAdapter.get(it) as? Recording)?.id == rec.id
                }
                if (pos != null) {
                    if (favOnly && !newFav) {
                        gridAdapter.removeItems(pos, 1)
                    } else {
                        gridAdapter.replace(pos, updated)
                    }
                }
                val targetId = arguments?.getInt("targetId") ?: return@launch
                ApiClient.clearCacheFor(targetId)
                Toast.makeText(requireContext(),
                    if (newFav) "Added to favorites" else "Removed from favorites",
                    Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Error toggling favorite", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideSource(rec: Recording, res: Int) {
        val streamerName = arguments?.getString("streamerName") ?: ""
        lifecycleScope.launch {
            try {
                ApiClient.hideSource(rec.id, res)
                AppPreferences.addHidden(rec.id, res, rec.displayDate, streamerName)
                val pos = (0 until gridAdapter.size()).firstOrNull {
                    (gridAdapter.get(it) as? Recording)?.id == rec.id
                }
                if (pos != null) {
                    val updatedSources = rec.sources.filter { it.resolution != res }
                    if (updatedSources.isEmpty()) {
                        gridAdapter.removeItems(pos, 1)
                    } else {
                        gridAdapter.replace(pos, rec.copy(sources = updatedSources))
                    }
                }
                val targetId = arguments?.getInt("targetId") ?: return@launch
                ApiClient.clearCacheFor(targetId)
                Toast.makeText(requireContext(), "${res}p source hidden", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Error hiding source", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openRecording(rec: Recording) {
        playingRecId = rec.id
        playingDuration = rec.duration
        val source = AppPreferences.resolveResolution(rec.sources)
        source?.let { src ->
            playingRes = src.resolution
            launchPlayer(ApiClient.playUrl(rec.id, src.resolution))
        }
    }

    private fun openLiveStream(card: LiveStreamCard) {
        if (card.streams.isEmpty()) {
            Toast.makeText(requireContext(), "Loading stream...", Toast.LENGTH_SHORT).show()
            return
        }
        playingRecId = -1
        playingDuration = 0
        val url = card.streams.values.firstOrNull() ?: return
        launchPlayer(url)
    }

    private fun launchPlayer(url: String) {
        val playerPkg = AppPreferences.resolvePlayerPackage()
        val resumeMs = if (playingRecId > 0) {
            val pct = AppPreferences.getWatchPercent(playingRecId)
            if (pct in 1..95 && playingDuration > 0) (playingDuration * 1000L * pct / 100) else 0L
        } else 0L

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            if (playerPkg.isNotEmpty()) setPackage(playerPkg)
            putExtra("return_result", true)
            if (resumeMs > 0) {
                putExtra("position", resumeMs.toInt())
                putExtra("extra_position", resumeMs)
            }
        }
        try {
            playerLauncher.launch(intent)
        } catch (_: Exception) {
            try {
                playerLauncher.launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    putExtra("return_result", true)
                })
            } catch (_: Exception) {}
        }
    }

    private fun onPlayerResult(data: Intent?) {
        if (!AppPreferences.trackPosition || playingRecId < 0) return

        var posMs = -1L
        var durMs = -1L

        if (data != null) {
            posMs = data.getLongExtra("position", -1L)
            if (posMs < 0) posMs = data.getIntExtra("position", -1).toLong()
            if (posMs < 0) posMs = data.getLongExtra("extra_position", -1L)
            if (posMs < 0) posMs = data.getIntExtra("com.mxtech.intent.result.position", -1).toLong()

            durMs = data.getLongExtra("duration", -1L)
            if (durMs < 0) durMs = data.getIntExtra("duration", -1).toLong()
            if (durMs < 0) durMs = data.getLongExtra("extra_duration", -1L)
            if (durMs < 0) durMs = data.getIntExtra("com.mxtech.intent.result.duration", -1).toLong()
        }

        if (durMs <= 0 && playingDuration > 0) durMs = playingDuration * 1000L

        if (posMs > 10000 && durMs > 0) {
            AppPreferences.saveWatchPosition(playingRecId, posMs, durMs)
            val recId = playingRecId
            val res = playingRes
            lifecycleScope.launch {
                ApiClient.saveWatchPosition(recId, res, posMs, durMs)
                ApiClient.clearCacheFor(arguments?.getInt("targetId") ?: -1)
            }
            val pct = ((posMs * 100) / durMs).toInt().coerceIn(0, 100)
            val pos = (0 until gridAdapter.size()).firstOrNull {
                (gridAdapter.get(it) as? Recording)?.id == playingRecId
            }
            if (pos != null) {
                val rec = gridAdapter.get(pos) as? Recording
                if (rec != null) gridAdapter.replace(pos, rec.copy(watchPercentage = pct))
            }
        }
        playingRecId = -1
        playingDuration = 0
        playingRes = 0
    }

    private fun launchSystemPicker(url: String) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            requireContext().startActivity(Intent.createChooser(intent, "Open with..."))
        } catch (_: Exception) {}
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) "%.2f GB".format(mb / 1024) else "%.1f MB".format(mb)
    }

    private fun toggleFavFilter() {
        favOnly = !favOnly
        val bg = favButton?.background as? GradientDrawable
        if (favOnly) {
            favButton?.text = "💚 Favs ✓"
            bg?.setColor(Color.parseColor("#335CCC5C"))
            favButton?.setTextColor(Color.parseColor("#5CCC5C"))
        } else {
            favButton?.text = "💚 Favs"
            bg?.setColor(Color.parseColor("#1A7B9FFF"))
            favButton?.setTextColor(Color.parseColor("#7B9FFF"))
        }
        rebuildGrid()
    }

    private fun rebuildGrid() {
        gridAdapter.clear()
        if (!favOnly) {
            liveCard?.let { gridAdapter.add(it) }
            postProcessingCard?.let { gridAdapter.add(it) }
        }
        val list = if (favOnly) allRecordings.filter { it.isFav } else allRecordings
        gridAdapter.addAll(gridAdapter.size(), list)
    }

    fun refreshData() {
        val targetId = arguments?.getInt("targetId") ?: return
        ApiClient.clearCacheFor(targetId)
        allRecordings = emptyList()
        liveCard = null
        postProcessingCard = null
        dataLoaded = false
        gridAdapter.clear()
        statusText?.text = "Loading..."
        statusText?.visibility = View.VISIBLE
        loadRecordings()
    }

    private fun loadRecordings() {
        val targetId = arguments?.getInt("targetId") ?: return
        val isLive = arguments?.getBoolean("isLive", false) ?: false
        val isPostprocessing = arguments?.getBoolean("isPostprocessing", false) ?: false
        val streamerName = arguments?.getString("streamerName") ?: ""

        if (isLive && streamerName.isNotEmpty()) {
            liveCard = LiveStreamCard(
                title = "$streamerName LIVE",
                streams = emptyMap(),
                streamerName = streamerName
            )
            gridAdapter.add(liveCard!!)
        }

        if (isPostprocessing && streamerName.isNotEmpty()) {
            postProcessingCard = PostProcessingCard(streamerName = streamerName)
            gridAdapter.add(postProcessingCard!!)
        }

        lifecycleScope.launch {
            try {
                val liveDeferred = if (isLive && streamerName.isNotEmpty()) {
                    async {
                        val slug = streamerName.removePrefix("@")
                        ApiClient.loadLiveStream(slug)
                    }
                } else null

                val recordings = ApiClient.loadRecordings(targetId)
                allRecordings = recordings
                dataLoaded = true
                statusText?.visibility = View.GONE
                rebuildGrid()
                mainAdapter.fragmentHost?.notifyDataReady(mainAdapter)

                liveDeferred?.let { deferred ->
                    val liveData = deferred.await()
                    if (liveData != null) {
                        liveCard = LiveStreamCard(
                            title = liveData.title,
                            streams = liveData.streams,
                            streamerName = streamerName
                        )
                        val livePos = (0 until gridAdapter.size()).firstOrNull {
                            gridAdapter.get(it) is LiveStreamCard
                        }
                        if (livePos != null) {
                            gridAdapter.replace(livePos, liveCard!!)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StreamRecGrid", "loadRecordings($targetId) failed", e)
                statusText?.text = "Failed to load recordings"
                statusText?.visibility = View.VISIBLE
                mainAdapter.fragmentHost?.notifyDataReady(mainAdapter)
            }
        }
    }
}

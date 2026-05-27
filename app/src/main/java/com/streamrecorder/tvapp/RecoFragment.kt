package com.streamrecorder.tvapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class RecoFragment : RowsSupportFragment(),
    BrowseSupportFragment.MainFragmentAdapterProvider {

    private val mainAdapter = BrowseSupportFragment.MainFragmentAdapter(this)
    private var statusText: TextView? = null
    private var favButton: TextView? = null
    private var favOnly = false
    private var allReco: Map<String, List<RecoFile>> = emptyMap()
    private var playingFile: RecoFile? = null
    private lateinit var playerLauncher: ActivityResultLauncher<Intent>

    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> = mainAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val rowsView = super.onCreateView(inflater, container, savedInstanceState)!!

        val ctx = requireContext()
        val d = ctx.resources.displayMetrics.density
        val t = AppPreferences.currentTheme()

        val wrapper = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val header = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val ph = (24 * d).toInt()
            val pv = (12 * d).toInt()
            setPadding(ph, pv, ph, pv)
        }
        val titleText = TextView(ctx).apply {
            text = "📂  Downloads"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            setTextColor(t.textPrimary)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(titleText)

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

        wrapper.addView(header)

        statusText = TextView(ctx).apply {
            text = "Loading..."
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            setTextColor(t.textSecondary)
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (24 * d).toInt() }
        }
        wrapper.addView(statusText)

        wrapper.addView(rowsView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        return wrapper
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        playerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            onPlayerResult(result.data)
        }
        adapter = ArrayObjectAdapter(ListRowPresenter())

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            val file = item as? RecoFile ?: return@OnItemViewClickedListener
            playRecoFile(file)
        }

        loadReco()
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
        rebuildRows()
    }

    private fun rebuildRows() {
        val rowsAdapter = adapter as ArrayObjectAdapter
        rowsAdapter.clear()

        val data = if (favOnly) {
            allReco.mapValues { (_, files) -> files.filter { it.isFav } }
                .filter { (_, files) -> files.isNotEmpty() }
        } else {
            allReco
        }

        if (data.isEmpty()) {
            statusText?.text = if (favOnly) "No favorite downloads" else "No downloaded recordings"
            statusText?.visibility = View.VISIBLE
            return
        }

        statusText?.visibility = View.GONE

        for ((username, files) in data) {
            val headerItem = HeaderItem(username)
            val cardAdapter = ArrayObjectAdapter(RecoCardPresenter { file -> showRecoPopup(file) })
            for (file in files) {
                cardAdapter.add(file)
            }
            rowsAdapter.add(ListRow(headerItem, cardAdapter))
        }
    }

    private fun playRecoFile(file: RecoFile) {
        if (!isAdded) return
        playingFile = file
        val url = ApiClient.recoPlayUrl(file.user, file.filename)
        val playerPkg = AppPreferences.resolvePlayerPackage()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            if (playerPkg.isNotEmpty()) setPackage(playerPkg)
            putExtra("return_result", true)
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
        val file = playingFile ?: return
        playingFile = null

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

        if (posMs > 10000 && durMs > 0) {
            val pct = ((posMs * 100) / durMs).toInt().coerceIn(0, 100)
            val updated = file.copy(watchPct = pct)
            allReco = allReco.mapValues { (_, files) ->
                files.map { if (it.user == file.user && it.filename == file.filename) updated else it }
            }
            rebuildRows()
            lifecycleScope.launch {
                ApiClient.saveRecoWatchPosition(file.user, file.filename, posMs, durMs)
            }
        }
    }

    private fun showRecoPopup(file: RecoFile) {
        if (!isAdded) return
        val ctx = requireContext()
        val items = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        items.add("▶  Play")
        actions.add { playRecoFile(file) }

        items.add("📲  Open with...")
        actions.add {
            val url = ApiClient.recoPlayUrl(file.user, file.filename)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), "video/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                startActivity(Intent.createChooser(intent, "Open with..."))
            } catch (_: Exception) {}
        }

        items.add(if (file.isFav) "💚  Unfavorite" else "🤍  Favorite")
        actions.add { toggleRecoFav(file) }

        if (file.watchPct > 0) {
            items.add("🔄  Clear progress (${file.watchPct}%)")
            actions.add { clearProgress(file) }
        }

        items.add("🗑  Delete")
        actions.add { confirmDelete(file) }

        val title = file.date ?: file.filename
        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setItems(items.toTypedArray()) { _, which -> actions[which]() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun toggleRecoFav(file: RecoFile) {
        lifecycleScope.launch {
            val newFav = ApiClient.toggleRecoFav(file.user, file.filename)
            if (!isAdded) return@launch
            val updated = file.copy(isFav = newFav)
            allReco = allReco.mapValues { (_, files) ->
                files.map { if (it.user == file.user && it.filename == file.filename) updated else it }
            }
            rebuildRows()
            Toast.makeText(requireContext(),
                if (newFav) "Added to favorites" else "Removed from favorites",
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearProgress(file: RecoFile) {
        lifecycleScope.launch {
            ApiClient.deleteRecoWatchPosition(file.user, file.filename)
            if (!isAdded) return@launch
            val updated = file.copy(watchPct = 0)
            allReco = allReco.mapValues { (_, files) ->
                files.map { if (it.user == file.user && it.filename == file.filename) updated else it }
            }
            rebuildRows()
            Toast.makeText(requireContext(), "Progress cleared", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmDelete(file: RecoFile) {
        if (!isAdded) return
        AlertDialog.Builder(requireContext())
            .setTitle("Delete recording?")
            .setMessage("${file.filename}\n${formatSize(file.size)}")
            .setPositiveButton("Delete") { _, _ -> deleteFile(file) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteFile(file: RecoFile) {
        lifecycleScope.launch {
            val ok = ApiClient.deleteRecoFile(file.user, file.filename)
            if (!isAdded) return@launch
            if (ok) {
                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                loadReco()
            } else {
                Toast.makeText(requireContext(), "Delete failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadReco() {
        lifecycleScope.launch {
            try {
                val reco = ApiClient.loadReco()
                if (!isAdded) return@launch
                allReco = reco
                rebuildRows()
                mainAdapter.fragmentHost?.notifyDataReady(mainAdapter)
            } catch (e: Exception) {
                if (!isAdded) return@launch
                statusText?.text = "Failed to load downloads"
                statusText?.visibility = View.VISIBLE
                mainAdapter.fragmentHost?.notifyDataReady(mainAdapter)
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1024) "%.2f GB".format(mb / 1024) else "%.1f MB".format(mb)
    }
}

package com.streamrecorder.tvapp

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.PageRow
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.PresenterSelector
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class MainFragment : BrowseSupportFragment() {
    companion object {
        const val SETTINGS_ID = -1L
        const val RECO_ID = -3L
        const val HIDDEN_ID = -2L
    }

    var currentGridFragment: VideoGridFragment? = null
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headersState = HEADERS_HIDDEN
        isHeadersTransitionOnBackEnabled = true

        val t = AppPreferences.currentTheme()
        brandColor = t.brand
        enableMainFragmentScaling(false)
        title = ""

        setHeaderPresenterSelector(object : PresenterSelector() {
            private val presenter = HeaderPresenter { targetId -> showStreamerPopup(targetId) }
            override fun getPresenter(item: Any?): Presenter = presenter
        })

        mainFragmentRegistry.registerFragment(PageRow::class.java, GridFragmentFactory())

        val cached = AppPreferences.cachedTargetsJson
        if (cached != null) {
            try {
                setTargetsAdapter(ApiClient.parseTargetsFromJson(cached))
            } catch (_: Exception) {}
        }

        loadTargets()
        syncSettings()
    }

    private fun syncSettings() {
        lifecycleScope.launch {
            try {
                val remote = ApiClient.loadSettings()
                if (remote.length() > 0) {
                    AppPreferences.applyServerSettings(remote)
                }
            } catch (_: Exception) {}
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleView?.visibility = View.GONE
    }

    private fun setTargetsAdapter(targets: List<Target>) {
        val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        rowsAdapter.add(PageRow(HeaderItem(SETTINGS_ID, "Settings")))
        targets.forEach { t ->
            val header = StreamerHeaderItem(
                t.id.toLong(), t.name, t.logo, t.isLive,
                t.isPostprocessing, AppPreferences.isPinned(t.id),
                t.platform, t.countTotal
            )
            rowsAdapter.add(PageRow(header))
        }
        rowsAdapter.add(PageRow(HeaderItem(RECO_ID, "Downloads")))
        rowsAdapter.add(PageRow(HeaderItem(HIDDEN_ID, "Hidden")))
        adapter = rowsAdapter
    }

    private fun loadTargets() {
        lifecycleScope.launch {
            try {
                val targets = ApiClient.loadTargets()
                setTargetsAdapter(targets)
                ApiClient.cachedTargetsJson?.let { AppPreferences.cachedTargetsJson = it }
                ChannelHelper.updateLiveChannel(requireContext(), targets)
            } catch (e: Exception) {
                Log.e("StreamRecMain", "loadTargets failed", e)
            }
        }
    }

    fun refreshAll() {
        ApiClient.clearCache()
        loadTargets()
    }

    private fun showStreamerPopup(targetId: Int) {
        val isPinned = AppPreferences.isPinned(targetId)
        val items = arrayOf("🔄  Refresh", if (isPinned) "📌  Unpin" else "📌  Pin")
        AlertDialog.Builder(requireContext())
            .setItems(items) { _, which ->
                when (which) {
                    0 -> {
                        currentGridFragment?.refreshData()
                        Toast.makeText(requireContext(), "Refreshing...", Toast.LENGTH_SHORT).show()
                    }
                    1 -> {
                        val nowPinned = AppPreferences.togglePin(targetId)
                        pushSettings()
                        refreshAll()
                        Toast.makeText(requireContext(),
                            if (nowPinned) "Pinned to top" else "Unpinned",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pushSettings() {
        lifecycleScope.launch {
            ApiClient.saveSettings(AppPreferences.getFullSettings())
        }
    }

    inner class GridFragmentFactory : BrowseSupportFragment.FragmentFactory<Fragment>() {
        override fun createFragment(row: Any?): Fragment {
            val pageRow = row as PageRow
            val header = pageRow.headerItem
            if (header.id == SETTINGS_ID) return SettingsFragment()
            if (header.id == RECO_ID) return RecoFragment()
            if (header.id == HIDDEN_ID) return HiddenFragment()
            val streamerHeader = header as? StreamerHeaderItem
            val fragment = VideoGridFragment.newInstance(
                targetId = header.id.toInt(),
                isLive = streamerHeader?.isLive ?: false,
                isPostprocessing = streamerHeader?.isPostprocessing ?: false,
                streamerName = header.name ?: "",
                logoUrl = streamerHeader?.logoUrl ?: "",
                platform = streamerHeader?.platform ?: "tiktok",
                countTotal = streamerHeader?.countTotal ?: 0
            )
            currentGridFragment = fragment
            return fragment
        }
    }
}

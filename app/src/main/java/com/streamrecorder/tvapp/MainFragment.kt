package com.streamrecorder.tvapp

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
        private const val TAG = "StreamRecMain"
        const val SETTINGS_ID = -1L
    }

    private var currentGridFragment: VideoGridFragment? = null
    private var initialLoadDone = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headersState = HEADERS_HIDDEN
        isHeadersTransitionOnBackEnabled = true

        val t = AppPreferences.currentTheme()
        brandColor = t.brand
        enableMainFragmentScaling(false)
        title = ""

        setHeaderPresenterSelector(object : PresenterSelector() {
            private val presenter = HeaderPresenter()
            override fun getPresenter(item: Any?): Presenter = presenter
        })

        setBrowseTransitionListener(object : BrowseTransitionListener() {
            override fun onHeadersTransitionStop(withHeaders: Boolean) {
                if (!withHeaders) {
                    currentGridFragment?.loadData()
                }
            }
        })

        mainFragmentRegistry.registerFragment(PageRow::class.java, GridFragmentFactory())
        loadTargets()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        titleView?.visibility = View.GONE
    }

    private fun loadTargets() {
        lifecycleScope.launch {
            try {
                val targets = ApiClient.loadTargets()
                val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
                rowsAdapter.add(PageRow(HeaderItem(SETTINGS_ID, "Settings")))
                targets.forEach { t ->
                    val header = StreamerHeaderItem(
                        t.id.toLong(), t.name, t.logo, t.isLive,
                        t.platform, t.countTotal
                    )
                    rowsAdapter.add(PageRow(header))
                }
                adapter = rowsAdapter
                view?.postDelayed({
                    if (!initialLoadDone) {
                        initialLoadDone = true
                        currentGridFragment?.loadData()
                    }
                }, 500)
            } catch (e: Exception) {
                Log.e(TAG, "loadTargets failed", e)
            }
        }
    }

    inner class GridFragmentFactory : BrowseSupportFragment.FragmentFactory<Fragment>() {
        override fun createFragment(row: Any?): Fragment {
            val pageRow = row as PageRow
            val header = pageRow.headerItem
            if (header.id == SETTINGS_ID) return SettingsFragment()
            val streamerHeader = header as? StreamerHeaderItem
            val fragment = VideoGridFragment.newInstance(
                targetId = header.id.toInt(),
                isLive = streamerHeader?.isLive ?: false,
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

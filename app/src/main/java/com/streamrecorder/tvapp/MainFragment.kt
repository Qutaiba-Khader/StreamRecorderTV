package com.streamrecorder.tvapp

import android.os.Bundle
import android.util.Log
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        headersState = HEADERS_HIDDEN
        isHeadersTransitionOnBackEnabled = true

        val t = AppPreferences.currentTheme()
        brandColor = t.brand
        searchAffordanceColor = t.accent

        setHeaderPresenterSelector(object : PresenterSelector() {
            private val presenter = HeaderPresenter()
            override fun getPresenter(item: Any?): Presenter = presenter
        })

        mainFragmentRegistry.registerFragment(PageRow::class.java, GridFragmentFactory())
        loadTargets()
    }

    private fun loadTargets() {
        lifecycleScope.launch {
            try {
                val targets = ApiClient.loadTargets()
                val rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
                rowsAdapter.add(PageRow(HeaderItem(SETTINGS_ID, "Settings")))
                targets.forEach { t ->
                    val header = StreamerHeaderItem(t.id.toLong(), t.name, t.logo, t.isLive)
                    rowsAdapter.add(PageRow(header))
                }
                adapter = rowsAdapter
            } catch (e: Exception) {
                Log.e(TAG, "loadTargets failed", e)
            }
        }
    }

    inner class GridFragmentFactory : BrowseSupportFragment.FragmentFactory<Fragment>() {
        override fun createFragment(row: Any?): Fragment {
            val pageRow = row as PageRow
            val id = pageRow.headerItem.id
            if (id == SETTINGS_ID) return SettingsFragment()
            return VideoGridFragment.newInstance(id.toInt())
        }
    }
}

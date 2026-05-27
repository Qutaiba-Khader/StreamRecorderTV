package com.streamrecorder.tvapp

import android.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.os.Bundle
import android.widget.Toast
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.VerticalGridPresenter

class SettingsFragment : VerticalGridSupportFragment(),
    BrowseSupportFragment.MainFragmentAdapterProvider {

    private val mainAdapter = BrowseSupportFragment.MainFragmentAdapter(this)
    private val gridAdapter = ArrayObjectAdapter(SettingsCardPresenter())

    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> = mainAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val presenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_SMALL, false)
        presenter.numberOfColumns = 3
        gridPresenter = presenter
        adapter = gridAdapter

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            val setting = item as SettingsItem
            when (setting.key) {
                "player" -> showPicker("Default Player", AppPreferences.players, AppPreferences.defaultPlayer) {
                    AppPreferences.defaultPlayer = it
                }
                "resolution" -> showPicker("Preferred Resolution", AppPreferences.resolutions, AppPreferences.preferredResolution) {
                    AppPreferences.preferredResolution = it
                }
                "theme" -> showPicker("Theme", AppPreferences.themeNames, AppPreferences.theme) {
                    AppPreferences.theme = it
                    applyTheme()
                }
                "cardSize" -> showPicker("Card Size", AppPreferences.cardSizes, AppPreferences.cardSize) {
                    AppPreferences.cardSize = it
                    refreshSettings()
                    Toast.makeText(requireContext(), "Card size: $it — switch streamer to apply", Toast.LENGTH_SHORT).show()
                }
                "trackPosition" -> {
                    AppPreferences.trackPosition = !AppPreferences.trackPosition
                    refreshSettings()
                }
                "hdThumbnails" -> {
                    AppPreferences.hdThumbnails = !AppPreferences.hdThumbnails
                    refreshSettings()
                    pushSettings()
                }
                "showViewerCount" -> {
                    AppPreferences.showViewerCount = !AppPreferences.showViewerCount
                    refreshSettings()
                    pushSettings()
                }
                "showDeletionCountdown" -> {
                    AppPreferences.showDeletionCountdown = !AppPreferences.showDeletionCountdown
                    refreshSettings()
                    pushSettings()
                }
                "hiddenSources" -> {
                    Toast.makeText(requireContext(), "Manage hidden sources on the web app", Toast.LENGTH_SHORT).show()
                }
                "refresh" -> {
                    val mainFragment = requireActivity().supportFragmentManager
                        .findFragmentById(android.R.id.content) as? MainFragment
                    mainFragment?.refreshAll()
                    Toast.makeText(requireContext(), "Refreshing...", Toast.LENGTH_SHORT).show()
                }
            }
        }

        refreshSettings()
        mainAdapter.fragmentHost?.notifyDataReady(mainAdapter)
    }

    private fun refreshSettings() {
        val focusedPos = gridAdapter.size().let { size ->
            if (size == 0) -1
            else {
                val grid = view?.findViewById<androidx.leanback.widget.VerticalGridView>(
                    androidx.leanback.R.id.browse_grid
                )
                grid?.selectedPosition ?: -1
            }
        }

        gridAdapter.clear()
        gridAdapter.add(SettingsItem("player", "Default Player", AppPreferences.defaultPlayer))
        gridAdapter.add(SettingsItem("resolution", "Preferred Resolution", AppPreferences.preferredResolution))
        gridAdapter.add(SettingsItem("theme", "Theme", AppPreferences.theme))
        gridAdapter.add(SettingsItem("cardSize", "Card Size", AppPreferences.cardSize))
        gridAdapter.add(SettingsItem("trackPosition", "Track Watch Position", if (AppPreferences.trackPosition) "ON" else "OFF"))
        gridAdapter.add(SettingsItem("hdThumbnails", "HD Thumbnails", if (AppPreferences.hdThumbnails) "ON" else "OFF"))
        gridAdapter.add(SettingsItem("showViewerCount", "Viewer Count Badge", if (AppPreferences.showViewerCount) "ON" else "OFF"))
        gridAdapter.add(SettingsItem("showDeletionCountdown", "Deletion Countdown", if (AppPreferences.showDeletionCountdown) "ON" else "OFF"))
        gridAdapter.add(SettingsItem("hiddenSources", "Hidden Sources", "Web app only"))
        gridAdapter.add(SettingsItem("refresh", "Refresh Data", "Clear cache & reload"))

        if (focusedPos in 0 until gridAdapter.size()) {
            view?.findViewById<androidx.leanback.widget.VerticalGridView>(
                androidx.leanback.R.id.browse_grid
            )?.post {
                view?.findViewById<androidx.leanback.widget.VerticalGridView>(
                    androidx.leanback.R.id.browse_grid
                )?.selectedPosition = focusedPos
            }
        }
    }

    private fun showPicker(title: String, options: List<String>, current: String, onSelect: (String) -> Unit) {
        val currentIndex = options.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setSingleChoiceItems(options.toTypedArray(), currentIndex) { dialog, which ->
                onSelect(options[which])
                refreshSettings()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun pushSettings() {
        lifecycleScope.launch {
            ApiClient.saveSettings(AppPreferences.getFullSettings())
        }
    }

    private fun applyTheme() {
        val t = AppPreferences.currentTheme()
        requireActivity().window.decorView.setBackgroundColor(t.windowBg)
        val mainFragment = requireActivity().supportFragmentManager
            .findFragmentById(android.R.id.content) as? MainFragment
        mainFragment?.brandColor = t.brand
        mainFragment?.currentGridFragment?.let { grid ->
            grid.refreshData()
        }
    }
}

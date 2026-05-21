package com.streamrecorder.tvapp

import android.app.AlertDialog
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
                    Toast.makeText(requireContext(), "Track Position: ${if (AppPreferences.trackPosition) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
                }
                "hiddenSources" -> {
                    Toast.makeText(requireContext(), "Hidden sources manager coming soon", Toast.LENGTH_SHORT).show()
                }
                "refresh" -> {
                    ApiClient.clearCache()
                    Toast.makeText(requireContext(), "Cache cleared", Toast.LENGTH_SHORT).show()
                }
            }
        }

        refreshSettings()
        mainAdapter.fragmentHost?.notifyDataReady(mainAdapter)
    }

    private fun refreshSettings() {
        gridAdapter.clear()
        gridAdapter.add(SettingsItem("player", "Default Player", AppPreferences.defaultPlayer))
        gridAdapter.add(SettingsItem("resolution", "Preferred Resolution", AppPreferences.preferredResolution))
        gridAdapter.add(SettingsItem("theme", "Theme", AppPreferences.theme))
        gridAdapter.add(SettingsItem("cardSize", "Card Size", AppPreferences.cardSize))
        gridAdapter.add(SettingsItem("trackPosition", "Track Watch Position", if (AppPreferences.trackPosition) "ON" else "OFF"))
        gridAdapter.add(SettingsItem("hiddenSources", "Hidden Sources", "Coming soon"))
        gridAdapter.add(SettingsItem("refresh", "Refresh Data", "Clear cache & reload"))
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

    private fun applyTheme() {
        val t = AppPreferences.currentTheme()
        requireActivity().window.decorView.setBackgroundColor(t.windowBg)
        val mainFragment = requireActivity().supportFragmentManager
            .findFragmentById(android.R.id.content) as? MainFragment
        mainFragment?.brandColor = t.brand
        mainFragment?.searchAffordanceColor = t.accent
    }
}

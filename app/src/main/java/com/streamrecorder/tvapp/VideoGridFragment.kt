package com.streamrecorder.tvapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.app.VerticalGridSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.FocusHighlight
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.leanback.widget.VerticalGridPresenter
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class VideoGridFragment : VerticalGridSupportFragment(),
    BrowseSupportFragment.MainFragmentAdapterProvider {

    private val mainAdapter = BrowseSupportFragment.MainFragmentAdapter(this)
    private val gridAdapter = ArrayObjectAdapter(CardPresenter())

    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> = mainAdapter

    companion object {
        private const val TAG = "StreamRecGrid"
        fun newInstance(targetId: Int) = VideoGridFragment().apply {
            arguments = Bundle().apply { putInt("targetId", targetId) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val presenter = VerticalGridPresenter(FocusHighlight.ZOOM_FACTOR_SMALL)
        presenter.numberOfColumns = 3
        gridPresenter = presenter
        adapter = gridAdapter

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            val rec = item as Recording
            val source = AppPreferences.resolveResolution(rec.sources)
            source?.let { src ->
                val url = ApiClient.playUrl(rec.id, src.resolution)
                val playerPkg = AppPreferences.resolvePlayerPackage()
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(url), "video/*")
                    if (playerPkg.isNotEmpty()) setPackage(playerPkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    requireContext().startActivity(intent)
                } catch (_: Exception) {
                    requireContext().startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
        loadRecordings()
    }

    private fun loadRecordings() {
        val targetId = arguments?.getInt("targetId") ?: return
        lifecycleScope.launch {
            try {
                val recordings = ApiClient.loadRecordings(targetId)
                gridAdapter.addAll(0, recordings)
            } catch (e: Exception) {
                Log.e(TAG, "loadRecordings($targetId) failed", e)
            }
            mainAdapter.fragmentHost?.notifyDataReady(mainAdapter)
        }
    }
}

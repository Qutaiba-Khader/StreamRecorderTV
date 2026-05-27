package com.streamrecorder.tvapp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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
        }
        header.addView(titleText)
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
        adapter = ArrayObjectAdapter(ListRowPresenter())

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            val file = item as? RecoFile ?: return@OnItemViewClickedListener
            playRecoFile(file)
        }

        loadReco()
    }

    private fun playRecoFile(file: RecoFile) {
        if (!isAdded) return
        val url = ApiClient.recoPlayUrl(file.user, file.filename)
        val playerPkg = AppPreferences.resolvePlayerPackage()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            if (playerPkg.isNotEmpty()) setPackage(playerPkg)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {}
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

        items.add("🗑  Delete")
        actions.add { confirmDelete(file) }

        val title = file.date ?: file.filename
        AlertDialog.Builder(ctx)
            .setTitle(title)
            .setItems(items.toTypedArray()) { _, which -> actions[which]() }
            .setNegativeButton("Cancel", null)
            .show()
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
                val rowsAdapter = adapter as ArrayObjectAdapter
                rowsAdapter.clear()

                if (reco.isEmpty()) {
                    statusText?.text = "No downloaded recordings"
                    statusText?.visibility = View.VISIBLE
                    mainAdapter.fragmentHost?.notifyDataReady(mainAdapter)
                    return@launch
                }

                statusText?.visibility = View.GONE

                for ((username, files) in reco) {
                    val headerItem = HeaderItem(username)
                    val cardAdapter = ArrayObjectAdapter(RecoCardPresenter { file -> showRecoPopup(file) })
                    for (file in files) {
                        cardAdapter.add(file)
                    }
                    rowsAdapter.add(ListRow(headerItem, cardAdapter))
                }

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

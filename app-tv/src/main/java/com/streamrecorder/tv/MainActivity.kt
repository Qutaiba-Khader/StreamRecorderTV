package com.streamrecorder.tv

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.streamrecorder.core.model.Recording
import com.streamrecorder.core.model.Target
import com.streamrecorder.tv.ui.MainViewModel
import com.streamrecorder.tv.ui.RecordingsGridAdapter
import com.streamrecorder.tv.ui.StreamerListAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var streamerAdapter: StreamerListAdapter
    private lateinit var recordingsAdapter: RecordingsGridAdapter

    // Views
    private lateinit var splashOverlay: View
    private lateinit var progressBar: ProgressBar
    private lateinit var streamerList: RecyclerView
    private lateinit var recordingsGrid: RecyclerView
    private lateinit var contentHeader: LinearLayout
    private lateinit var headerAvatar: ImageView
    private lateinit var headerTitle: TextView
    private lateinit var headerMeta: TextView
    private lateinit var emptyState: TextView
    private lateinit var errorState: LinearLayout
    private lateinit var errorText: TextView
    private lateinit var gridLoading: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val app = application as StreamRecorderApp
        viewModel = ViewModelProvider(this, MainViewModel.Factory(app.container))
            .get(MainViewModel::class.java)

        bindViews()
        setupAdapters()
        observeState()
        setupButtons()

        viewModel.initialize()
    }

    private fun bindViews() {
        splashOverlay = findViewById(R.id.splashOverlay)
        progressBar = findViewById(R.id.progressBar)
        streamerList = findViewById(R.id.streamerList)
        recordingsGrid = findViewById(R.id.recordingsGrid)
        contentHeader = findViewById(R.id.contentHeader)
        headerAvatar = findViewById(R.id.headerAvatar)
        headerTitle = findViewById(R.id.headerTitle)
        headerMeta = findViewById(R.id.headerMeta)
        emptyState = findViewById(R.id.emptyState)
        errorState = findViewById(R.id.errorState)
        errorText = findViewById(R.id.errorText)
        gridLoading = findViewById(R.id.gridLoading)
    }

    private fun setupAdapters() {
        streamerAdapter = StreamerListAdapter { target ->
            viewModel.selectStreamer(target)
        }
        streamerList.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = streamerAdapter
        }

        recordingsAdapter = RecordingsGridAdapter(
            onRecordingClick = { recording -> viewModel.playRecording(this, recording) },
            onRecordingLongClick = { recording -> viewModel.onRecordingLongPress(recording) },
        )
        recordingsGrid.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 4)
            adapter = recordingsAdapter
        }
    }

    private fun observeState() {
        viewModel.uiState.observe(this) { state ->
            when (state) {
                is MainViewModel.UiState.Loading -> showLoading(state.hasCachedData)
                is MainViewModel.UiState.Ready -> showReady(state)
                is MainViewModel.UiState.Error -> showError(state.message)
            }
        }

        viewModel.targets.observe(this) { targets ->
            streamerAdapter.submitList(targets)
        }

        viewModel.recordings.observe(this) { recordings ->
            if (recordings != null) {
                showRecordings(recordings)
            }
        }

        viewModel.selectedTarget.observe(this) { target ->
            if (target != null) {
                showContentHeader(target)
            } else {
                hideContentHeader()
            }
        }

        viewModel.isRefreshing.observe(this) { refreshing ->
            progressBar.visibility = if (refreshing) View.VISIBLE else View.GONE
        }

        viewModel.toastMessage.observe(this) { msg ->
            if (msg != null) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupButtons() {
        findViewById<ImageButton>(R.id.btnRefresh).setOnClickListener {
            viewModel.refresh()
        }
        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            // TODO: open settings
        }
        findViewById<View>(R.id.btnRetry).setOnClickListener {
            viewModel.initialize()
        }
    }

    private fun showLoading(hasCached: Boolean) {
        if (hasCached) {
            splashOverlay.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
        } else {
            splashOverlay.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
        errorState.visibility = View.GONE
    }

    private fun showReady(state: MainViewModel.UiState.Ready) {
        splashOverlay.visibility = View.GONE
        errorState.visibility = View.GONE
        if (state.selectedTarget == null) {
            emptyState.visibility = View.VISIBLE
            recordingsGrid.visibility = View.GONE
            gridLoading.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        splashOverlay.visibility = View.GONE
        progressBar.visibility = View.GONE
        errorState.visibility = View.VISIBLE
        errorText.text = message
        emptyState.visibility = View.GONE
    }

    private fun showContentHeader(target: Target) {
        contentHeader.visibility = View.VISIBLE
        headerTitle.text = target.name
        headerMeta.text = "${target.platform} · ${target.countTotal} recordings"
        Glide.with(this)
            .load(target.avatarUrl)
            .placeholder(R.drawable.avatar_placeholder)
            .circleCrop()
            .into(headerAvatar)
    }

    private fun hideContentHeader() {
        contentHeader.visibility = View.GONE
        recordingsGrid.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        gridLoading.visibility = View.GONE
    }

    private fun showRecordings(recordings: List<Recording>) {
        emptyState.visibility = View.GONE
        gridLoading.visibility = View.GONE
        if (recordings.isEmpty()) {
            recordingsGrid.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
            emptyState.text = getString(R.string.no_recordings)
        } else {
            recordingsGrid.visibility = View.VISIBLE
            recordingsAdapter.submitList(recordings)
        }
    }

    override fun onBackPressed() {
        if (viewModel.selectedTarget.value != null) {
            viewModel.deselectStreamer()
            streamerAdapter.clearSelection()
        } else {
            super.onBackPressed()
        }
    }
}

# CLAUDE.md — StreamRecorder TV

## Project Overview

Native Android TV Leanback app for StreamRecorder — a TikTok recording manager. Companion to the Web SPA (CT 112, port 5001) and Mobile phone app (repo `streamrecorder-home-mobile`).

- **Package**: `com.streamrecorder.tvapp`
- **Current version**: 4.0.3 (versionCode 8)

## Tech Stack

- **Language**: Kotlin 1.9.24
- **UI**: AndroidX Leanback 1.0.0 (BrowseSupportFragment + VerticalGridSupportFragment + RowsSupportFragment)
- **Images**: Glide 4.16.0
- **Network**: OkHttp 4.12.0 + org.json (no Retrofit/Gson)
- **TV Channels**: AndroidX TVProvider 1.1.0-alpha01 (PreviewChannel, PreviewProgram)
- **Build**: AGP 8.7.3, compileSdk 35, minSdk 28, targetSdk 35, JVM 17
- **No internal player** — launches external players (MPV, MX Player, VLC) via intent

## Build & Deploy

### Build on CT 200 (code-cc, 192.168.1.243)
```bash
# Source lives at /root/StreamRecorderTV.old/ on CT 200
# SDK at /opt/android-sdk (local.properties: sdk.dir=/opt/android-sdk)
pct exec 200 -- bash -c 'cd /root/StreamRecorderTV.old && ./gradlew assembleDebug'
pct exec 200 -- bash -c 'cd /root/StreamRecorderTV.old && ./gradlew assembleRelease'
```

### Deploy APK
```bash
pct exec 200 -- cp /root/StreamRecorderTV.old/app/build/outputs/apk/release/app-release.apk \
  /mnt/Shared1Tb/public/tvapp/StreamRecorder-TV-v4.0.3.apk
```

### Install on Google TV Streamer (192.168.1.222) via CT 126
```bash
pct pull 200 /root/StreamRecorderTV.old/app/build/outputs/apk/debug/app-debug.apk /tmp/tv.apk
pct push 126 /tmp/tv.apk /tmp/tv.apk
pct exec 126 -- adb -s 192.168.1.222:5555 install -r /tmp/tv.apk
```

### GitHub Release
```bash
pct exec 200 -- gh release create v4.0.3 /mnt/Shared1Tb/public/tvapp/StreamRecorder-TV-v4.0.3.apk \
  --repo Qutaiba-Khader/StreamRecorderTV --title "StreamRecorder TV v4.0.3" --notes "Release notes"
```

## Architecture

```
app/src/main/java/com/streamrecorder/tvapp/
├── MainActivity.kt          # Entry point, init preferences + theme window bg
├── MainFragment.kt          # BrowseSupportFragment, sidebar + PageRow factory
├── VideoGridFragment.kt     # 3-col grid, header, fav filter, live/processing cards, player launch, download
├── SettingsFragment.kt      # 3-col settings grid, AlertDialog pickers
├── HiddenFragment.kt        # Hidden sources grid, click to unhide
├── RecoFragment.kt          # Downloads page — rows per user, play/fav/progress/delete, in-place card updates
├── RecoCardPresenter.kt     # Download card with thumbnail, date, resolution, size
├── ChannelHelper.kt         # TvProvider "Live Now" preview channel management
├── CardPresenter.kt         # Card views: recordings, live (spinner), post-processing (yellow)
├── SettingsCardPresenter.kt # Settings card, theme-aware focus colors
├── HeaderPresenter.kt       # Sidebar: avatar, live 🔴 dot, processing 🟡 dot, pin 📌, hidden count, downloads
├── StreamerHeaderItem.kt    # HeaderItem + logoUrl + isLive + isPostprocessing + isPinned + platform + count
├── Models.kt                # Target, Recording, LiveStreamCard, PostProcessingCard, HiddenSource, RecoFile
├── AppPreferences.kt        # SharedPreferences + ThemeColors + card sizing + watch position + hidden + pinned users
├── SettingsItem.kt          # Settings data model
└── ApiClient.kt             # OkHttp, local/remote failback, download/reco/delete endpoints
```

## API

Backend is the StreamRecorder SPA server on CT 112 (192.168.1.31:5001).

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `/api/targets` | GET | List all streamers (includes `is_live`, `is_postprocessing`) |
| `/api/recordings/{id}` | GET | Recordings for a target |
| `/api/live-stream/{slug}` | GET | Live stream URLs |
| `/api/fav` | POST | Toggle favorite |
| `/api/hide` | POST | Hide recording |
| `/api/unhide` | POST | Unhide recording |
| `/api/watch-position` | POST | Save watch position |
| `/api/settings` | GET/POST | Sync settings (features + pinned users) |
| `/api/download` | POST | Start server-side download via aria2c |
| `/api/download/check` | POST | Check download status |
| `/api/reco` | GET | List all downloaded files grouped by user |
| `/api/reco/delete` | POST | Delete a downloaded file |
| `/api/reco/fav` | POST | Toggle favorite on downloaded file |
| `/api/reco/watch-position` | POST | Save watch position for downloaded file |
| `/api/reco/watch-position/delete` | POST | Clear watch position for downloaded file |
| `/reco/thumb/{user}/{file}` | GET | Thumbnail for downloaded file (generated via ffmpeg, cached) |
| `/reco/play/{user}/{file}` | GET | Stream downloaded file (HTTP range support) |
| `/play/{id}?res={res}` | GET | 301 redirect to CDN |

Failover: tries local `192.168.1.31:5001` first (2s timeout), falls back to `strm-rec-h.websnake.org`.

## Key Behaviors

- **Pin feature**: Long-click sidebar streamer → popup with Refresh/Pin. Pinned users sort above live users with 📌 prefix. Synced to server via settings API
- **Downloads page**: "📂 Downloads" sidebar section with horizontal rows per user. Cards show ffmpeg-generated thumbnails, fav badge, watch progress bar. Click to play, long-click for play/open-with/fav/clear-progress/delete. In-place card updates (no focus loss on player return)
- **Live Now channel**: TvProvider preview channel showing live streamers on launcher home screen
- **Download to SMB**: Long-click recording card → download per resolution. Server-side aria2c downloads to `/mnt/Reco/@username/`. Status check on download click (not on popup open, for speed)
- **Post-processing**: Yellow 🟡 dot in sidebar, yellow non-clickable PostProcessingCard in grid
- **Live card loading**: Shows circular ProgressBar spinner while stream data loads, becomes clickable when ready
- **Sort priority**: pinned > live > processing > offline
- **Recordings skip**: `status == "running"` and `status == "postprocessing"` entries excluded from normal list
- **CDN avatar optimization**: Replace `250x250` with `56x56` in logo URLs
- **ViewHolder recycling**: Must reset all visual state in onBindViewHolder
- **Lifecycle safety**: All coroutine callbacks guard with `isAdded` before `requireContext()` to prevent crashes on fragment detachment
- **Settings sync**: JSONObject format with feature booleans + pinnedUsers array, synced on app start

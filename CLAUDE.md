# CLAUDE.md — StreamRecorder TV

## Project Overview

Native Android TV Leanback app for StreamRecorder — a TikTok recording manager. Companion to the Web SPA (CT 112, port 5001) and Mobile phone app (repo `streamrecorder-home-mobile`).

- **Package**: `com.streamrecorder.tvapp`
- **Current version**: 3.2.0 (versionCode 4)

## Tech Stack

- **Language**: Kotlin 1.9.24
- **UI**: AndroidX Leanback 1.0.0 (BrowseSupportFragment + VerticalGridSupportFragment)
- **Images**: Glide 4.16.0
- **Network**: OkHttp 4.12.0 + org.json (no Retrofit/Gson)
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

### Sync files from Proxmox host to CT 200
```bash
cat local/path/File.kt | pct exec 200 -- tee /root/StreamRecorderTV.old/app/src/main/java/com/streamrecorder/tvapp/File.kt > /dev/null
```
For binary files (icons, images), use Shared1Tb mount as intermediary — `cat | pct exec tee` corrupts binaries.

### Deploy APK
```bash
pct exec 200 -- cp /root/StreamRecorderTV.old/app/build/outputs/apk/release/app-release.apk \
  /mnt/Shared1Tb/public/tvapp/StreamRecorder-TV-v3.2.0.apk
```

### Install on Google TV Streamer (192.168.1.222) via CT 126
```bash
cat /mnt/pve/Shared1Tb/public/tvapp/StreamRecorder-TV-v3.2.0.apk | pct exec 126 -- tee /tmp/tv.apk > /dev/null
pct exec 126 -- adb connect 192.168.1.222:5555
pct exec 126 -- adb -s 192.168.1.222:5555 install -r /tmp/tv.apk
```

### GitHub Release
```bash
pct exec 200 -- gh release create v3.2.0 /mnt/Shared1Tb/public/tvapp/StreamRecorder-TV-v3.2.0.apk \
  --repo Qutaiba-Khader/StreamRecorderTV --title "StreamRecorder TV v3.2.0" --notes "Release notes"
```

## Architecture

```
app/src/main/java/com/streamrecorder/tvapp/
├── MainActivity.kt          # Entry point, init preferences + theme window bg
├── MainFragment.kt          # BrowseSupportFragment, sidebar + PageRow factory
├── VideoGridFragment.kt     # 3-col grid, header, fav filter, live/processing cards, player launch
├── SettingsFragment.kt      # 3-col settings grid, AlertDialog pickers
├── HiddenFragment.kt        # Hidden sources grid, click to unhide
├── CardPresenter.kt         # Card views: recordings, live (spinner), post-processing (yellow)
├── SettingsCardPresenter.kt # Settings card, theme-aware focus colors
├── HeaderPresenter.kt       # Sidebar: avatar, live 🔴 dot, processing 🟡 dot, hidden count
├── StreamerHeaderItem.kt    # HeaderItem + logoUrl + isLive + isPostprocessing + platform + count
├── Models.kt                # Target, Recording, LiveStreamCard, PostProcessingCard, HiddenSource
├── AppPreferences.kt        # SharedPreferences + ThemeColors + card sizing + watch position + hidden
├── SettingsItem.kt          # Settings data model
└── ApiClient.kt             # OkHttp, local/remote failback, parses is_live + is_postprocessing
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
| `/play/{id}?res={res}` | GET | 301 redirect to CDN |

Failover: tries local `192.168.1.31:5001` first (2s timeout), falls back to `strm-rec-h.websnake.org`.

## Key Behaviors

- **Post-processing**: Yellow 🟡 dot in sidebar, yellow non-clickable PostProcessingCard in grid
- **Live card loading**: Shows circular ProgressBar spinner while stream data loads, becomes clickable when ready
- **Live card replacement**: Finds card by type scan (`is LiveStreamCard`), not by index
- **Sort priority**: live > processing > offline
- **Recordings skip**: `status == "running"` and `status == "postprocessing"` entries excluded from normal list
- **CDN avatar optimization**: Replace `250x250` with `56x56` in logo URLs (250x250 returns SVG)
- **ViewHolder recycling**: Must reset all visual state in onBindViewHolder (text color, Glide images, stroke)

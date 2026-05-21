# StreamRecorder TV

Native Android TV app for browsing and playing StreamRecorder recordings. Built with Leanback library (same framework as SmartTube).

## Features

- **Sidebar navigation** — streamer list with avatars, live indicators
- **Video grid** — 3-column grid with thumbnails, resolution info, duration
- **Settings page** — default player, preferred resolution, themes, track position
- **10 themes** — Glass Dark, Midnight Blue, AMOLED Black, Deep Purple, Ocean, Charcoal, Warm Dark, Light, Light Blue, Light Warm
- **Multi-player support** — MPV, MX Player, VLC, Just Player, System Picker
- **Resolution picker** — Max, 1080p, 720p, 480p preference
- **Dynamic card sizing** — cards fill the screen width automatically
- **Recording cache** — in-memory cache per streamer for instant switching

## Architecture

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed component documentation.

## Project Structure

```
app/src/main/java/com/streamrecorder/tvapp/
├── MainActivity.kt          # FragmentActivity shell, init preferences + theme
├── MainFragment.kt          # BrowseSupportFragment — sidebar + content area
├── VideoGridFragment.kt     # VerticalGridSupportFragment — recording cards grid
├── SettingsFragment.kt      # VerticalGridSupportFragment — settings cards grid
├── CardPresenter.kt         # ImageCardView presenter — dynamic 16:9 cards
├── SettingsCardPresenter.kt # Custom settings card presenter — theme-aware
├── HeaderPresenter.kt       # Sidebar header presenter — avatar + name
├── StreamerHeaderItem.kt    # HeaderItem with logo URL and live status
├── Models.kt                # Target, Source, Recording data classes
├── AppPreferences.kt        # SharedPreferences + theme definitions
├── SettingsItem.kt          # Settings data model
└── ApiClient.kt             # OkHttp API client with cache
```

## API

Connects to StreamRecorder v1 API at `http://192.168.1.31:5001`:

| Endpoint | Purpose |
|----------|---------|
| `GET /api/targets` | List all streamers |
| `GET /api/recordings/{id}` | Recordings for a streamer |
| `GET /play/{id}?res={res}` | 301 redirect to video CDN |

## Build

```bash
cd StreamRecorderTV
ANDROID_HOME=/opt/android-sdk ./gradlew assembleRelease
```

APK output: `app/build/outputs/apk/release/app-release.apk`

## Requirements

- Android TV (minSdk 28, targetSdk 35)
- Leanback launcher
- Network access to StreamRecorder API

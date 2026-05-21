# StreamRecorder TV — Architecture

## Leanback Framework

The app uses Android's Leanback library (`androidx.leanback:leanback:1.0.0`), the same framework used by SmartTube. Leanback provides TV-optimized UI components with built-in d-pad navigation, focus handling, and entrance transitions.

## Component Overview

### BrowseSupportFragment (MainFragment)

The root UI component. Provides two areas:
- **Headers dock** (left sidebar) — `VerticalGridView` of streamer entries
- **Content area** (right) — swappable fragment per selected sidebar item

Sidebar slides via margin manipulation, not a drawer:
- **Show sidebar**: headers container `marginStart = 0`, content pushed right
- **Hide sidebar**: headers container slides off-screen with negative margin, content fills width

Fragment swap happens on **selection** (d-pad focus change on sidebar), not on click. Click only collapses the sidebar.

### PageRow + MainFragmentAdapterProvider

Each sidebar item is a `PageRow` with a `HeaderItem`. When a PageRow is selected, `GridFragmentFactory.createFragment()` creates the appropriate content fragment.

Content fragments **must** implement `MainFragmentAdapterProvider` and call `notifyDataReady()` after loading data. Without this call, BrowseSupportFragment shows a spinner forever.

### VideoGridFragment

Extends `VerticalGridSupportFragment`. Displays recordings in a 3-column grid.

- Implements `MainFragmentAdapterProvider` for PageRow integration
- Loads recordings via `ApiClient.loadRecordings(targetId)`
- Calls `notifyDataReady()` after data loads
- Click handler opens video in the configured player app
- Uses `AppPreferences.resolveResolution()` to pick the preferred source
- Uses `AppPreferences.resolvePlayerPackage()` for the player intent

### SettingsFragment

Same base as VideoGridFragment (VerticalGridSupportFragment + MainFragmentAdapterProvider) but with 3-column grid of settings cards.

Settings items:
1. **Default Player** — picker dialog (Any/MPV/MX Player/VLC/Just Player)
2. **Preferred Resolution** — picker dialog (Max/1080p/720p/480p)
3. **Theme** — picker dialog (10 themes)
4. **Track Watch Position** — toggle (ON/OFF)
5. **Hidden Sources** — placeholder (coming soon)
6. **Refresh Data** — clears recording cache

Picker dialogs use `AlertDialog.setSingleChoiceItems()` for TV-friendly selection.

### CardPresenter

Renders recording cards using Leanback's `ImageCardView`.

Card width is calculated dynamically from screen width:
```
cardWidth = screenWidth * 0.85 / 3
cardHeight = cardWidth * 9 / 16
```

This ensures cards fill the available width regardless of screen resolution or density.

Displays: thumbnail (Glide), date, resolutions, file size, duration.

### SettingsCardPresenter

Custom programmatic card view (no XML layout). Theme-aware colors:
- Unfocused: theme's `cardBg` background, `textPrimary`/`textSecondary` text
- Focused: theme's `accent` background, dark text

Cards are sized dynamically (same width as video cards, fixed 80dp height).

### HeaderPresenter

Custom `RowHeaderPresenter` for sidebar entries. Programmatic layout:
- `ImageView` (36dp circle) + `TextView` (14sp)
- Streamers: Glide circular avatar + name (with live indicator)
- Settings: accent-colored dot + gear emoji + name
- Alpha: 0.5 unselected → 1.0 selected (interpolated by `selectLevel`)

### ApiClient

Singleton OkHttp client connecting to `http://192.168.1.31:5001`.

- `loadTargets()` — fetches all streamers, sorted by live-first then latest timestamp
- `loadRecordings(targetId)` — fetches recordings, cached in `ConcurrentHashMap`
- `playUrl(recId, res)` — constructs play URL (server 301-redirects to CDN)
- `clearCache()` — clears recording cache (used by Settings refresh)

### AppPreferences

SharedPreferences wrapper with theme system.

Stores: `player`, `resolution`, `trackPosition`, `theme`

10 themes defined as `ThemeColors(brand, accent, cardBg, windowBg, textPrimary, textSecondary)`:
- Dark themes: Glass Dark, Midnight Blue, AMOLED Black, Deep Purple, Ocean, Charcoal, Warm Dark
- Light themes: Light, Light Blue, Light Warm

Theme is applied to: sidebar brand color, focus accent color, card backgrounds, window background, text colors.

## Data Flow

```
App Launch
  → MainActivity.onCreate() → init preferences, apply theme window bg
  → MainFragment.onCreate() → apply theme brand/accent, load targets
  → API: GET /api/targets → sidebar populated with PageRows
  → First item auto-selected (Settings) → SettingsFragment created
  → Settings cards rendered immediately (no async data)

User selects streamer in sidebar
  → PageRow selection → GridFragmentFactory.createFragment(targetId)
  → VideoGridFragment.onCreate() → loadRecordings(targetId)
  → API: GET /api/recordings/{id} → grid populated with cards
  → notifyDataReady() → grid appears

User clicks recording card
  → AppPreferences.resolveResolution() → pick best source
  → Intent(ACTION_VIEW) with player package → opens video
  → Server 301-redirects to CDN URL
```

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| `androidx.leanback:leanback` | 1.0.0 | TV UI framework |
| `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.0 | Coroutine lifecycle scope |
| `com.github.bumptech.glide:glide` | 4.16.0 | Image loading |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP client |
| `org.json:json` | 20240303 | JSON parsing |

# 🎵 FMusic - Kotlin Android App (v1.0.01)

<div align="center">
  <img src="pict/logo.png" width="120" height="120" alt="FMusic Logo" style="border-radius: 24px;" />
  <h3>Feel the Rhythm, Everywhere</h3>
  <p>A native Android music streaming application inspired by Spotify with a Deep Midnight Blue & Electric Neon theme.</p>
</div>

---

## 🌟 Features

- 🌌 **Midnight Blue & Electric Neon Aesthetic**: Replaces traditional green with electric cyan/neon blue progress bars, sleek dark gradients, and smooth micro-animations.
- 🚀 **Professional BootScreen / Splash**: Animated glowing FMusic branding with version `1.0.01` badge.
- ⚡ **Skeleton Shimmer Loading**: Polished shimmer effect while fetching online music cards and streams.
- 🏠 **Homepage**:
  - Mix Indo Music & Trending shelves
  - Horizontal carousels & quick picks
  - Dynamic artist & album discovery
- 🔍 **Spotify-Styled Search**:
  - Multi-category search filters (`Songs`, `Videos`, `Albums`, `Artists`, `Playlists`)
  - Instant live search suggestions
  - Recent searches history tags with individual delete & clear all
  - Recently played songs with quick play
  - Vibrant "Browse All" category mood cards (from `/api/moods`)
- 📊 **Charts & Tangga Lagu**:
  - Tangga Lagu Teratas (Top Tracks with numbered ranking)
  - Top Music Videos & Top Artists
  - Genre breakdowns
- 📚 **Your Library**:
  - Tab filters: `Playlists`, `Favorites`, `Saved (Offline)`, `History`, `Stats`
  - `+ New Playlist` creation with local database persistence
  - `Import from YT Music`, `Backup`, and `Restore` actions
  - Offline downloaded songs management
- 🎶 **Spotify-Style Music Player (Mini & Full Player)**:
  - Floating persistent Mini Player with Neon Blue progress bar
  - Full Player Modal with large album art, explicit `E` badge, love/favorite toggle, and playlist add button
  - **Neon Blue Seekbar** & live duration tracking
  - **Sleep Timer (Pengatur Waktu Tidur)**: Configurable from 5 minutes to 30 minutes with live countdown and auto-pause
  - **Share Button**: Share currently playing song title and artist
  - **Synchronized Lyrics (Pratinjau Lirik)**: Real-time karaoke scrolling lyrics with glowing active line + plain lyrics fallback
  - **About Artist (Tentang Artis)**: Artist thumbnail, bio, and profile navigation
  - **Offline Storage**: Save and download MP3 music locally to device storage for offline playback
- 🛰️ **Background Playback**: AndroidX Media3 (ExoPlayer) foreground service with lockscreen and notification media controls.

---

## 🏗️ Architecture & Tech Stack

- **Language**: Kotlin 1.9.22
- **UI Framework**: Jetpack Compose + Material 3
- **Media Engine**: AndroidX Media3 / ExoPlayer
- **Network**: Retrofit 2 + OkHttp 3 + Gson
- **Image Loading**: Coil Compose with memory & disk caching
- **Local Persistence**: Room Database 2.6 (Search History, Recently Played, Favorites, Playlists, Offline Music)
- **Settings**: AndroidX Preferences DataStore
- **CI/CD**: GitHub Actions APK Build & Automated Release Workflow

---

## 📡 API Integration (`API/server.js`)

The app interfaces with the included Node.js YouTube Music proxy backend:

| Endpoint | Description |
|---|---|
| `GET /api/home` | Home shelves, Indonesian mixes, and trending cards |
| `GET /api/charts` | Top charts, music videos, and artists |
| `GET /api/moods` | Category cards with hex colors for Search |
| `GET /api/search` | Search queries with item filters |
| `GET /api/suggest` | Live query auto-complete suggestions |
| `GET /api/browse` | Album details, playlist details, and artist profiles |
| `GET /api/next` | Queue generation and related tracks |
| `GET /api/lyrics` | Synced LRCLIB / NetEase / YouTube lyrics |
| `GET /api/download-start` | Starts MP3 conversion for offline caching |
| `GET /api/download-progress` | Polls conversion progress and downloads MP3 |
| `GET /api/sponsorblock` | Non-music / sponsor segment skipping |

### Setting the Server URL in App
You can change the API server URL anytime directly inside the app:
1. Tap the **Settings** icon on the top right of the Home screen.
2. Enter your backend URL (e.g. `http://10.0.2.2:3000` for emulator or `http://192.168.1.x:3000` or `https://your-server.vercel.app`).
3. Tap **Simpan**.

---

## 🚀 Building the APK via GitHub Actions

This repository includes a preconfigured GitHub Actions workflow in [`.github/workflows/build.yml`](.github/workflows/build.yml).

### Steps to build on GitHub:
1. Push this repository to GitHub:
   ```bash
   git add .
   git commit -m "Initial commit - FMusic v1.0.01"
   git branch -M main
   git push -u origin main
   ```
2. GitHub Actions will automatically:
   - Compile Kotlin code and resources using JDK 17
   - Execute `./gradlew assembleDebug`
   - Upload the generated APK artifact `FMusic-v1.0.01.apk`
3. Download your APK directly from the **Actions** tab on your GitHub repository:
   👉 **https://github.com/VidArr212/FMusic-APK/actions**

---

## 💻 Local Build Instructions

```bash
# Clone the repository
git clone https://github.com/VidArr212/FMusic-APK.git
cd FMusic-APK

# Start the API backend
cd API
npm install
node server.js

# Build Android APK locally (in root directory)
./gradlew assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📄 License
Version 1.0.01 - Developed for FMusic.

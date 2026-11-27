# ARTFRAME — Android TV

ARTFRAME turns an Android TV or Google TV device into a full-screen Wallhaven art display.

## Features

- Search Wallhaven by user-entered terms.
- SFW, Sketchy and NSFW purity filters.
- Wallhaven API key stored using AndroidX encrypted shared preferences.
- Full original image URLs (not thumbnails).
- Strict 16:9 filtering with a 0.025 ratio tolerance.
- Minimum source resolution of 1920×1080.
- Original image preloading and a 2 GB disk cache.
- Configurable display duration (10 seconds to 60 minutes).
- Android TV remote navigation and immersive full-screen display.

## Wallhaven API key

1. Sign in to your Wallhaven account.
2. Create an API key at https://wallhaven.cc/settings/account.
3. Launch ARTFRAME and enter the key in the API Key field.
4. Select the desired content categories and enter a search term.

The app sends the key in the `X-API-Key` request header. It does not ask for or store your Wallhaven password. NSFW results depend on Wallhaven account permissions and API access.

## Build

Requirements: Android Studio with Android SDK 36 and JDK 17.

Open this repository in Android Studio, allow Gradle sync, then build:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

The APK is produced at:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

Release APKs must be signed with your own release keystore before distribution.

## GitHub Actions

The repository includes a workflow that builds debug and release APKs on demand and uploads the APKs as workflow artifacts.

## Display behaviour

The display is fixed to landscape 16:9. Wallhaven search requests use the 16:9 ratio filter and the client additionally rejects results outside a 0.025 ratio tolerance or below 1920×1080. The image is rendered with Fit, preserving its original proportions without distortion.

## Remote controls

- Left / Right: previous / next wallpaper.
- OK / Enter: toggle image information.
- Back: return to settings.
- Menu / Settings: open settings.

## Notes

Wallhaven controls access to its API and content. Search results can be empty when the selected purity categories, query, or minimum resolution are restrictive. Try a broader search term or adjust category selection. The application does not bypass Wallhaven authentication or content permissions.

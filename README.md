# ARTFRAME — Android TV

A full-screen digital art display for Android TV and Google TV, powered by the [Wallhaven API](https://wallhaven.cc/help/api).

## Features

- Search Wallhaven by any user-entered term.
- Choose SFW, Sketchy and/or NSFW content categories.

- Authenticate API requests with a Wallhaven API key.
- Retrieve original image files, not thumbnail previews.
- Fixed 16:9 presentation with aspect-ratio filtering; images are never stretched.
- Rank wallpapers by similarity to the TV's physical display resolution.
- Prefer 4K sources on 4K displays, with a 1080p fallback.
- Preload the next image and cache originals locally (up to 2 GB).
- Configurable display interval from 10 seconds to 60 minutes.
- Smooth crossfade transitions and Android TV remote navigation.
- API key encrypted at rest with AndroidX Security Crypto.

## Wallhaven setup

1. Sign in to your Wallhaven account.
2. Create an API key at https://wallhaven.cc/settings/account.
3. Launch ARTFRAME and paste the API key into the settings screen.
4. Enter a search term and select the desired content categories.
5. Choose how many seconds each wallpaper should remain on screen, then select **Start Display**.

ARTFRAME uses the API key in the `X-API-Key` HTTP header. It does not request or store your Wallhaven password. NSFW results remain subject to Wallhaven account permissions and API policy.

## Image matching

ARTFRAME requests 16:9 results and applies a second client-side filter with a 0.025 aspect-ratio tolerance. It rejects images smaller than 1920×1080. On higher-resolution displays it prefers images close to the TV's physical resolution; if no matching 4K results are returned, it retries at 1920×1080.

Images are displayed using a fit scale mode, preserving their original proportions without distortion or cropping.


<img width="1672" height="941" alt="ARTFRAME Android TV Setup Storyboard" src="https://github.com/user-attachments/assets/b399ca6d-4fe6-44ac-96ec-8c1f4547457b" />

## Remote controls

- **Left / Right:** previous / next wallpaper.
- **OK / Enter:** show or hide image details.
- **Back:** return to settings.
- **Menu / Settings:** open settings.

- Package: `cc.wallhaven.artframe`
- Minimum Android version: Android 6.0 (API 23)
- Target SDK: 35
- Compile SDK: 36

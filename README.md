# Snack Shelf

A native Android (Kotlin + Jetpack Compose) snack counter POS and inventory app, built for a tablet on the snack shelf.

- **Shelf:** every snack on one board. Tap snacks to add them (tap again for more, − to remove one), see the running total, then "Take all". Undo is available for 5 seconds.
- **Admin** (PIN, default `1234`):
  - **Stock:** stock and par levels, plus a shopping list.
  - **Stock count:** count what's actually there and log what's missing.
  - **Snacks & prices:** names, prices, photos, add and remove snacks.
  - **Insights:** most-taken snacks and busiest times.
  - **Settings:** PIN, the "only a few left" level, clearing history.

Everything is saved on the device only.

## Install

Download the APK from [Releases](../../releases) and open it on the device. You may need to allow installing from your browser or file manager.

## Build

```bash
./gradlew assembleRelease
```

The APK lands in `app/build/outputs/apk/release/app-release.apk`. Release builds are signed with the local debug key so they install directly. Updating over an existing install requires the same machine's key, so use a real keystore if you'll ship updates from somewhere else.

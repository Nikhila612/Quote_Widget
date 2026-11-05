# Sunnote

Sunnote is a home-screen widget for iOS and Android that shows one uplifting quote per day, changing at local midnight, with no repeats until the whole library has been shown.

```
shared/quotes.json   # the quote library, bundled into both apps
ios/                 # SwiftUI app + WidgetKit extension + QuoteCore package
android/             # Compose app + Glance widget (app) + core module
```

## iOS
- Open `ios/QuoteWidget.xcodeproj`, pick the `QuoteApp` scheme, run.
- Set your Team under Signing for both targets before running on a device (the App Group `group.io.github.nikhila612.quotewidget` needs it).
- Project is generated from `ios/project.yml`: after adding files, run `xcodegen generate` in `ios/`.
- Core tests: `cd ios/QuoteCore && swift test`
- Command-line simulator builds must be signed (ad-hoc is enough) or the App Group is dropped and the app and widget show different quotes:

```bash
xcodebuild -project ios/QuoteWidget.xcodeproj -scheme QuoteApp -destination 'generic/platform=iOS Simulator' CODE_SIGN_IDENTITY=- build
```

- App icon: drawn by `tools/make_icon.swift` (`swift tools/make_icon.swift ios/QuoteApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png`).

## Screenshots
Widgets on a real home screen (5 Nov 2026) are in [docs/screenshots](docs/screenshots).

Store images (App Store 1320 x 2868, Google Play 1080 x 1920, Play feature graphic and 512 px icon) are in [docs/store](docs/store). They are built from the raw captures in `docs/store/raw`; to change a caption, edit `tools/make_store_images.swift` and run:

```bash
swift tools/make_store_images.swift docs/store/raw docs/store
```

## Android
- Open `android/` in Android Studio, run the `app` configuration.
- Core tests: `cd android && ./gradlew :core:testDebugUnitTest`

## Daily pick
Both platforms use the same rule (`QuotePicker`): days since install pick a position in a per-install shuffled order (SplitMix64 + Fisher–Yates). Both test suites assert the same shuffle vectors, so keep the two implementations in sync.

## Mood check-in
Once a day the app asks four optional questions (`CheckIn` in `Mood.swift` / `Mood.kt`). `MoodPicker.tag` scores the answers into one of the six quote tags, and `MoodPicker.pick` chooses a quote with that tag from the tag's own shuffled order, skipping anything shown in the last 30 days. The pick is saved per day, so the widget, the app and History all agree. Mood buttons on the large widget (iOS 17 App Intent, Android Glance action) replace the morning's answers with just that feeling. Both test suites assert the same mood vectors.

## Own quotes and widget styles
Users write their own quotes in My Quotes. They take turns every 7th day, and **Show today** (or the editor's "Show it in today's widget" switch) pins one to today's widget by saving it as today's pick. A later check-in replaces the pinned quote.

Widget styles: Sunrise, Light, Dark, Ocean, Forest, Lavender, Rose, Sand and Midnight on both platforms, set in Settings (iOS `Shared/Theme.swift`, Android `widget_bg_*.xml` plus the ink colours in `QuoteGlanceWidget.kt`). Android 12+ also has **Wallpaper**, which uses the phone's wallpaper colours through `GlanceTheme`. On iOS 18 tinted and clear home screens the widget follows the system look automatically.

## Adding quotes
Append to `shared/quotes.json` with the next `id`, then check the file:

```bash
python3 tools/validate_quotes.py
```

Rules: positive, 25 words or fewer, no religious wording, and accurately attributed. Use authors who died more than 70 years ago, works published before 1930, or traditional proverbs. Put the work it comes from in `source`; use `Attributed` only when the source can't be pinned down, and recheck those first.

To check wording against the original books, run the source checker. It downloads the public-domain texts listed in `tools/sources.json` from Project Gutenberg into `tools/.texts/` (git-ignored, about 60 MB on first run). It then reports each quote as FOUND, CLOSE (with the book's actual wording), COLLECTION (not in the author's own books, but word-for-word in a quotation or proverb collection such as Bartlett's *Familiar Quotations*), MISSING or SKIPPED:

```bash
python3 tools/check_quotes.py
```

MISSING is expected for letters, speeches, proverbs and works Gutenberg doesn't have. Check a CLOSE result against the printed wording and fix it.

Only ever append. Once the app ships, changing or reordering ids would reshuffle people's daily order and break their favourites.

Note: this folder is in iCloud-synced Documents. If a build fails with "defined multiple times" or files ending in ` 2`, delete the `build` folders and rebuild.

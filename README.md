# Daily Quote Widget

A home-screen widget for iOS and Android that shows one uplifting quote per day, changing at local midnight, with no repeats until the whole library has been shown.

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

## Android
- Open `android/` in Android Studio, run the `app` configuration.
- Core tests: `cd android && ./gradlew :core:testDebugUnitTest`

## Daily pick
Both platforms use the same rule (`QuotePicker`): days since install pick a position in a per-install shuffled order (SplitMix64 + Fisher–Yates). Both test suites assert the same shuffle vectors, so keep the two implementations in sync.

## Adding quotes
Append to `shared/quotes.json` with the next `id`. Rules: positive, 25 words or fewer, accurately attributed (public domain or original).

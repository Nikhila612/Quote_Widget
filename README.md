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

## Android
- Open `android/` in Android Studio, run the `app` configuration.
- Core tests: `cd android && ./gradlew :core:testDebugUnitTest`

## Daily pick
Both platforms use the same rule (`QuotePicker`): days since install pick a position in a per-install shuffled order (SplitMix64 + Fisher–Yates). Both test suites assert the same shuffle vectors, so keep the two implementations in sync.

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

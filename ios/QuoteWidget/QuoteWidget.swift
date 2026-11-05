import AppIntents
import SwiftUI
import WidgetKit
import QuoteCore

struct QuoteEntry: TimelineEntry {
    let date: Date
    let quote: Quote
    let theme: WidgetTheme
    var feeling: Feeling? = nil   // today's check-in mood, for highlighting the widget's mood buttons
}

/// Tapping a mood on the large widget: re-picks today's quote for that mood (iOS 17 interactive widget).
struct MoodIntent: AppIntent {
    static let title: LocalizedStringResource = "Set today's mood"

    @Parameter(title: "Mood") var mood: String

    init() {}
    init(_ feeling: Feeling) { mood = feeling.rawValue }

    func perform() async throws -> some IntentResult {
        // A widget tap means "this is how I feel now", so it replaces the morning's answers.
        let store = QuoteStore(quotes: QuoteLibrary.load())
        store.checkIn(CheckIn(feeling: Feeling(rawValue: mood)))
        return .result()
    }
}

struct QuoteProvider: TimelineProvider {
    private static let quotes = QuoteLibrary.load()

    func placeholder(in context: Context) -> QuoteEntry {
        QuoteEntry(date: .now, quote: QuoteLibrary.fallback, theme: .sunrise)
    }

    func getSnapshot(in context: Context, completion: @escaping (QuoteEntry) -> Void) {
        let store = QuoteStore(quotes: Self.quotes)
        completion(QuoteEntry(date: .now, quote: store.quote(), theme: store.theme))
    }

    /// One entry now, then one at each of the next 7 local midnights, so the quote
    /// flips on time even though iOS limits how often a widget reloads.
    func getTimeline(in context: Context, completion: @escaping (Timeline<QuoteEntry>) -> Void) {
        let store = QuoteStore(quotes: Self.quotes)
        let calendar = Calendar.current
        let today = calendar.startOfDay(for: .now)

        var entries = [QuoteEntry(date: .now, quote: store.quote(), theme: store.theme, feeling: store.isMoodPick() ? store.checkIn()?.feeling : nil)]
        for offset in 1...7 {
            guard let midnight = calendar.date(byAdding: .day, value: offset, to: today) else { continue }
            entries.append(QuoteEntry(date: midnight, quote: store.quote(for: midnight), theme: store.theme))
        }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

struct QuoteWidgetView: View {
    @Environment(\.widgetFamily) private var family
    @Environment(\.widgetRenderingMode) private var renderingMode
    let entry: QuoteEntry

    /// Full colour normally; on tinted or clear home screens (iOS 18) the system tints the widget,
    /// so text uses the primary style and the quote is marked accentable.
    private var ink: Color { renderingMode == .fullColor ? entry.theme.foreground : .primary }

    /// The moods offered on the large widget (the app's check-in has all six).
    static let widgetMoods: [Feeling] = [.happy, .calm, .tired, .worried]

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            VStack(alignment: .leading, spacing: 8) {
                Text("\u{201C}\(entry.quote.text)\u{201D}")
                    .font(.system(quoteSize, design: .serif))
                    .minimumScaleFactor(0.5)
                    .widgetAccentable()
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)

                if family != .systemSmall {
                    Text("— \(entry.quote.author)")
                        .font(.caption.weight(.medium))
                        .opacity(0.8)
                }
            }
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Today's quote: \(entry.quote.text), by \(entry.quote.author)")

            if family == .systemLarge {
                moodButtons
                HStack {
                    Text(entry.quote.tags.first?.capitalized ?? "")
                    Spacer()
                    Text(entry.date, format: .dateTime.weekday(.wide).day().month())
                }
                .font(.caption2)
                .opacity(0.65)
            }
        }
        .foregroundStyle(ink)
        .containerBackground(for: .widget) { entry.theme.background }
        .widgetURL(URL(string: "quotewidget://today"))
    }

    /// "How are you?" with four tappable moods; the current one is filled in.
    private var moodButtons: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("How are you?").font(.caption2.weight(.semibold)).opacity(0.7)
            HStack(spacing: 6) {
                ForEach(Self.widgetMoods) { mood in
                    let isOn = entry.feeling == mood
                    Button(intent: MoodIntent(mood)) {
                        Text(mood.title)
                            .font(.caption.weight(isOn ? .semibold : .regular))
                            .lineLimit(1)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 6)
                            .background(ink.opacity(isOn ? 0.22 : 0.08), in: Capsule())
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("I feel \(mood.title.lowercased())")
                    .accessibilityAddTraits(isOn ? .isSelected : [])
                }
            }
        }
    }

    private var quoteSize: Font.TextStyle {
        switch family {
        case .systemSmall: .subheadline
        case .systemMedium: .title3
        default: .title2
        }
    }
}

struct QuoteWidget: Widget {
    let kind = "QuoteWidget"

    var body: some WidgetConfiguration {
        StaticConfiguration(kind: kind, provider: QuoteProvider()) { entry in
            QuoteWidgetView(entry: entry)
        }
        .configurationDisplayName("Sunnote")
        .description("A fresh, uplifting quote every day.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

@main
struct QuoteWidgetBundle: WidgetBundle {
    var body: some Widget {
        QuoteWidget()
    }
}

#Preview(as: .systemMedium) {
    QuoteWidget()
} timeline: {
    QuoteEntry(date: .now, quote: QuoteLibrary.fallback, theme: .sunrise)
    QuoteEntry(date: .now, quote: QuoteLibrary.fallback, theme: .dark)
}

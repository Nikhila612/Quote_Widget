import SwiftUI
import WidgetKit
import QuoteCore

struct QuoteEntry: TimelineEntry {
    let date: Date
    let quote: Quote
    let theme: WidgetTheme
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

        var entries = [QuoteEntry(date: .now, quote: store.quote(), theme: store.theme)]
        for offset in 1...7 {
            guard let midnight = calendar.date(byAdding: .day, value: offset, to: today) else { continue }
            entries.append(QuoteEntry(date: midnight, quote: store.quote(for: midnight), theme: store.theme))
        }
        completion(Timeline(entries: entries, policy: .atEnd))
    }
}

struct QuoteWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: QuoteEntry

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("\u{201C}\(entry.quote.text)\u{201D}")
                .font(.system(quoteSize, design: .serif))
                .minimumScaleFactor(0.5)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)

            if family != .systemSmall {
                Text("— \(entry.quote.author)")
                    .font(.caption.weight(.medium))
                    .opacity(0.8)
            }
            if family == .systemLarge, let tag = entry.quote.tags.first {
                HStack {
                    Text(tag.capitalized)
                    Spacer()
                    Text(entry.date, format: .dateTime.weekday(.wide).day().month())
                }
                .font(.caption2)
                .opacity(0.65)
            }
        }
        .foregroundStyle(entry.theme.foreground)
        .containerBackground(for: .widget) { entry.theme.background }
        .widgetURL(URL(string: "quotewidget://today"))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Today's quote: \(entry.quote.text), by \(entry.quote.author)")
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

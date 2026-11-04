import Foundation

public enum WidgetTheme: String, CaseIterable, Identifiable, Sendable {
    case sunrise, light, dark
    public var id: String { rawValue }
    public var title: String { rawValue.capitalized }
}

/// Settings and state shared between the app and the widget through the App Group.
public final class QuoteStore {
    public static let appGroup = "group.io.github.nikhila612.quotewidget"

    private enum Key {
        static let seed = "seed"
        static let installDay = "installDay"
        static let favourites = "favourites"
        static let theme = "theme"
    }

    private let defaults: UserDefaults
    private let calendar: Calendar
    public let quotes: [Quote]

    public init(
        quotes: [Quote],
        defaults: UserDefaults = UserDefaults(suiteName: QuoteStore.appGroup) ?? .standard,
        calendar: Calendar = .current
    ) {
        self.quotes = quotes.isEmpty ? [QuoteLibrary.fallback] : quotes
        self.defaults = defaults
        self.calendar = calendar
        ensureInstallState()
    }

    // MARK: Daily quote

    public func dayIndex(for date: Date) -> Int {
        let start = calendar.startOfDay(for: installDate)
        let end = calendar.startOfDay(for: date)
        return max(0, calendar.dateComponents([.day], from: start, to: end).day ?? 0)
    }

    public func quote(for date: Date = .now) -> Quote {
        let index = QuotePicker.quoteIndex(
            dayIndex: dayIndex(for: date), count: quotes.count, seed: seed)
        return quotes[index]
    }

    /// Today's quote first, then earlier days, never before the install day.
    public func history(days: Int = 30, from date: Date = .now) -> [(date: Date, quote: Quote)] {
        let today = calendar.startOfDay(for: date)
        let available = min(days, dayIndex(for: date) + 1)
        return (0..<available).compactMap { offset in
            guard let day = calendar.date(byAdding: .day, value: -offset, to: today) else { return nil }
            return (day, quote(for: day))
        }
    }

    // MARK: Favourites

    public var favouriteIDs: Set<String> {
        Set(defaults.stringArray(forKey: Key.favourites) ?? [])
    }

    public var favourites: [Quote] {
        let ids = favouriteIDs
        return quotes.filter { ids.contains($0.id) }
    }

    public func isFavourite(_ quote: Quote) -> Bool { favouriteIDs.contains(quote.id) }

    public func toggleFavourite(_ quote: Quote) {
        var ids = favouriteIDs
        if ids.contains(quote.id) { ids.remove(quote.id) } else { ids.insert(quote.id) }
        defaults.set(Array(ids).sorted(), forKey: Key.favourites)
    }

    // MARK: Theme

    public var theme: WidgetTheme {
        get { WidgetTheme(rawValue: defaults.string(forKey: Key.theme) ?? "") ?? .sunrise }
        set { defaults.set(newValue.rawValue, forKey: Key.theme) }
    }

    // MARK: Install state

    private var seed: UInt64 {
        UInt64(bitPattern: Int64(defaults.integer(forKey: Key.seed)))
    }

    private var installDate: Date {
        let stamp = defaults.double(forKey: Key.installDay)
        return Date(timeIntervalSince1970: stamp)
    }

    private func ensureInstallState() {
        if defaults.object(forKey: Key.seed) == nil {
            defaults.set(Int.random(in: Int.min...Int.max), forKey: Key.seed)
        }
        if defaults.object(forKey: Key.installDay) == nil {
            let start = calendar.startOfDay(for: .now)
            defaults.set(start.timeIntervalSince1970, forKey: Key.installDay)
        }
    }
}

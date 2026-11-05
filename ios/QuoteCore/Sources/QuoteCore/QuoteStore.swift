import Foundation

public enum WidgetTheme: String, CaseIterable, Identifiable, Sendable {
    case sunrise, light, dark
    public var id: String { rawValue }
    public var title: String { rawValue.capitalized }
}

/// Settings and state shared between the app and the widget through the App Group.
public final class QuoteStore {
    public static let appGroup = "group.io.github.nikhila612.quotewidget"
    public static let maxCustomLength = 160
    public static let defaultCustomAuthor = "Me"

    private enum Key {
        static let seed = "seed"
        static let installDay = "installDay"
        static let favourites = "favourites"
        static let theme = "theme"
        static let customQuotes = "customQuotes"
        static let moodPicks = "moodPicks"        // [yyyy-MM-dd: quote id] chosen by a check-in
        static let tagUses = "tagUses"            // [tag: positions used in that tag's rotation]
        static let checkInDay = "checkInDay"      // yyyy-MM-dd of the last check-in
        static let checkInAnswers = "checkInAnswers"
        static let skipDay = "skipDay"            // yyyy-MM-dd the check-in was skipped
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
        if let id = moodPicks[dayKey(date)], let picked = quote(withID: id) { return picked }
        let custom = customQuotes
        let pick = QuotePicker.dailyPick(
            dayIndex: dayIndex(for: date), libraryCount: quotes.count, customCount: custom.count, seed: seed)
        switch pick {
        case .library(let index): return quotes[index]
        case .custom(let index): return custom[index]
        }
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

    // MARK: Daily check-in

    /// True until the user checks in or skips today.
    public func needsCheckIn(on date: Date = .now) -> Bool {
        let key = dayKey(date)
        return defaults.string(forKey: Key.checkInDay) != key && defaults.string(forKey: Key.skipDay) != key
    }

    public func skipCheckIn(on date: Date = .now) {
        defaults.set(dayKey(date), forKey: Key.skipDay)
    }

    /// Today's answers, if the user has checked in today.
    public func checkIn(on date: Date = .now) -> CheckIn? {
        guard defaults.string(forKey: Key.checkInDay) == dayKey(date),
              let data = defaults.data(forKey: Key.checkInAnswers) else { return nil }
        return try? JSONDecoder().decode(CheckIn.self, from: data)
    }

    /// Records the answers and picks today's quote to match them. Returns today's quote.
    @discardableResult
    public func checkIn(_ answers: CheckIn, on date: Date = .now) -> Quote {
        let key = dayKey(date)
        defaults.set(key, forKey: Key.checkInDay)
        defaults.set(try? JSONEncoder().encode(answers), forKey: Key.checkInAnswers)
        var picks = moodPicks
        picks[key] = nil
        guard let tag = MoodPicker.tag(for: answers) else {
            moodPicks = picks
            return quote(for: date)
        }
        let candidates = quotes.indices.filter { quotes[$0].tags.contains(tag) }
        var avoid = Set<Int>()
        for offset in 1...30 {
            guard let day = calendar.date(byAdding: .day, value: -offset, to: date) else { continue }
            let shown = quote(for: day)
            if let index = quotes.firstIndex(of: shown) { avoid.insert(index) }
        }
        var uses = tagUses
        guard let choice = MoodPicker.pick(candidates: candidates, tag: tag, seed: seed,
                                           uses: uses[tag] ?? 0, avoid: avoid) else { return quote(for: date) }
        uses[tag] = (uses[tag] ?? 0) + choice.consumed
        tagUses = uses
        picks[key] = quotes[choice.index].id
        moodPicks = picks.filter { $0.key >= dayKey(calendar.date(byAdding: .day, value: -60, to: date) ?? date) }
        return quotes[choice.index]
    }

    private var moodPicks: [String: String] {
        get { defaults.dictionary(forKey: Key.moodPicks) as? [String: String] ?? [:] }
        set { defaults.set(newValue, forKey: Key.moodPicks) }
    }

    private var tagUses: [String: Int] {
        get { defaults.dictionary(forKey: Key.tagUses) as? [String: Int] ?? [:] }
        set { defaults.set(newValue, forKey: Key.tagUses) }
    }

    private func quote(withID id: String) -> Quote? {
        quotes.first { $0.id == id } ?? customQuotes.first { $0.id == id }
    }

    /// yyyy-MM-dd in the user's calendar; sorts in date order.
    private func dayKey(_ date: Date) -> String {
        let c = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", c.year ?? 0, c.month ?? 0, c.day ?? 0)
    }

    // MARK: The user's own quotes

    public var customQuotes: [Quote] {
        guard let data = defaults.data(forKey: Key.customQuotes),
              let saved = try? JSONDecoder().decode([Quote].self, from: data)
        else { return [] }
        return saved
    }

    /// Adds a new quote, or updates the one with `id`. Returns nil when the text is empty.
    @discardableResult
    public func saveCustomQuote(text: String, author: String, id: String? = nil) -> Quote? {
        let cleanText = String(text.trimmingCharacters(in: .whitespacesAndNewlines).prefix(Self.maxCustomLength))
        guard !cleanText.isEmpty else { return nil }
        let cleanAuthor = author.trimmingCharacters(in: .whitespacesAndNewlines)
        let quote = Quote(
            id: id ?? "u-\(UUID().uuidString)",
            text: cleanText,
            author: cleanAuthor.isEmpty ? Self.defaultCustomAuthor : cleanAuthor,
            tags: ["mine"])

        var all = customQuotes
        if let existing = all.firstIndex(where: { $0.id == quote.id }) {
            all[existing] = quote
        } else {
            all.append(quote)
        }
        writeCustomQuotes(all)
        return quote
    }

    public func deleteCustomQuote(id: String) {
        writeCustomQuotes(customQuotes.filter { $0.id != id })
        var ids = favouriteIDs
        ids.remove(id)
        defaults.set(Array(ids).sorted(), forKey: Key.favourites)
    }

    private func writeCustomQuotes(_ quotes: [Quote]) {
        defaults.set(try? JSONEncoder().encode(quotes), forKey: Key.customQuotes)
    }

    // MARK: Favourites

    public var favouriteIDs: Set<String> {
        Set(defaults.stringArray(forKey: Key.favourites) ?? [])
    }

    public var favourites: [Quote] {
        let ids = favouriteIDs
        return (customQuotes + quotes).filter { ids.contains($0.id) }
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

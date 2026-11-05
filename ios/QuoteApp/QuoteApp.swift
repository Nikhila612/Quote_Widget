import SwiftUI
import WidgetKit
import QuoteCore

@main
struct QuoteApp: App {
    @State private var model = QuoteModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environment(model)
                .onOpenURL { _ in model.selectedTab = .today }
                .onReceive(NotificationCenter.default.publisher(for: .NSSystemTimeZoneDidChange)) { _ in
                    model.refresh()
                }
        }
    }
}

enum AppTab: Hashable { case today, myQuotes, favourites, history, settings }

@Observable
final class QuoteModel {
    private let store = QuoteStore(quotes: QuoteLibrary.load())

    var selectedTab: AppTab = .today
    private(set) var today: Quote
    private(set) var favourites: [Quote]
    private(set) var theme: WidgetTheme
    private(set) var customQuotes: [Quote]
    private(set) var checkIn: CheckIn?
    var showCheckIn = false

    init() {
        today = store.quote()
        favourites = store.favourites
        theme = store.theme
        customQuotes = store.customQuotes
        checkIn = store.checkIn()
        shownTodayID = store.shownTodayID()
    }

    private(set) var shownTodayID: String?

    /// The tag today's quote was picked for, when a check-in chose it.
    var moodTag: String? {
        shownTodayID == nil ? checkIn.flatMap { MoodPicker.tag(for: $0) } : nil
    }

    /// Puts one of the user's own quotes on today's widget.
    func showToday(_ quote: Quote) {
        store.showToday(quote)
        today = quote
        shownTodayID = quote.id
        WidgetCenter.shared.reloadAllTimelines()
    }

    func submitCheckIn(_ answers: CheckIn) {
        today = store.checkIn(answers)
        checkIn = answers
        shownTodayID = nil
        showCheckIn = false
        WidgetCenter.shared.reloadAllTimelines()
    }

    func skipCheckIn() {
        store.skipCheckIn()
        showCheckIn = false
    }

    /// Called when the app comes to the front: refresh, and ask once a day.
    func becameActive() {
        refresh()
        if store.needsCheckIn() { showCheckIn = true }
    }

    /// Adds a new quote, or updates `existing`. Returns false when the text is empty.
    @discardableResult
    func saveCustomQuote(text: String, author: String, existing: Quote? = nil, showToday: Bool = false) -> Bool {
        guard let saved = store.saveCustomQuote(text: text, author: author, id: existing?.id) else { return false }
        if showToday || saved.id == shownTodayID { store.showToday(saved) }
        customQuotesChanged()
        return true
    }

    func deleteCustomQuote(_ quote: Quote) {
        store.deleteCustomQuote(id: quote.id)
        customQuotesChanged()
    }

    private func customQuotesChanged() {
        customQuotes = store.customQuotes
        favourites = store.favourites
        refresh()
    }

    var history: [(date: Date, quote: Quote)] { store.history() }

    func isFavourite(_ quote: Quote) -> Bool { favourites.contains(quote) }

    func toggleFavourite(_ quote: Quote) {
        store.toggleFavourite(quote)
        favourites = store.favourites
    }

    func setTheme(_ newTheme: WidgetTheme) {
        store.theme = newTheme
        theme = newTheme
        WidgetCenter.shared.reloadAllTimelines()
    }

    func refresh() {
        today = store.quote()
        checkIn = store.checkIn()
        shownTodayID = store.shownTodayID()
        WidgetCenter.shared.reloadAllTimelines()
    }
}

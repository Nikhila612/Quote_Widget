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

    init() {
        today = store.quote()
        favourites = store.favourites
        theme = store.theme
        customQuotes = store.customQuotes
    }

    /// Adds a new quote, or updates `existing`. Returns false when the text is empty.
    @discardableResult
    func saveCustomQuote(text: String, author: String, existing: Quote? = nil) -> Bool {
        guard store.saveCustomQuote(text: text, author: author, id: existing?.id) != nil else { return false }
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
        WidgetCenter.shared.reloadAllTimelines()
    }
}

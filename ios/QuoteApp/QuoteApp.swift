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

enum AppTab: Hashable { case today, favourites, history, settings }

@Observable
final class QuoteModel {
    private let store = QuoteStore(quotes: QuoteLibrary.load())

    var selectedTab: AppTab = .today
    private(set) var today: Quote
    private(set) var favourites: [Quote]
    private(set) var theme: WidgetTheme

    init() {
        today = store.quote()
        favourites = store.favourites
        theme = store.theme
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

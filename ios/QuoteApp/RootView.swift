import SwiftUI
import QuoteCore

struct RootView: View {
    @Environment(QuoteModel.self) private var model
    @Environment(\.scenePhase) private var scenePhase

    var body: some View {
        @Bindable var model = model
        TabView(selection: $model.selectedTab) {
            TodayView()
                .tabItem { Label("Today", systemImage: "sun.max") }
                .tag(AppTab.today)
            MyQuotesView()
                .tabItem { Label("My Quotes", systemImage: "pencil.line") }
                .tag(AppTab.myQuotes)
            FavouritesView()
                .tabItem { Label("Favourites", systemImage: "heart") }
                .tag(AppTab.favourites)
            HistoryView()
                .tabItem { Label("History", systemImage: "clock") }
                .tag(AppTab.history)
            SettingsView()
                .tabItem { Label("Settings", systemImage: "gearshape") }
                .tag(AppTab.settings)
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { model.refresh() }
        }
    }
}

struct TodayView: View {
    @Environment(QuoteModel.self) private var model

    var body: some View {
        let quote = model.today
        ZStack {
            model.theme.background.ignoresSafeArea()
            VStack(alignment: .leading, spacing: 20) {
                Text(Date.now, format: .dateTime.weekday(.wide).day().month(.wide))
                    .font(.subheadline.weight(.medium))
                    .opacity(0.7)
                Text("\u{201C}\(quote.text)\u{201D}")
                    .font(.system(.largeTitle, design: .serif))
                    .transition(.opacity)
                    .id(quote.id)
                Text("— \(quote.author)")
                    .font(.headline)
                HStack(spacing: 16) {
                    Button {
                        withAnimation(.spring) { model.toggleFavourite(quote) }
                    } label: {
                        Image(systemName: model.isFavourite(quote) ? "heart.fill" : "heart")
                            .font(.title2)
                            .symbolEffect(.bounce, value: model.isFavourite(quote))
                    }
                    .accessibilityLabel(model.isFavourite(quote) ? "Remove from favourites" : "Add to favourites")
                    ShareLink(item: "\u{201C}\(quote.text)\u{201D} — \(quote.author)") {
                        Image(systemName: "square.and.arrow.up").font(.title2)
                    }
                }
                .padding(.top, 8)
            }
            .foregroundStyle(model.theme.foreground)
            .padding(32)
            .animation(.easeInOut, value: quote.id)
        }
    }
}

struct QuoteRow: View {
    let quote: Quote
    var date: Date?

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if let date {
                Text(date, format: .dateTime.weekday(.abbreviated).day().month())
                    .font(.caption).foregroundStyle(.secondary)
            }
            Text(quote.text).font(.system(.body, design: .serif))
            Text("— \(quote.author)").font(.caption).foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}

struct FavouritesView: View {
    @Environment(QuoteModel.self) private var model

    var body: some View {
        NavigationStack {
            Group {
                if model.favourites.isEmpty {
                    ContentUnavailableView("No favourites yet", systemImage: "heart",
                                           description: Text("Tap the heart on a quote to save it here."))
                } else {
                    List(model.favourites) { quote in
                        QuoteRow(quote: quote)
                            .swipeActions { Button("Remove", role: .destructive) { model.toggleFavourite(quote) } }
                    }
                }
            }
            .navigationTitle("Favourites")
        }
    }
}

struct HistoryView: View {
    @Environment(QuoteModel.self) private var model

    var body: some View {
        NavigationStack {
            List(model.history, id: \.date) { item in
                QuoteRow(quote: item.quote, date: item.date)
            }
            .navigationTitle("History")
        }
    }
}

struct SettingsView: View {
    @Environment(QuoteModel.self) private var model

    var body: some View {
        NavigationStack {
            Form {
                Section("Widget style") {
                    Picker("Theme", selection: Binding(get: { model.theme }, set: { model.setTheme($0) })) {
                        ForEach(WidgetTheme.allCases) { Text($0.title).tag($0) }
                    }
                    .pickerStyle(.segmented)
                }
                Section {
                    Text("A new quote appears every day at midnight. Add the widget from your Home Screen: touch and hold, tap Edit, then Add Widget.")
                        .font(.footnote).foregroundStyle(.secondary)
                }
            }
            .navigationTitle("Settings")
        }
    }
}

#Preview {
    RootView().environment(QuoteModel())
}

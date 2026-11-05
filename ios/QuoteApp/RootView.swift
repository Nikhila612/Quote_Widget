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
            if phase == .active { model.becameActive() }
        }
        .onAppear { model.becameActive() }
        .sheet(isPresented: $model.showCheckIn) {
            CheckInView(initial: model.checkIn ?? CheckIn())
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

                Button {
                    model.showCheckIn = true
                } label: {
                    Label(model.moodTag.map { "Picked for you: \($0.capitalized)" } ?? "How are you today?",
                          systemImage: "sparkles")
                        .font(.subheadline.weight(.medium))
                        .padding(.horizontal, 14).padding(.vertical, 8)
                        .background(.white.opacity(0.35), in: Capsule())
                }
                .padding(.top, 12)
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
                Section {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 88), spacing: 14)], spacing: 16) {
                        ForEach(WidgetTheme.allCases) { theme in
                            ThemeSwatch(theme: theme, isSelected: model.theme == theme) { model.setTheme(theme) }
                        }
                    }
                    .padding(.vertical, 8)
                } header: {
                    Text("Widget style")
                } footer: {
                    Text("Pick the one that suits your wallpaper. On iOS 18 tinted or clear home screens, Sunnote follows your home screen's look automatically.")
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

/// A small preview of a widget style: its gradient with "Aa" in its text colour.
struct ThemeSwatch: View {
    let theme: WidgetTheme
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 6) {
                RoundedRectangle(cornerRadius: 14)
                    .fill(theme.background)
                    .frame(height: 64)
                    .overlay(Text("Aa").font(.system(.title3, design: .serif)).foregroundStyle(theme.foreground))
                    .overlay(RoundedRectangle(cornerRadius: 14)
                        .stroke(isSelected ? Color.accentColor : Color.primary.opacity(0.12), lineWidth: isSelected ? 3 : 1))
                Text(theme.title).font(.caption.weight(isSelected ? .semibold : .regular))
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(theme.title) style")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

#Preview {
    RootView().environment(QuoteModel())
}

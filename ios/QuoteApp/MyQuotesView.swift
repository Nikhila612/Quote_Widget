import SwiftUI
import QuoteCore

/// The user's own quotes. One of them shows in the widget every 7th day.
struct MyQuotesView: View {
    @Environment(QuoteModel.self) private var model
    @State private var editing: EditTarget?

    var body: some View {
        NavigationStack {
            Group {
                if model.customQuotes.isEmpty {
                    ContentUnavailableView {
                        Label("Write your own", systemImage: "pencil.line")
                    } description: {
                        Text("Add words that lift you up. One of your quotes appears in the widget once a week.")
                    } actions: {
                        Button("Write a quote") { editing = .new }
                            .buttonStyle(.borderedProminent)
                    }
                } else {
                    List {
                        Section {
                            ForEach(model.customQuotes) { quote in
                                HStack(alignment: .center, spacing: 12) {
                                    Button { editing = .edit(quote) } label: {
                                        VStack(alignment: .leading, spacing: 6) {
                                            QuoteRow(quote: quote)
                                            if model.shownTodayID == quote.id {
                                                Label("On today's widget", systemImage: "sun.max.fill")
                                                    .font(.caption.weight(.semibold))
                                                    .foregroundStyle(.orange)
                                            }
                                        }
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .contentShape(Rectangle())
                                    }
                                    .buttonStyle(.borderless)
                                    .tint(.primary)

                                    if model.shownTodayID != quote.id {
                                        Button { model.showToday(quote) } label: {
                                            Label("Show today", systemImage: "sun.max")
                                                .font(.caption.weight(.semibold))
                                        }
                                        .buttonStyle(.bordered)
                                        .controlSize(.small)
                                        .accessibilityLabel("Show this quote in today's widget")
                                    }
                                }
                                .swipeActions {
                                    Button("Delete", role: .destructive) { model.deleteCustomQuote(quote) }
                                }
                            }
                        } footer: {
                            Text("Tap Show today to put a quote on your widget now. Your quotes also take turns once a week.")
                        }
                    }
                }
            }
            .navigationTitle("My Quotes")
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button { editing = .new } label: { Image(systemName: "plus") }
                        .accessibilityLabel("Write a quote")
                }
            }
            .sheet(item: $editing) { target in
                QuoteEditor(existing: target.quote)
            }
        }
    }
}

private enum EditTarget: Identifiable {
    case new
    case edit(Quote)

    var id: String { quote?.id ?? "new" }
    var quote: Quote? {
        if case .edit(let quote) = self { return quote }
        return nil
    }
}

private struct QuoteEditor: View {
    @Environment(QuoteModel.self) private var model
    @Environment(\.dismiss) private var dismiss
    let existing: Quote?

    @State private var text = ""
    @State private var author = ""
    @State private var showToday = true
    @FocusState private var textFocused: Bool

    private var trimmed: String { text.trimmingCharacters(in: .whitespacesAndNewlines) }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("Something that makes you smile…", text: $text, axis: .vertical)
                        .lineLimit(3...8)
                        .font(.system(.title3, design: .serif))
                        .focused($textFocused)
                        .onChange(of: text) { _, new in
                            if new.count > QuoteStore.maxCustomLength {
                                text = String(new.prefix(QuoteStore.maxCustomLength))
                            }
                        }
                } header: {
                    Text("Quote")
                } footer: {
                    Text("\(text.count)/\(QuoteStore.maxCustomLength) characters. Short quotes fit the small widget best.")
                }
                Section("Author") {
                    TextField(QuoteStore.defaultCustomAuthor, text: $author)
                        .textInputAutocapitalization(.words)
                }
                Section {
                    Toggle("Show it in today's widget", isOn: $showToday)
                }
                if let existing {
                    Section {
                        Button("Delete quote", role: .destructive) {
                            model.deleteCustomQuote(existing)
                            dismiss()
                        }
                    }
                }
            }
            .navigationTitle(existing == nil ? "New quote" : "Edit quote")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        if model.saveCustomQuote(text: text, author: author, existing: existing, showToday: showToday) { dismiss() }
                    }
                    .disabled(trimmed.isEmpty)
                }
            }
            .onAppear {
                text = existing?.text ?? ""
                author = existing.map { $0.author == QuoteStore.defaultCustomAuthor ? "" : $0.author } ?? ""
                showToday = existing == nil || model.shownTodayID == existing?.id
                textFocused = existing == nil
            }
        }
        .presentationDetents([.medium, .large])
    }
}

#Preview {
    MyQuotesView().environment(QuoteModel())
}

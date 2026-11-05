import SwiftUI
import QuoteCore

/// The daily check-in: four optional questions answered with a tap each.
struct CheckInView: View {
    @Environment(QuoteModel.self) private var model
    @State private var answers: CheckIn

    init(initial: CheckIn) { _answers = State(initialValue: initial) }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    Text("Answer any you like, and today's quote will match.")
                        .font(.subheadline).foregroundStyle(.secondary)
                    question("What's your mood right now?", Feeling.allCases, \.title, $answers.feeling)
                    question("How will your day be?", DayKind.allCases, \.title, $answers.day)
                    question("How do you want today to feel?", Wish.allCases, \.title, $answers.wish)
                    question("What kind of quote today?", QuoteKind.allCases, \.title, $answers.kind)
                }
                .padding(20)
            }
            .navigationTitle("How are you today?")
            .navigationBarTitleDisplayMode(.inline)
            .safeAreaInset(edge: .bottom) {
                VStack(spacing: 10) {
                    Button {
                        model.submitCheckIn(answers)
                    } label: {
                        Text("Show my quote").frame(maxWidth: .infinity).padding(.vertical, 6)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(answers.isEmpty)
                    Button("Skip for today") { model.skipCheckIn() }
                        .font(.subheadline)
                }
                .padding(.horizontal, 20).padding(.vertical, 12)
                .background(.bar)
            }
        }
        .presentationDetents([.large])
    }

    /// One question with its answers as chips; tapping the chosen chip again clears it.
    private func question<T: Identifiable & Equatable>(
        _ title: String, _ options: [T], _ label: KeyPath<T, String>, _ selection: Binding<T?>
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title).font(.headline)
            LazyVGrid(columns: [GridItem(.adaptive(minimum: 104), spacing: 8)], alignment: .leading, spacing: 8) {
                ForEach(options) { option in
                    let isOn = selection.wrappedValue == option
                    Button {
                        selection.wrappedValue = isOn ? nil : option
                    } label: {
                        Text(option[keyPath: label])
                            .font(.subheadline.weight(isOn ? .semibold : .regular))
                            .frame(maxWidth: .infinity).padding(.vertical, 9)
                            .background(isOn ? Color.accentColor.opacity(0.18) : Color(.secondarySystemBackground),
                                        in: Capsule())
                            .overlay(Capsule().stroke(isOn ? Color.accentColor : .clear, lineWidth: 1.5))
                    }
                    .buttonStyle(.plain)
                    .accessibilityAddTraits(isOn ? .isSelected : [])
                }
            }
        }
    }
}

#Preview {
    CheckInView(initial: CheckIn(feeling: .calm)).environment(QuoteModel())
}

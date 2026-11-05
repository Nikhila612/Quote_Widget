import SwiftUI
import QuoteCore

/// A shareable picture of a quote: 1080 x 1350 (4:5), in the chosen widget style.
struct QuoteCard: View {
    static let size = CGSize(width: 1080, height: 1350)

    let quote: Quote
    let theme: WidgetTheme

    /// Longer quotes get smaller type so every quote fits. Android uses the same steps.
    private var fontSize: CGFloat {
        switch quote.text.count {
        case ..<80: 76
        case ..<160: 62
        default: 50
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 48) {
            Spacer(minLength: 0)
            Text("\u{201C}\(quote.text)\u{201D}")
                .font(.system(size: fontSize, design: .serif))
                .lineSpacing(fontSize * 0.18)
                .minimumScaleFactor(0.6)
            Text("— \(quote.author)")
                .font(.system(size: 40, weight: .semibold))
            Spacer(minLength: 0)
            Text("Sunote")
                .font(.system(size: 34, weight: .bold, design: .serif))
                .opacity(0.6)
        }
        .foregroundStyle(theme.foreground)
        .padding(.horizontal, 110)
        .padding(.vertical, 100)
        .frame(width: Self.size.width, height: Self.size.height, alignment: .leading)
        .background(theme.background)
    }

    /// Renders the card as an image, or nil if rendering fails.
    @MainActor
    static func image(for quote: Quote, theme: WidgetTheme) -> Image? {
        let renderer = ImageRenderer(content: QuoteCard(quote: quote, theme: theme))
        renderer.scale = 1
        guard let uiImage = renderer.uiImage else { return nil }
        return Image(uiImage: uiImage)
    }
}

#Preview {
    QuoteCard(quote: QuoteLibrary.fallback, theme: .sunrise)
        .scaleEffect(0.3)
        .frame(width: 324, height: 405)
}

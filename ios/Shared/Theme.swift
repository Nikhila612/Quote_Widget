import SwiftUI
import QuoteCore

/// Colours shared by the app and the widget. Each style is a soft three-stop gradient
/// plus a text colour chosen for contrast (WCAG AA on every stop).
extension WidgetTheme {
    private var stops: [Color] {
        switch self {
        case .sunrise: [rgb(1.00, 0.80, 0.67), rgb(0.98, 0.62, 0.62), rgb(0.76, 0.66, 0.91)]
        case .light: [Color(white: 0.99), Color(white: 0.97), Color(white: 0.94)]
        case .dark: [rgb(0.12, 0.12, 0.18), rgb(0.10, 0.10, 0.15), rgb(0.07, 0.07, 0.10)]
        case .ocean: [rgb(0.74, 0.94, 0.96), rgb(0.55, 0.80, 0.92), rgb(0.50, 0.65, 0.89)]
        case .forest: [rgb(0.87, 0.94, 0.82), rgb(0.66, 0.83, 0.63), rgb(0.50, 0.71, 0.56)]
        case .lavender: [rgb(0.93, 0.89, 1.00), rgb(0.80, 0.72, 0.96), rgb(0.66, 0.61, 0.92)]
        case .rose: [rgb(1.00, 0.89, 0.93), rgb(0.97, 0.71, 0.80), rgb(0.90, 0.56, 0.69)]
        case .sand: [rgb(0.98, 0.95, 0.89), rgb(0.93, 0.85, 0.72), rgb(0.87, 0.75, 0.58)]
        case .midnight: [rgb(0.17, 0.23, 0.40), rgb(0.11, 0.15, 0.28), rgb(0.06, 0.08, 0.19)]
        }
    }

    var background: LinearGradient {
        LinearGradient(colors: stops, startPoint: .topLeading, endPoint: .bottomTrailing)
    }

    var foreground: Color {
        switch self {
        case .sunrise: rgb(0.23, 0.12, 0.16)
        case .light: Color(white: 0.12)
        case .dark: Color(white: 0.96)
        case .ocean: rgb(0.06, 0.16, 0.26)
        case .forest: rgb(0.12, 0.20, 0.14)
        case .lavender: rgb(0.16, 0.12, 0.29)
        case .rose: rgb(0.29, 0.09, 0.19)
        case .sand: rgb(0.24, 0.17, 0.09)
        case .midnight: rgb(0.95, 0.95, 1.00)
        }
    }
}

private func rgb(_ r: Double, _ g: Double, _ b: Double) -> Color { Color(red: r, green: g, blue: b) }

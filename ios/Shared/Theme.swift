import SwiftUI
import QuoteCore

/// Colours shared by the app and the widget.
extension WidgetTheme {
    var background: LinearGradient {
        switch self {
        case .sunrise:
            LinearGradient(
                colors: [Color(red: 1.00, green: 0.80, blue: 0.67),
                         Color(red: 0.98, green: 0.62, blue: 0.62),
                         Color(red: 0.76, green: 0.66, blue: 0.91)],
                startPoint: .topLeading, endPoint: .bottomTrailing)
        case .light:
            LinearGradient(colors: [Color(white: 0.99), Color(white: 0.95)],
                           startPoint: .top, endPoint: .bottom)
        case .dark:
            LinearGradient(colors: [Color(red: 0.12, green: 0.12, blue: 0.18),
                                    Color(red: 0.07, green: 0.07, blue: 0.10)],
                           startPoint: .top, endPoint: .bottom)
        }
    }

    var foreground: Color {
        switch self {
        case .sunrise: Color(red: 0.23, green: 0.12, blue: 0.16)
        case .light: Color(white: 0.12)
        case .dark: Color(white: 0.96)
        }
    }
}

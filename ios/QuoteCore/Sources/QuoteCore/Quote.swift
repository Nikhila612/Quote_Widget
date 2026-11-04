import Foundation

public struct Quote: Codable, Hashable, Identifiable, Sendable {
    public let id: String
    public let text: String
    public let author: String
    public let tags: [String]

    public init(id: String, text: String, author: String, tags: [String]) {
        self.id = id
        self.text = text
        self.author = author
        self.tags = tags
    }
}

public enum QuoteLibrary {
    /// Loads `quotes.json` from the given bundle (the app or widget bundle).
    public static func load(from bundle: Bundle = .main) -> [Quote] {
        guard let url = bundle.url(forResource: "quotes", withExtension: "json"),
              let data = try? Data(contentsOf: url),
              let quotes = try? JSONDecoder().decode([Quote].self, from: data),
              !quotes.isEmpty
        else { return [fallback] }
        return quotes
    }

    public static let fallback = Quote(
        id: "fallback",
        text: "Every day may not be good, but there is something good in every day.",
        author: "Unknown",
        tags: ["happiness"]
    )
}

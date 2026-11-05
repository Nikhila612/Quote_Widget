import Foundation

/// The daily check-in. Every answer is optional.
/// Must stay identical to android/core/.../Mood.kt — both share the same test vectors.
public struct CheckIn: Codable, Equatable, Sendable {
    public var feeling: Feeling?
    public var day: DayKind?
    public var wish: Wish?
    public var kind: QuoteKind?

    public init(feeling: Feeling? = nil, day: DayKind? = nil, wish: Wish? = nil, kind: QuoteKind? = nil) {
        self.feeling = feeling
        self.day = day
        self.wish = wish
        self.kind = kind
    }

    public var isEmpty: Bool { feeling == nil && day == nil && wish == nil && kind == nil }
}

/// "What's your mood right now?"
public enum Feeling: String, Codable, CaseIterable, Identifiable, Sendable {
    case happy, calm, tired, worried, sad, motivated
    public var id: String { rawValue }
    public var title: String { rawValue.capitalized }
    var tags: [String] {
        switch self {
        case .happy: ["happiness", "gratitude"]
        case .calm: ["calm", "gratitude"]
        case .tired: ["calm", "kindness"]
        case .worried: ["calm", "courage"]
        case .sad: ["kindness", "happiness", "courage"]
        case .motivated: ["growth", "courage"]
        }
    }
}

/// "How will your day be?"
public enum DayKind: String, Codable, CaseIterable, Identifiable, Sendable {
    case busy, relaxed, challenging, exciting
    public var id: String { rawValue }
    public var title: String { rawValue.capitalized }
    var tags: [String] {
        switch self {
        case .busy: ["calm", "growth"]
        case .relaxed: ["happiness", "gratitude"]
        case .challenging: ["courage", "growth"]
        case .exciting: ["happiness", "courage"]
        }
    }
}

/// "How do you want today to feel?"
public enum Wish: String, Codable, CaseIterable, Identifiable, Sendable {
    case peaceful, confident, joyful, productive, grateful, kind
    public var id: String { rawValue }
    public var title: String { rawValue.capitalized }
    var tag: String {
        switch self {
        case .peaceful: "calm"
        case .confident: "courage"
        case .joyful: "happiness"
        case .productive: "growth"
        case .grateful: "gratitude"
        case .kind: "kindness"
        }
    }
}

/// "What kind of quote today?"
public enum QuoteKind: String, Codable, CaseIterable, Identifiable, Sendable {
    case happiness, courage, calm, growth, kindness, gratitude, surprise
    public var id: String { rawValue }
    public var title: String { self == .surprise ? "Surprise me" : rawValue.capitalized }
    var tag: String? { self == .surprise ? nil : rawValue }
}

public enum MoodPicker {
    /// The six quote tags, in a fixed order used to break ties and to salt each tag's shuffle.
    public static let tags = ["happiness", "courage", "calm", "growth", "kindness", "gratitude"]

    /// The one tag that best fits the answers, or nil when there's nothing to go on.
    /// Weights: quote kind 3, wish 2, feeling 2 (shared across its tags), day 1 (shared).
    public static func tag(for checkIn: CheckIn) -> String? {
        var score: [String: Double] = [:]
        if let t = checkIn.kind?.tag { score[t, default: 0] += 3 }
        if let t = checkIn.wish?.tag { score[t, default: 0] += 2 }
        if let f = checkIn.feeling { for t in f.tags { score[t, default: 0] += 2.0 / Double(f.tags.count) } }
        if let d = checkIn.day { for t in d.tags { score[t, default: 0] += 1.0 / Double(d.tags.count) } }
        guard let best = score.values.max(), best > 0 else { return nil }
        // Ties go to the most direct answer, then the fixed tag order.
        let priority = [checkIn.kind?.tag, checkIn.wish?.tag].compactMap { $0 }
            + (checkIn.feeling?.tags ?? []) + (checkIn.day?.tags ?? []) + tags
        return priority.first { abs((score[$0] ?? 0) - best) < 1e-9 }
    }

    /// Picks from `candidates` (library indices carrying the tag, in library order).
    /// Walks the tag's own shuffled order from `uses`, skipping anything in `avoid`.
    /// Returns the chosen index and how many positions were consumed.
    public static func pick(candidates: [Int], tag: String, seed: UInt64, uses: Int, avoid: Set<Int>) -> (index: Int, consumed: Int)? {
        guard !candidates.isEmpty else { return nil }
        let salt = UInt64((tags.firstIndex(of: tag) ?? 0) + 1) &* 0x100_0000_01B3
        let order = QuotePicker.shuffledOrder(count: candidates.count, seed: seed ^ salt)
        for step in 0..<candidates.count {
            let index = candidates[order[(uses + step) % candidates.count]]
            if !avoid.contains(index) { return (index, step + 1) }
        }
        return (candidates[order[uses % candidates.count]], 1)
    }
}

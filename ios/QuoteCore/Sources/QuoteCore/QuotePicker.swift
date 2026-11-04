import Foundation

/// Deterministic daily pick. Must stay byte-for-byte identical to
/// android/core/src/main/java/.../QuotePicker.kt — both share the same test vectors.
public enum QuotePicker {

    /// Index into the quote list to show on day `dayIndex` (days since install).
    /// Every quote is shown once per cycle of `count` days before any repeat.
    public static func quoteIndex(dayIndex: Int, count: Int, seed: UInt64) -> Int {
        precondition(count > 0, "count must be positive")
        let day = max(0, dayIndex)
        let cycle = UInt64(day / count)
        let position = day % count
        return shuffledOrder(count: count, seed: seed &+ cycle)[position]
    }

    /// Fisher–Yates shuffle of 0..<count driven by SplitMix64.
    public static func shuffledOrder(count: Int, seed: UInt64) -> [Int] {
        var order = Array(0..<count)
        var rng = SplitMix64(seed: seed)
        var i = count - 1
        while i > 0 {
            let j = Int(rng.next() % UInt64(i + 1))
            order.swapAt(i, j)
            i -= 1
        }
        return order
    }
}

public struct SplitMix64 {
    private var state: UInt64

    public init(seed: UInt64) { state = seed }

    public mutating func next() -> UInt64 {
        state &+= 0x9E37_79B9_7F4A_7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return z ^ (z >> 31)
    }
}

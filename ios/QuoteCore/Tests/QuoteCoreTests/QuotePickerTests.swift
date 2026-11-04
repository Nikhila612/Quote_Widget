import XCTest
@testable import QuoteCore

final class QuotePickerTests: XCTestCase {

    // Shared vectors: android/core/src/test/.../QuotePickerTest.kt asserts the same values.
    func testShuffleMatchesSharedVectors() {
        XCTAssertEqual(QuotePicker.shuffledOrder(count: 10, seed: 42), [0, 9, 5, 8, 6, 4, 7, 2, 1, 3])
        XCTAssertEqual(QuotePicker.shuffledOrder(count: 10, seed: 43), [4, 2, 5, 6, 1, 3, 9, 8, 7, 0])
        XCTAssertEqual(QuotePicker.shuffledOrder(count: 10, seed: 0), [6, 3, 2, 9, 8, 1, 4, 7, 0, 5])
    }

    func testNoRepeatWithinACycle() {
        let count = 36
        for cycle in 0..<3 {
            let picks = (0..<count).map {
                QuotePicker.quoteIndex(dayIndex: cycle * count + $0, count: count, seed: 7)
            }
            XCTAssertEqual(Set(picks).count, count, "cycle \(cycle) repeated a quote")
        }
    }

    func testSecondCycleUsesNextSeed() {
        XCTAssertEqual(QuotePicker.quoteIndex(dayIndex: 10, count: 10, seed: 42), 4)
    }

    func testNegativeDayClampsToZero() {
        XCTAssertEqual(
            QuotePicker.quoteIndex(dayIndex: -5, count: 10, seed: 42),
            QuotePicker.quoteIndex(dayIndex: 0, count: 10, seed: 42))
    }

    func testStoreChangesQuoteAtMidnight() throws {
        let defaults = try XCTUnwrap(UserDefaults(suiteName: "QuoteCoreTests"))
        defaults.removePersistentDomain(forName: "QuoteCoreTests")
        let quotes = (0..<5).map { Quote(id: "q\($0)", text: "Quote \($0)", author: "A", tags: []) }
        let store = QuoteStore(quotes: quotes, defaults: defaults)

        let calendar = Calendar.current
        let today = calendar.startOfDay(for: .now)
        let lateToday = today.addingTimeInterval(23 * 3600)
        let tomorrow = try XCTUnwrap(calendar.date(byAdding: .day, value: 1, to: today))

        XCTAssertEqual(store.dayIndex(for: lateToday), 0)
        XCTAssertEqual(store.dayIndex(for: tomorrow), 1)
        XCTAssertEqual(store.quote(for: today), store.quote(for: lateToday))
        XCTAssertEqual(Set((0..<5).map { store.quote(for: calendar.date(byAdding: .day, value: $0, to: today)!) }).count, 5)
    }
}

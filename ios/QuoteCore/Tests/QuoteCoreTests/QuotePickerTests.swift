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

    // Shared vectors: Android QuotePickerTest.dailyPickMatchesSharedVectors asserts the same values.
    func testDailyPickMatchesSharedVectors() {
        func pick(_ day: Int, custom: Int = 3) -> DailyPick {
            QuotePicker.dailyPick(dayIndex: day, libraryCount: 10, customCount: custom, seed: 42)
        }
        XCTAssertEqual(pick(5), .library(4))
        XCTAssertEqual(pick(6), .custom(0))
        XCTAssertEqual(pick(13), .custom(1))
        XCTAssertEqual(pick(20), .custom(2))
        XCTAssertEqual(pick(27), .custom(0))
        XCTAssertEqual(pick(6, custom: 0), .library(7))
    }

    func testCustomQuotesJoinRotation() throws {
        let defaults = try XCTUnwrap(UserDefaults(suiteName: "QuoteCoreCustomTests"))
        defaults.removePersistentDomain(forName: "QuoteCoreCustomTests")
        let quotes = (0..<5).map { Quote(id: "q\($0)", text: "Quote \($0)", author: "A", tags: []) }
        let store = QuoteStore(quotes: quotes, defaults: defaults)

        let mine = try XCTUnwrap(store.saveCustomQuote(text: "  I can do hard things. ", author: ""))
        XCTAssertEqual(mine.text, "I can do hard things.")
        XCTAssertEqual(mine.author, QuoteStore.defaultCustomAuthor)
        XCTAssertNil(store.saveCustomQuote(text: "   ", author: "Me"))

        let day6 = try XCTUnwrap(Calendar.current.date(byAdding: .day, value: 6, to: .now))
        XCTAssertEqual(store.quote(for: day6), mine)

        _ = store.saveCustomQuote(text: "I am enough.", author: "Me", id: mine.id)
        XCTAssertEqual(store.customQuotes.count, 1)
        XCTAssertEqual(store.quote(for: day6).text, "I am enough.")

        store.toggleFavourite(store.customQuotes[0])
        XCTAssertEqual(store.favourites.map(\.id), [mine.id])

        store.deleteCustomQuote(id: mine.id)
        XCTAssertTrue(store.customQuotes.isEmpty)
        XCTAssertTrue(store.favourites.isEmpty)
        XCTAssertTrue(quotes.contains(store.quote(for: day6)))
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

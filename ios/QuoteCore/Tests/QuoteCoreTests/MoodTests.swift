import XCTest
@testable import QuoteCore

final class MoodTests: XCTestCase {

    // Shared vectors: android/core/src/test/.../MoodTest.kt asserts the same values.
    func testTagMatchesSharedVectors() {
        XCTAssertEqual(MoodPicker.tag(for: CheckIn(feeling: .worried)), "calm")
        XCTAssertEqual(MoodPicker.tag(for: CheckIn(feeling: .tired, day: .busy)), "calm")
        XCTAssertEqual(MoodPicker.tag(for: CheckIn(feeling: .sad, day: .exciting, wish: .productive)), "growth")
        XCTAssertEqual(MoodPicker.tag(for: CheckIn(feeling: .motivated, kind: .gratitude)), "gratitude")
        XCTAssertNil(MoodPicker.tag(for: CheckIn(kind: .surprise)))
        XCTAssertEqual(MoodPicker.tag(for: CheckIn(day: .challenging)), "courage")
        XCTAssertNil(MoodPicker.tag(for: CheckIn()))
    }

    func testPickMatchesSharedVectors() {
        let candidates = [3, 8, 11, 15, 21, 30, 42]
        XCTAssertEqual(MoodPicker.pick(candidates: candidates, tag: "calm", seed: 42, uses: 0, avoid: [])?.index, 42)
        XCTAssertEqual(MoodPicker.pick(candidates: candidates, tag: "calm", seed: 42, uses: 2, avoid: [])?.index, 30)
        let skipped = MoodPicker.pick(candidates: candidates, tag: "calm", seed: 42, uses: 0, avoid: [42])
        XCTAssertEqual(skipped?.index, 3)
        XCTAssertEqual(skipped?.consumed, 2)
        XCTAssertEqual(MoodPicker.pick(candidates: candidates, tag: "courage", seed: 42, uses: 0, avoid: [])?.index, 11)
    }

    func testCheckInPicksMatchingQuoteAndKeepsHistory() throws {
        let defaults = try XCTUnwrap(UserDefaults(suiteName: "MoodTests"))
        defaults.removePersistentDomain(forName: "MoodTests")
        let tags = MoodPicker.tags
        let quotes = (0..<60).map { Quote(id: "q\($0)", text: "Quote \($0)", author: "A", tags: [tags[$0 % tags.count]]) }
        let store = QuoteStore(quotes: quotes, defaults: defaults)
        let today = Date.now

        XCTAssertTrue(store.needsCheckIn(on: today))
        let picked = store.checkIn(CheckIn(feeling: .worried), on: today)
        XCTAssertEqual(picked.tags, ["calm"])
        XCTAssertEqual(store.quote(for: today), picked, "widget and app see the check-in pick")
        XCTAssertFalse(store.needsCheckIn(on: today))
        XCTAssertEqual(store.checkIn(on: today), CheckIn(feeling: .worried))

        // Checking in again gives a different quote of the new mood.
        let second = store.checkIn(CheckIn(kind: .gratitude), on: today)
        XCTAssertEqual(second.tags, ["gratitude"])
        XCTAssertEqual(store.quote(for: today), second)

        // Yesterday is untouched.
        let yesterday = try XCTUnwrap(Calendar.current.date(byAdding: .day, value: -1, to: today))
        XCTAssertFalse(store.quote(for: yesterday).id.isEmpty)

        // "Surprise me" alone falls back to the normal daily quote.
        let fresh = try XCTUnwrap(UserDefaults(suiteName: "MoodTests2"))
        fresh.removePersistentDomain(forName: "MoodTests2")
        let other = QuoteStore(quotes: quotes, defaults: fresh)
        let normal = other.quote(for: today)
        XCTAssertEqual(other.checkIn(CheckIn(kind: .surprise), on: today), normal)
    }

    func testSkipStopsTheDailyPrompt() throws {
        let defaults = try XCTUnwrap(UserDefaults(suiteName: "MoodSkip"))
        defaults.removePersistentDomain(forName: "MoodSkip")
        let store = QuoteStore(quotes: [QuoteLibrary.fallback], defaults: defaults)
        store.skipCheckIn()
        XCTAssertFalse(store.needsCheckIn())
        let tomorrow = try XCTUnwrap(Calendar.current.date(byAdding: .day, value: 1, to: .now))
        XCTAssertTrue(store.needsCheckIn(on: tomorrow))
    }
}

final class ShowTodayTests: XCTestCase {
    func testOwnQuoteCanBeShownTodayAndCheckInReplacesIt() throws {
        let defaults = try XCTUnwrap(UserDefaults(suiteName: "ShowToday"))
        defaults.removePersistentDomain(forName: "ShowToday")
        let tags = MoodPicker.tags
        let quotes = (0..<30).map { Quote(id: "q\($0)", text: "Quote \($0)", author: "A", tags: [tags[$0 % tags.count]]) }
        let store = QuoteStore(quotes: quotes, defaults: defaults)
        let mine = try XCTUnwrap(store.saveCustomQuote(text: "I can do this.", author: ""))

        store.showToday(mine)
        XCTAssertEqual(store.quote(), mine)
        XCTAssertEqual(store.shownTodayID(), mine.id)
        XCTAssertFalse(store.isMoodPick())

        // A check-in later today replaces it.
        let picked = store.checkIn(CheckIn(feeling: .calm))
        XCTAssertNotEqual(picked, mine)
        XCTAssertNil(store.shownTodayID())
        XCTAssertTrue(store.isMoodPick())

        // Deleting a quote that is on today's widget takes it off.
        store.showToday(mine)
        store.deleteCustomQuote(id: mine.id)
        XCTAssertNotEqual(store.quote().id, mine.id)
    }
}

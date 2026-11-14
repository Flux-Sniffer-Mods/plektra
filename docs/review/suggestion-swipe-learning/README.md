# Suggestion swipes that learn

Two options under the native trackpad's swipe distances (Settings → Advanced → Trackpad gestures, with the sensitivity settings), both off by default. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026), where they are on.

## Behaviour

- **Learn from your swipes**: a swipe-up pick undone straight away (Backspace, or the swipe that deletes a word, within 3 seconds) was a scroll taken for a pick, so swipes need to go 8% further; a swipe up that got at least 60% of the way, followed within 2.5 seconds by a pick, was a pick that fell short, so they need 5% less. The learning is a scale on your own distance, kept between 0.6 and 1.8 of it. **Forget what was learned** sets it back to your setting.
- **Swipes scroll after a pause**: after the chosen number of seconds without typing (1 to 30; 0, the default, means always), a swipe up picks nothing, so scrolling through a page with the trackpad doesn't pick a suggestion. Typing a key or picking makes swipes pick again.
- Only the native trackpad provider; the Shizuku provider is unchanged.

## Implementation boundaries

- `core/SuggestionSwipeLearning` (ported, with both defaults off) holds the switches, the learned scale and the timing. Its three preferences are registered in `BackupContract`.
- `PhysicalKeyboardInputMethodService`: the native suggestion swipe distance is the setting times the scale; the swipe-up decision asks whether swipes pick, reports a pick, and reports a swipe that fell short; the delete-word swipe and Backspace report a delete; other keys report typing.
- New: two settings-search links (`trackpad.swipe_learning`, `trackpad.swipe_idle`, which open the sensitivity settings); seven strings in their own file with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*SuggestionSwipeLearningTest'
```

`SuggestionSwipeLearningTest` (in `devices-layouts`): off by default with swipes always picking; an undone pick raising the scale and a late Backspace not; a near miss before a pick lowering it and a short swipe not; the bounds, forgetting and turning it off; and swipes scrolling after a pause until a key is typed.

## Validation

Run on 14 November 2026 against source commit `5b62c16e` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,422 tests (upstream alone: 185 classes, 1,417 tests). Of these, 5 are new, in `SuggestionSwipeLearningTest`. One failure, `EmojiPagesSurfacesTest.aCategoryTabTappedWhileSearchingShowsThatCategorysResults`: a race in that upstream test, unrelated to this change. The class passed when rerun on its own; `flux/test-races` fixes the race.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `devices-layouts` topic, with the trackpad tests.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.
- The first run found a fault in the ported code: a pick within 2.5 seconds of the keyboard starting counted as a pick after a near miss (no near miss had happened). Fixed here by checking a near miss was recorded.

## Device check (still to do)

Not yet run on a device with a native trackpad. Suggested check: turn on Learn from your swipes; pick a suggestion with a swipe up and press Backspace at once, a few times; swipes should then need a little further. Set Swipes scroll after a pause to 5 s, wait without typing and swipe up: nothing should be picked until a key is typed.

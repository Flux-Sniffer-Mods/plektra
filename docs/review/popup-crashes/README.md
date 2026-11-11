# Popups that outlive the keyboard window, and popups on very narrow keyboards

Two ways a popup could end the keyboard, fixed together because both are in popup placement. Ported from Flux Keyboard's stability work onto upstream `68ace8ab` (10 November 2026).

## The keyboard's window is gone

The snippet and shortcode popup (`TextExpansionPopup`) and the emoji variants popup (`EmojiPickerView`) are shown from callbacks that can run after the keyboard's window has closed: the keyboard hidden mid-word, or closed while a variant was being pressed. `PopupWindow.showAtLocation` and `showAsDropDown` then fail with `WindowManager.BadTokenException`, which ends the keyboard. Both now return when the anchor is no longer attached to a window, and log a `BadTokenException` that still races in rather than letting it through.

## Very narrow keyboards

Popup positions were clamped with `coerceIn(0, width - popupWidth)`, which throws `IllegalArgumentException` when the popup is wider than the keyboard (an empty range). The long-press alternates popup sizes its columns to fit, so in Plektra this is reached only with very narrow geometry: a key preview (at least 52dp) on a keyboard narrower than that, a layer row with more alternates than pixels across, or a bubble narrower than its corner radius. Each clamp now uses `maxOf(0, …)`, so such a popup starts at the left edge. An empty layer row selects nothing instead of clamping to `lastIndex == -1`.

## Implementation boundaries

- Five one-line clamp changes in `AospKeyboardView`, one in `EmojiPickerView`, and an attached-window check with a narrow `BadTokenException` catch in `TextExpansionPopup.show()` and the emoji variants popup. Popup sizes, positions on normal keyboards and behaviour are unchanged.
- No preference, string, permission or resource changes.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*TextExpansionPopupTest' --tests '*AospKeyboardViewTest'
```

`TextExpansionPopupTest` (now Robolectric) shows the popup over an anchor that was never attached to a window and checks that nothing is shown. `AospKeyboardViewTest` presses a key on a 40px keyboard, narrower than the key preview, and checks that the preview opens at the left edge.

## Validation

Run on 11 November 2026 against source commit `a4beefb1` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 185 classes, 1,419 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 1 in `TextExpansionPopupTest`, 1 in `AospKeyboardViewTest`.
- The new tests on upstream alone, without the fix: both fail. `anAnchorWithoutAWindowShowsNothingInsteadOfCrashing` with an `AssertionError`, and `keyPreviewWiderThanAVeryNarrowKeyboard_startsAtItsEdgeInsteadOfCrashing` with `IllegalArgumentException: Cannot coerce value to an empty range: maximum -12.0 is less than minimum 0.0`.
- One earlier full run on this commit had a single failure elsewhere, `ClipboardHistoryTest.startupShowsLoadingAndBlocksHistoryActionsUntilReady`, in code this change does not touch. The clipboard list is a `ListAdapter`, which diffs on a background thread once a list has been shown, and the test checks the view after idling the main looper only, so it can look before the diff lands. It passed three times on its own here and three times on upstream, and in the full run above.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check: with snippets set to the floating popup, type a snippet prefix and hide the keyboard (Back) at once, repeatedly; on the emoji page, long-press an emoji with skin tones and press Back while holding it. The keyboard should keep running in both cases.

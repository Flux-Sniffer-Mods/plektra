# Smart toggle: Alt and Ctrl switch themselves off by context

Two opt-in switches in **Modifiers** for a latched modifier that has done its job. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

**Smart toggle: Alt off after an opening** (in the Alt section, after Alt latch stays on space). With Alt latched or locked, typing an opening quote or bracket with Alt switches Alt off, so the words inside come out as letters: `(` `[` `{` `<` `“` `‘` `«` `‹` `„` `‚` `¿` `¡` `「` `『` `（`. A straight quote (`"` `'` `` ` ``) counts as opening only after a space, the start of the text or another opener; after a word it closes, and Alt stays.

**Smart toggle: Ctrl off after a shortcut** (under Ctrl tap latches, shown while that is on). A tap-latched Ctrl switches off after one shortcut such as copy, paste, undo or select all. Cursor and selection moves keep it on, since they are pressed again and again: Ctrl mappings to the arrows, Page Up and Down, Home, End and Tab, and the `move_*`, `expand_selection*`, `page_start` and `page_end` actions.

Never affected: Ctrl held down, Nav Mode's Ctrl, and a double-tap Ctrl lock (it only exists while Ctrl tap latches is off, and the switch then does nothing). Both switches are off by default; off, everything works as before.

## Implementation boundaries

- `core/SmartModifierToggle` holds the two pure checks: whether Alt's text opens a quote or bracket, and whether a Ctrl mapping is a cursor or selection move.
- `InputEventRouter` calls them right after `handleAltModifiedKey` and `handleCtrlModifiedKey` have consumed a key, and clears the latch through `ModifierStateController` as Alt on space already does. The Ctrl check also requires Ctrl tap latches, so a double-tap lock is never ended.
- New: two preferences (`smart_alt_off_after_opening`, `smart_ctrl_off_after_shortcut`, default false), registered in `BackupContract`; two settings-search links (`modifiers.smart_alt_off`, and `modifiers.smart_ctrl_off`, which falls back to Ctrl tap latches while that is off); four strings in their own file (`smart_modifier_toggle.xml`) with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*InputEventRouterModifierE2ETest' --tests '*BackupPreferenceContractIntegrationTest' --tests '*SettingLinkCoverageTest'
```

Four new cases in `InputEventRouterModifierE2ETest` route keys through the real router and modifier state: a latched Alt typing `(` switches off; with the switch off it stays, and a `"` after a word keeps it too; a tap-latched Ctrl stays after Ctrl+S (cursor left) and switches off after Ctrl+A (select all); and a Ctrl lock with tap latches off is never ended.

## Validation

Run on 11 November 2026 against source commit `568a6448` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 185 classes, 1,421 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 4 in `InputEventRouterModifierE2ETest`. `BackupPreferenceContractIntegrationTest` and `SettingLinkCoverageTest` pass with the new preferences and links.
- An earlier full run on this commit had one failure elsewhere, `EmojiPagesSurfacesTest.aCategoryTabTappedWhileSearchingShowsThatCategorysResults`, a test that races the picker's background recents reload (`flux/test-races` fixes the wait); it passed in the full run above.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`; the new strings have all ten translations.

## Device check (still to do)

Not yet run on a device. Suggested check on a keyboard with an Alt layer: turn on Smart toggle: Alt off after an opening, lock Alt, type a digit, then Alt's `(`, then letters; they should come out as letters. Turn on Ctrl tap latches and Smart toggle: Ctrl off after a shortcut, tap Ctrl, press the cursor-left mapping twice (Ctrl stays), then C to copy (Ctrl goes off).

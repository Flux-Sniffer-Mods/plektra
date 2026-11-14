# Automatic Shift by kind of field

**Shift in all text fields** (an on/off switch under Automatic capitalization) becomes **Automatic Shift**: a row listing the kinds of field that start with Shift, and a dialog to tick them. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

| Kind | How it is told | Until chosen |
| --- | --- | --- |
| Messages, notes and other text | any other text field | on |
| Names | `TYPE_TEXT_VARIATION_PERSON_NAME` | on |
| Postal addresses | `TYPE_TEXT_VARIATION_POSTAL_ADDRESS` | on |
| Search bars | `FILTER`, or a text field whose action is Search | off |
| Links and web addresses | `URI` | off |
| Email addresses | `EMAIL_ADDRESS`, `WEB_EMAIL_ADDRESS` | off |
| Other fields (no input type) | `TYPE_NULL` | off |

- Someone who had **Shift in all text fields** on starts with search, links and email ticked too, so nothing changes for them until they choose.
- A kind that isn't ticked gets no automatic Shift, even when the app asks for capitals (`CAP_SENTENCES`, `CAP_WORDS`). An app asking for all capitals (`CAP_CHARACTERS`) is handled as before.
- Password fields, Exact typing apps and literal-entry fields never get automatic Shift, as before.
- Two defaults differ from before: text fields whose action is Search and editors with no input type no longer start with Shift. The second matters with a hardware keyboard: apps report a type-less editor when nothing is focused, where letters are the app's own shortcuts.

## Implementation boundaries

- `inputmethod/ShiftFieldTypes` tells the kinds apart and holds the choice (`auto_shift_field_types`, comma-separated ids, unset until chosen; registered in `BackupContract`). The former switch's preference is kept and only read for the default.
- `PhysicalKeyboardInputMethodService.shouldDisableAutoCapitalize` asks `ShiftFieldTypes` after the existing opt-outs (layout without capitals, Exact typing, password, literal entry). `AutoCapitalizeHelper.maybeEnableSmartShift` checks it before the app's capitalization flags.
- The settings link `text_input.auto_capitalize_restricted_fields` now finds the new row. New strings in their own file with all ten translations; the former switch's two strings are removed.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*ShiftFieldTypesTest' --tests '*PhysicalKeyboardInputMethodServiceDeviceBehaviorTest'
```

`ShiftFieldTypesTest` (in `editing`): each kind is told apart, passwords and numbers are never shifted, the defaults with and without the former switch, and a choice kept over it. In `PhysicalKeyboardInputMethodServiceDeviceBehaviorTest`, `autoCap_fieldTypes_chooseWhichKindsStartWithShift` focuses real fields: an email field starts with Shift once Email is ticked, a text field asking for capitals gets none while Messages isn't, and does once it is. The existing tests for the former switch pass unchanged.

## Validation

Run on 11 November 2026 against source commit `46f134fe` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,422 tests (upstream alone: 185 classes, 1,417 tests). New: 4 in `ShiftFieldTypesTest`, 1 in `PhysicalKeyboardInputMethodServiceDeviceBehaviorTest`; `SettingLinkCoverageTest` and `BackupPreferenceContractIntegrationTest` pass with the moved link and the new preference. One failure, `ClipboardHistoryTest.startupShowsLoadingAndBlocksHistoryActionsUntilReady`: a race in that upstream test (the history list is diffed on a background thread), unrelated to this change. The class passed when rerun on its own; `flux/test-races` fixes the race.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `editing` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,054 errors, no new errors against `ci/lint-errors.json`. Two fewer than upstream: the former switch's two strings, which lacked translations (`MissingTranslation`), are removed.

## Device check (still to do)

Not yet run on a device. Suggested check: in Smart Features, open Automatic Shift and confirm the defaults; type in a browser's address bar and a search bar (no capital), and in a message (capital). Tick Email addresses and confirm an email field starts with Shift.

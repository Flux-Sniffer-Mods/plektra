# Remember emails and phone numbers

Emails and phone numbers typed by hand are kept in the user dictionary and offered again as chips in email and phone fields; a tap fills the field. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- **Remember emails and phone numbers**, in Auto-correction after Exact typing (on by default).
- Learned: in an email or phone field, the whole entry when it ends (Enter, Tab, leaving the field); elsewhere, an email or a number written without spaces as its word ends. Only what was typed by hand: an entry longer than the keys typed in that field (pasted or filled in) is never kept. Never from password fields, nor from fields that ask for no learning (`IME_FLAG_NO_PERSONALIZED_LEARNING`), where saved ones are still offered.
- Offered: up to three saved ones that go on from what is typed, most recently used first, as chips in the bar; a tap replaces what was typed and marks it used. Emails match ignoring case, numbers ignoring their spacing.
- Saved ones are ordinary user dictionary entries: they can be seen and removed there, and go with its backups.

## Implementation boundaries

- `core/ContactDetails` is pure logic: what counts as an email or a number (7 to 15 digits), what to learn from a field and how much of it was typed, whether it is saved already, and which saved ones match.
- `PhysicalKeyboardInputMethodService` counts keys typed in the field, learns on Enter, Tab, Space (emails and unspaced numbers) and `onFinishInput`, and refreshes the chips on field start and selection changes through `CandidatesBarController.showExpansionSuggestions`. It uses the existing `UserDictionaryStore`.
- Typing is counted from hardware key events; text from the on-screen keyboard isn't counted yet, so it isn't learned (saved ones are still offered).
- New: one preference (`learn_contact_details`, default true) in `BackupContract`; a settings-search link (`auto_correction.learn_contact_details`); two strings in their own file with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*ContactDetailsTest'
```

`ContactDetailsTest` (in `suggestions`): emails and numbers recognised, only what was typed by hand learned, in ordinary text only the word just typed, an email field learning only emails, and saved details matched and not saved twice.

## Validation

Run on 11 November 2026 against source commit `87bffd42` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,422 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 5 in `ContactDetailsTest`; `SettingLinkCoverageTest` and `BackupPreferenceContractIntegrationTest` pass with the new link and preference.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `suggestions` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check with a hardware keyboard: type an email address in an app's email field and press Enter; in another email field type its first letters and confirm the chip, then tap it. Paste an address instead and confirm it isn't learned.

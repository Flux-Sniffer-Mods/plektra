# Search bars wait for typing

A switch in Settings → Text input, under the keyboard's showing settings: **Search bars wait for typing** (off by default). When an app opens with its search bar already focused (a launcher, a maps or music app, a settings search), Plektra doesn't bring its bar or keyboard up by itself; the first key or a tap on the search bar does. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- Only the first text field of an app as it comes to the front, and only a search bar: a filter field (`TYPE_TEXT_VARIATION_FILTER`) or a text field whose action is Search. Going to the search bar of an app already in use shows the bar as before, and so do Plektra's own screens.
- What brings it up: any key (the existing recovery on a hardware key), a tap on the field, or the app asking Android to show the keyboard; Plektra only skips its own request.
- Off: as before.

## Implementation boundaries

- `inputmethod/SearchBarWait` holds the switch (`search_bar_waits_for_typing`, registered in `BackupContract`) and tells a search bar from its `EditorInfo`.
- `PhysicalKeyboardInputMethodService.onStartInput`: where Plektra decides how to show itself for a new field, a waiting search bar comes first and requests nothing (keypad included). Flux Keyboard hides the bar again after showing it; here it is never requested, so nothing flashes and the visibility controller keeps its usual state.
- New: a settings-search link (`text_input.search_bar_waits`); two strings in their own file with all ten translations. No permission, route or migration.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*SearchBarWaitTest'
```

`SearchBarWaitTest` (in `ime-integration`) counts Plektra's own show requests through the visibility controller, as `SoftwareKeypadServiceIntegrationTest` does: what counts as a search bar; off, the bar comes up as before; on, none as the app opens and one with the first key; and none skipped for another field of the same app, its search bar gone to later, or Plektra's own screens.

## Validation

Run on 13 November 2026 against source commit `fd3fe087` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,421 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 4 in `SearchBarWaitTest`; `SettingLinkCoverageTest` and `BackupPreferenceContractIntegrationTest` pass with the new link and preference.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `ime-integration` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check, with the switch on and a hardware keyboard: open an app that opens on its search bar (a launcher's app search, the Play Store's search): the bar should not appear until you type a letter, which goes into the search. Open a messaging app's text field and confirm the bar appears at once.

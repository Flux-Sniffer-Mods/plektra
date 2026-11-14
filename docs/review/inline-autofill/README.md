# Autofill in the suggestion bar

A switch in Auto-correction: **Autofill in the suggestion bar** (Android 11 and later, off by default). Password managers and Android's autofill offer their chips (saved logins, one-time codes, addresses) to the keyboard, which shows them in the middle of the suggestion bar; tapping one fills the field. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026), where it is on by default; here it is off until it has been checked on devices.

## Behaviour

- On, the keyboard answers Android's inline suggestions request; the password manager decides which fields get chips.
- The chips take the place of the suggestions, between the bar's menu buttons, each on a suggestion button's background in the theme's colours. They scroll when they don't fit, and are kept back while symbols or the clipboard are open.
- They leave when the field is finished or the password manager sends none. Chips still inflating for a field that has since been left are dropped.
- Off, or below Android 11: no request is answered and the row isn't shown.

## Implementation boundaries

- `inputmethod/InlineAutofill` builds the request (chip style from the bar's colours and height) and inflates the chips; `InlineAutofillSettings` holds the switch (`inline_autofill_enabled`, registered in `BackupContract`) and its settings-search link (`auto_correction.inline_autofill`).
- `FullSuggestionsBar` gains a centre host shown in place of the suggestions (`centerAccessoryHost` / `setCenterAccessoryActive`); while it is active the bar stays visible without suggestions.
- `inputmethod/InlineAutofillStrip` puts the chips in that host; `StatusBarController` and `CandidatesBarController` pass them to the bar on screen.
- The keyboard service overrides `onCreateInlineSuggestionsRequest` and `onInlineSuggestionsResponse` and clears the chips in `onFinishInput`.
- `res/xml/method.xml` declares `supportsInlineSuggestions`; the `androidx.autofill:autofill:1.1.0` dependency provides the chip style.
- `inputmethod/InlineAutofillSetting` is the switch's row; two strings in their own file with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*InlineAutofillTest'
```

`InlineAutofillTest` (in `suggestions`): off by default and backed up; chips replacing the suggestions and leaving with their clear; chips shown where there are no suggestions; chips held back leaving the bar to the suggestions; and the service answering no request while the switch is off.

## Validation

Run on 14 November 2026 against source commit `4ae9c99b` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,422 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). Of these, 5 are new, in `InlineAutofillTest`.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `suggestions` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

This needs a device most of all: the chips are drawn by the password manager in its own surface, which tests can't show. Suggested check on Android 11 or later: choose a password manager that offers inline suggestions (Google Password Manager, Bitwarden or 1Password), turn the switch on, and open a login form. A chip with the saved login should appear in the bar; tapping it should fill the field. Check the chip is readable in light and dark themes, then turn the switch off and check no chip appears.

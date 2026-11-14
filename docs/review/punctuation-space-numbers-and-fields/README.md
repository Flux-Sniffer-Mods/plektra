# Automatic spaces after punctuation: not inside numbers, addresses or passwords

With `,` `.` or `:` ticked under **Before next text** in **Punctuation spacing** (Smart Features, Spacing & punctuation; none by default), the deferred space went in before whatever came next. Numbers, times and decimals broke apart, and so did email addresses, links and passwords. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

| Typed, with `,` `.` `:` ticked | Before this change | Now |
| --- | --- | --- |
| `1,000` `12:30` `3.14` | `1, 000` `12: 30` `3. 14` | `1,000` `12:30` `3.14` |
| `Yes,no` `Note:5 items` `Done.Then` | `Yes, no` `Note: 5 items` `Done. Then` | unchanged |
| `jane.doe@mail.com` in an email field | `jane. doe@mail. com` | `jane.doe@mail.com` |
| A link, a password, a filter | spaces inside | no automatic spaces |
| A number, phone or date field | spaces inside | no automatic spaces |
| A single-line field hinted or named "Email or username", "Login", "Website"… | spaces inside | no automatic spaces |
| A multi-line message hinted "Compose email" | spaces | unchanged: spaces |

A digit gets no space only when the punctuation itself followed a digit, so `Note:5 items` still gets its space.

## Implementation boundaries

- `DeferredPunctuationSpaceTracker` remembers which punctuation its space is pending after, skips the space when a digit follows `<digit><punctuation>`, and gains `startField(EditorInfo?)`, called from `onStartInput` in place of `clear()`. `appliesTo()` turns the tracker off for email, web address, password and filter variations, for the number, phone and date classes, and for single-line text fields whose hint or field name contains `email`, `e-mail`, `username`, `user name`, `login`, `sign in`, `url` or `website`. Editors that report no type (`TYPE_NULL`) keep the spaces, as before.
- **Space after comma**, a separate switch that adds its space at once, is unchanged.
- No preference, string or resource changes.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*DeferredPunctuationSpaceTrackerTest'
```

Six new cases in `DeferredPunctuationSpaceTrackerTest` type character by character through the tracker: numbers, times and decimals; words after the same marks, which keep their spaces; email, link and password fields; a single-line field known by its hint; a multi-line body whose hint mentions email; and a number field followed by a text field. The fake input connection now answers `getTextBeforeCursor` from what was typed.

## Validation

Run on 11 November 2026 against source commit `11917460` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 185 classes, 1,423 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 6 in `DeferredPunctuationSpaceTrackerTest`.
- On upstream alone, without the fix, the numbers case gives `1, 000 at 12: 30 is 3. 14`. The field cases need `startField`, which upstream doesn't have; there every field gets the deferred space.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check: in Punctuation spacing, tick `,` `.` and `:` under Before next text. In a message, type `1,000 at 12:30 is 3.14.Then` and confirm only `. Then` gets a space. In a browser's sign-in form, type an email address and a password with dots; neither should get spaces.

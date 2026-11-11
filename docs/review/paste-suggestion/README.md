# Paste suggestion

Text copied in the last minute is offered as a chip in the next text field: `⎘ ` and the text on one short line; a tap pastes it, typing dismisses it, and each copy is offered once. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- On by default (Advanced, before the clipboard retention settings), as in other keyboards.
- A copy marked sensitive by its app (a password manager's) is offered only in password fields. **In password fields too** (on by default, shown while the suggestion is on) offers any recent copy there as `⎘ ••••••••`, the same length whatever was copied; the text itself is never shown.
- The copy is kept in memory only, by its own clipboard listener: whether clipboard history is on or off, nothing goes into it from here, and a sensitive copy never does.
- Long text is cut to one line of about 24 characters without splitting an emoji or an accent.

## Implementation boundaries

- `clipboard/RecentCopyTracker` listens to the clipboard while the keyboard runs (started in `onCreate`, stopped in `onDestroy`) and keeps the last copy with its time and sensitivity. It doesn't touch `ClipboardHistoryManager`.
- `clipboard/PasteSuggestion.chipFor()` decides what to offer (or nothing) from the copy, the time, the field and the setting; `label()` makes the one-line text.
- `PhysicalKeyboardInputMethodService` offers it in `onStartInputView` for a new field through `CandidatesBarController.showExpansionSuggestions`, and clears it on the next typed key or when it is used.
- New: two preferences (`paste_suggestion_enabled`, `paste_suggestion_password_fields`, both default true) in `BackupContract`; two settings-search links; four strings in their own file with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*PasteSuggestionTest' --tests '*RecentCopyTrackerTest'
```

`PasteSuggestionTest`: labels (whole, cut, never inside an emoji or a flag, the masked one) and `chipFor` (the one-minute window, sensitive copies only masked in password fields, the password-field switch). `RecentCopyTrackerTest` uses Robolectric's clipboard: a copy is kept until used, a password manager's is marked sensitive and its text never appears in `toString()`, and blank copies or copies after `stop()` are not kept. Both are in the `suggestions` topic.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: copy a sentence in one app, open a message field in another within a minute, and tap the `⎘` chip; copy again and type a letter instead (the chip goes). Copy a password from a password manager and open a password field: the chip shows `⎘ ••••••••` and pastes it; in a message field it isn't offered.

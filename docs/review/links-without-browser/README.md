# Links on a phone without a browser

Seven places started an `ACTION_VIEW` intent (or the input method settings) without a guard, so on a phone with no app for the link (no browser, a browser disabled, a de-Googled phone without the Play Store) Android threw `ActivityNotFoundException`, which closed the settings screen or ended the keyboard. Ported from Flux Keyboard's stability work onto upstream `68ace8ab` (10 November 2026).

## Behaviour

When no app can open the link, a short message, "No app can open this link", appears and the screen or keyboard stays. With an app for the link, nothing changes.

| Where | Link |
| --- | --- |
| Settings, support row | `pkb.rocks/donate` |
| Update dialog | The release page |
| Trackpad gestures | Shizuku's page |
| Typing sounds | The sound pack docs |
| What's new page | The release docs |
| Clicks keyboard profile | The Clicks companion app on the Play Store |
| Keyboard, voice input | The voice input app's download page, or the input method settings |

The About screen and the online layout editor link already catch failures; they are left as they are.

## Implementation boundaries

- One small helper, `ExternalLinks.start(context, intent)`: starts the intent and returns true, or catches `ActivityNotFoundException`, logs it, shows the message and returns false. Each of the seven sites calls it in place of `startActivity`, with the same intent and flags.
- New: one string, `external_link_no_app`, in its own file (`external_links.xml`) with all ten translations. No preference, permission or route.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*ExternalLinksTest'
```

`ExternalLinksTest` makes Robolectric check for an activity that handles the intent, as a phone without a browser would: with none, `start` returns false, shows the message and starts nothing; with an app registered for `https` links, it starts it with the link.

## Validation

Run on 11 November 2026 against source commit `c0087d48` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,419 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 2 in `ExternalLinksTest`.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `ui-settings` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`; the new string has all ten translations.

## Device check (still to do)

Not yet run on a device. Suggested check: disable the browser (Settings, Apps, the browser, Disable) or use a work profile without one, then tap Settings, support, and the trackpad gestures' Shizuku link. Each should show the message and stay open. Turn the browser back on and confirm the links open.

# Apps' own shortcuts in the quick launcher

A switch with the quick launcher's search settings: **App shortcuts in results** (off by default). Apps' own shortcuts, the ones their launcher icon shows on a long press ("New message", "Scan QR code"), become quick launcher results you can open straight away, and can be assigned to a key like any command. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026), where it is on by default.

## Behaviour

- Each launcher app's declared shortcuts are listed under the app's name, with its icon, searchable by the shortcut's name and the app's.
- Only screens the app lets other apps open are offered; one the app has since closed off is refused when opened.
- Read from the apps on the phone without any permission (the shortcuts an app declares in its manifest), and kept until an app updates. Shortcuts an app adds while running (recent chats, say) are only visible to the home screen app, so they aren't included.

## Implementation boundaries

- `shortcuts/AppActionDiscovery` (ported from Flux Keyboard) reads an app's `android.app.shortcuts` resource and checks each screen can be opened.
- `commands/AppShortcutCommandSource` turns them into commands (source App actions, kind Shortcut), cached per app by its update time; its switch (`quick_launcher_app_shortcuts`) is registered in `BackupContract`. Added to `CommandRegistry`.
- `CommandLaunchSpec.IntentUri` gains an optional `intentUri` (saved with an assigned key by `CommandJson`); `CommandExecutor` opens it through `AppActionDiscovery.intentFor`.
- A row in the quick launcher's search settings and a settings-search link (`quick_launcher.app_shortcuts`); two strings in their own file with all ten translations.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*AppShortcutCommandsTest'
```

`AppShortcutCommandsTest` (in `ime-integration`): off by default and backed up; an app without shortcuts offering none; a shortcut opening only its own app's exported screen; and its intent kept when saved to a key.

## Validation

Run on 14 November 2026 against source commit `66501e0a` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 186 classes, 1,421 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). Of these, 4 are new, in `AppShortcutCommandsTest`.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new class is in the `ime-integration` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check: turn the switch on and open the quick launcher; type "scan" (Google Lens, a bank app) or "new" (a messaging or notes app) and check the app's own shortcut is a result that opens the right screen. Assign one to a key and check the key opens it.

# The keyboard keeps typing through faults

Faults that used to end the keyboard (and end it again at the next press) are logged instead, and typing carries on. Ported from Flux Keyboard's stability pass onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- A key, a long press, a cursor move or a field the keyboard can't handle: the fault is logged and the key goes to the app as a plain key (`InputMethodService`'s own handling), instead of the keyboard ending.
- The accessibility service (the Clicks keyboard's launcher buttons) lets a key through as it is the same way, rather than stopping, which could also leave Android turning it off.
- A failure on a background thread (a download, say) is logged and ends that thread alone, not the app the keyboard lives in. The main thread's failures and running out of memory end it as before.
- Back keeps working as it does now on Android 16: the app opts out of predictive back, which would skip its own Back handling.

## Implementation boundaries

- `PhysicalKeyboardInputMethodService`: `onKeyDown`, `onKeyUp` and `onKeyLongPress` call their former bodies (now `receiveKeyDown`, `receiveKeyUp`, `receiveKeyLongPress`) through `keySafely`; `onStartInput`, `onStartInputView` and `onUpdateSelection` likewise (`receiveStartInput`, `receiveStartInputView`, `receiveSelection`). The bodies are unchanged.
- `ClicksLauncherButtonAccessibilityService.onKeyEvent` calls `handleKey` the same way and resets the device's held state on a fault.
- `PlektraApplication` installs `BackgroundFailures.handler` over the default handler.
- `AndroidManifest.xml`: `android:enableOnBackInvokedCallback="false"`.
- The entry points' signature lines change, so other branches that add code right at the start of those methods (contacts, one-time codes, paste suggestion, terminal microphone) need a small rebase after this one; the method bodies merge as they are.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*KeysThroughFaultsTest'
```

`KeysThroughFaultsTest` (in `ime-integration`), with a fault forced in the key filters: a key down, long press and up passed on as plain keys and typing carrying on after; a field start and cursor move logged rather than thrown, and typing working after; and the background handler ending only a background thread's failure quietly, the main thread's and running out of memory passed on.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

There is no fault to trigger on a working build; the check is that nothing changes. Type in a few apps, use Back to close the keyboard, the SYM pages and settings screens (Android 16 especially), and check everything behaves as before.

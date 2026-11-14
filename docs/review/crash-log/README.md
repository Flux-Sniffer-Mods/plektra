# A crash log in the debug export

What ends the keyboard (or a thread of it) is now written to a small crash log in the app's own storage, with its stack, and the debug export (Settings → debug recorder) carries it in a `[crash_log]` section. The recorder's Clear empties it too. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- Each failure that reaches Android's default handler is recorded with the time, the thread and the stack, then handled as before (the app still ends where it did).
- The newest entries are kept, up to 64 KB. Nothing typed is written: only what failed and where.
- Only once the phone has been unlocked (the log lives in credential-protected storage); before that, failures are handled as before without a record.

## Implementation boundaries

- `CrashLog` writes, reads and clears `crash_log.txt` in `filesDir`; `install` wraps the default uncaught-exception handler.
- `PlektraApplication.initializeUnlockedApplication` installs it.
- `MainActivity`: the debug report gains `[crash_log]`; the recorder's Clear calls `CrashLog.clear`.
- No setting, string, permission or migration. With the keys-through-faults PR also merged, its logged faults could be recorded here too (one `CrashLog.record` call in `keySafely`).

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*CrashLogTest'
```

`CrashLogTest` (in `app-distribution`): a failure kept with its stack and cleared; only the newest kept within the size, starting at an entry; an uncaught failure recorded and passed on to the previous handler.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: export a debug report and check `[crash_log]` reads "(no crashes recorded)"; after a crash (if one happens), export again and check its stack is there; Clear empties it.

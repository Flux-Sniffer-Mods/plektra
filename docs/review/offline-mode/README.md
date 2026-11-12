# Offline mode

A switch in Settings → Advanced, **Offline mode** (off by default): nothing in Plektra goes online. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026), where it covers what that fork fetches; here it covers what Plektra does.

## Behaviour

Plektra goes online in six places, and each one checks the switch:

| What | Where | In offline mode |
| --- | --- | --- |
| Update checks | `update/UpdatePolicy.shouldUseGithubUpdateChecks` | not made, as on an F-Droid install |
| Release notes after an update | `update/ReleaseNotesFetcher` | the built-in summary is shown |
| Dictionary list and downloads | `DictionaryRepositoryManager` | fail as a network error does |
| Layout list and downloads | `LayoutRepositoryManager` | fail as a network error does |
| Online voice input (OpenAI, OpenRouter), its model list and key check | `SpeechApiClient` | nothing is sent |
| Live transcription | `OpenAiLiveSpeechClient` | no connection is opened |

- With an online voice provider chosen, the microphone uses Android's speech recognition while offline mode is on. The choice and its key are kept for when it is turned off again. FUTO is on the device and is used as before.
- Links the user opens (a release page, FUTO's download) still open in the browser: that is the browser going online, not the keyboard.
- Android's own speech recognition may use the network; that is the phone's service and outside this switch.

## Implementation boundaries

- `OfflineMode` holds the preference (`offline_mode`, default false) and a flag read without a `Context`, so the download managers and speech clients can check it. `PlektraApplication` loads it once the user is unlocked, and it follows the preference however it changes, a restored backup included.
- Each guard sits at the single place its requests pass through (`execute` for `SpeechApiClient`), before anything is built or sent.
- New: one preference, registered in `BackupContract`; one settings-search link (`advanced.offline_mode`); two strings in their own file (`offline_mode.xml`) with all ten translations. No permission, route or migration.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*OfflineModeTest' --tests '*OfflineSpeechClientsTest' --tests '*OfflineModeBackupTest'
```

`OfflineModeTest` (in `app-distribution`): off unless switched on; following the preference however it changes; update checks off, the dictionary and layout lists failing without a request and release notes answering with none; Android's voice input in place of an online provider, with the choice kept. `OfflineSpeechClientsTest` (in `speech`) points both clients at a local server and checks that no request reaches it. `OfflineModeBackupTest` (in `data-backup`): ZIP export, clear and restore, the restored switch taking effect at once.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: turn on Offline mode, then open Dictionaries and Layouts (the online lists should fail to load), choose OpenAI or OpenRouter for voice input and press the microphone (Android's recognition should start), and confirm with a network monitor or the phone in a captive network log that Plektra makes no connection. Turn it off and confirm the lists load again.

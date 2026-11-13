# Private mode

One command, **Private mode**, turns on incognito typing in every field and offline mode together, and a second press puts both back. It is offered in the quick launcher and can be assigned to a key, like the other Plektra commands. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

**Stacked:** this branch carries `flux/incognito-typing` and `flux/offline-mode` (their feature commits), which it builds on. Review and merge those first; the diff of this PR alone is its last commit.

## Behaviour

- On: every field is incognito from that moment, the one being typed in too, and offline mode is on. A toast says so.
- Off: incognito goes back to each field's own rule (Incognito typing, or the app asking), and offline mode to what it was before private mode, on or off.
- Kept across restarts. Left out of backups: it is a temporary state, not a setting (offline mode's own switch is backed up as before).

## Implementation boundaries

- `core/PrivateMode` holds the state (`private_mode`, and offline mode as it was before) and `toggle`. Its preferences are not in `BackupContract`, so backups skip them.
- `IncognitoTyping.active` becomes the field's own value or private mode; the input method still sets the field's value as each field starts. `SuggestionController` checks private mode at the word boundary too, so next words stop being learned at once rather than from the next field.
- `PlektraCommandSource` lists the command (assigned key, quick launcher, Nav Mode); `CommandExecutor` runs it. `PlektraApplication` reads the state at start.
- New: four strings in their own file (`private_mode.xml`) with all ten translations. No setting row, permission, route or migration.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*PrivateModeTest' --tests '*IncognitoTypingTest' --tests '*OfflineModeTest'
```

`PrivateModeTest` (in `suggestions`): on turns on offline mode and makes the current field incognito; off puts both back, keeping a field's own incognito and offline mode chosen before; the state read again at start; the command found in the quick launcher, on keys and by searching "incognito", and running it.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: open the quick launcher, search "private" and run Private mode (a toast says it's on); type `pink elephant` a few times, insert an emoji, and open Dictionaries (the online list should fail). Run it again: in a new field `pink` should not predict `elephant`, the emoji should not be among the recents, and the online lists load again.

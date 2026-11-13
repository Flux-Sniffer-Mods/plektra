# One-time codes

A switch in Settings → Advanced, **One-time codes** (off by default): a code from a text message, email or authenticator notification is offered as a chip in the next field it goes in, and a tap types it. It needs Android's notification access, which the switch opens; until it is granted a row under the switch says so. Ported from Flux Keyboard onto upstream `68ace8ab` (10 November 2026).

## Behaviour

- Found: 4 to 8 digits near a word like "code", "PIN", "verification" or "login" in the keyboard's languages, a sender's prefix ("G-123456"), a split code ("123 456"), or a notification's "Copy 123456" button. The newest message of a conversation decides. Prices, years and times are passed over.
- Offered in a number field, or a text field whose hint or name says code; never in an email, name, address or ordinary message field. A code arriving while such a field is open is offered at once. Typing or another field takes the chip away.
- Typed a character at a time, each once the last has landed, so codes split over a box per digit fill every box. Digits go as key presses, which those boxes listen for.
- Kept in memory only, for 3 minutes or until typed, and a code once typed isn't offered again. Nothing is stored or sent. The keyboard's own notifications are ignored.
- Android 15 and later hide one-time codes from notification listeners such as this one, unless the app may read sensitive notifications. A user can allow that over ADB: `adb shell appops set rocks.pkb.plektra RECEIVE_SENSITIVE_NOTIFICATIONS allow` (Flux Keyboard does it through its shell access, which Plektra doesn't have). Without it, only codes Android doesn't recognise as such reach the keyboard.

## Implementation boundaries

- `otp/OneTimeCodes` (ported unchanged apart from its package) finds codes and holds the latest one. `otp/OneTimeCodeListener` is the notification listener, declared in the manifest with `BIND_NOTIFICATION_LISTENER_SERVICE`; it reads nothing while the switch is off. `otp/OneTimeCodeFields` decides which fields take a code; `otp/OneTimeCodeSettings` holds the switch (`one_time_codes`, registered in `BackupContract`) and the access check.
- `PhysicalKeyboardInputMethodService`: the chip uses the bar's expansion suggestions, offered in `onStartInputView` (not on a restart) and when a code arrives, cleared on the next key; the typing paces itself on the editor's text.
- New: one settings-search link (`advanced.one_time_codes`); five strings in their own file (`one_time_codes.xml`) with all ten translations. The only new permission is the system binding a user grants as notification access.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*OneTimeCodesTest' --tests '*OneTimeCodeFieldsTest' --tests '*OneTimeCodesBackupTest'
```

`OneTimeCodesTest` (Flux Keyboard's, in `suggestions`): common code messages in several languages, codes near their keyword, prices, years, times and words that only look like keywords passed over, copy buttons, and the lifetime and once-typed rules. `OneTimeCodeFieldsTest` (in `suggestions`): which fields take a code, and the code read from real notifications (text, a copy button, a conversation's newest message). `OneTimeCodesBackupTest` (in `data-backup`): ZIP export, clear and restore of the switch.

## Validation

VALIDATION_PLACEHOLDER

## Device check (still to do)

Not yet run on a device. Suggested check: turn the switch on and grant notification access; send the phone a text such as "Your verification code is 482913"; open a sign-in page's code field (one field, then one with a box per digit): the chip "Code 482913" should appear and fill it. Check it isn't offered in a message field, and is gone after 3 minutes.

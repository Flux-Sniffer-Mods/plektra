# Clean copied links

An opt-in switch, **Clean copied links** (Settings → Advanced, beside the clipboard retention settings), removes tracking from a link as it is copied: the clipboard itself holds the clean link, so it is clean wherever it is pasted, in the keyboard's clipboard history, an app's own Paste or Ctrl+V. This answers palsoftware/pastiera#278 and is ported from Flux Keyboard (in its 2.0.0 release, where it is on by default) onto upstream `68ace8ab` (10 November 2026). Here it is off by default, as it rewrites the clipboard.

## Behaviour

- Tracking parameters go: `utm_*`, `fbclid`, `gclid`, `mc_eid`, `igshid` and similar network-wide ones, plus share ids only on the sites that add them (YouTube's `si` and `ab_channel`, Spotify's `si`, Amazon's and eBay's referral parameters, and others). Parameters that change the page (a video id, a playlist, a start time, a search query) stay.
- Mobile hosts become the normal site (`m.youtube.com` → `youtube.com`, `en.m.wikipedia.org` → `en.wikipedia.org`), and redirect links become the link they lead to (`google.com/url?q=…`, `l.facebook.com/l.php?u=…`, YouTube's `/redirect`).
- Text around a link is kept, and punctuation after a link stays outside it: `Read this: https://example.com/a?id=7&utm_source=x.` becomes `Read this: https://example.com/a?id=7.`
- The clipboard keeps its label.

## Implementation boundaries

- `LinkCleaner` is pure text logic, ported unchanged apart from its package and a precomputed lower-case parameter set. `CopiedLinkCleaning` holds the preference (`clean_copied_links`, default false) and the clipboard step.
- `ClipboardHistoryManager.onPrimaryClipChanged()` cleans first. When it replaces the clip, the clean copy arrives as a clipboard change of its own and is the one recorded in history; an already clean clip is left alone, so there is no loop. Cleaning works whether or not clipboard history is on.
- Only a single plain-text item is replaced, or formatted text that is nothing but a link. A copy marked sensitive (password managers), a URI, an intent, several items, text without `://` and text over 4,000 characters are never touched.
- Links are cleaned when the keyboard sees the copy, that is while Plektra's keyboard service is running and listening to the clipboard. History entries saved before the switch was turned on are not rewritten.
- New: one preference, registered in `BackupContract` so ZIP backups carry it; one settings-search link (`advanced.clean_copied_links`); two strings in their own file (`clean_copied_links.xml`) with all ten translations, using each locale's existing clipboard term. No permission, route or migration.

## Reproduce automated checks

```sh
LC_ALL=C.UTF-8 ./gradlew :app:testStableDebugUnitTest --tests '*LinkCleanerTest' --tests '*CopiedLinkCleaningTest' --tests '*CleanCopiedLinksBackupTest'
python3 scripts/ci-tests.py run --topic data-backup
```

`LinkCleanerTest` covers the parameter rules, site-specific ids, mobile hosts, redirects, surrounding text and that cleaning twice changes nothing. `CopiedLinkCleaningTest` uses Robolectric's clipboard: off by default, a copied link cleaned with its label kept and no second rewrite, sensitive copies untouched, formatted text only when it is a lone link, and other copies (no link, already clean, a URI, two items, too long) left as they are. `CleanCopiedLinksBackupTest` exports, clears and restores the setting through the ZIP backup.

## Validation

Run on 11 November 2026 against source commit `97a44d4f` (upstream `68ace8ab` plus this change), with a UTF-8 locale as on CI:

- Full `testStableDebugUnitTest`: 188 classes, 1,429 tests, no failures, errors or skips (upstream alone: 185 classes, 1,417 tests). New: 6 in `LinkCleanerTest`, 5 in `CopiedLinkCleaningTest`, 1 in `CleanCopiedLinksBackupTest`; `SettingLinkCoverageTest` and `BackupPreferenceContractIntegrationTest` pass with the new link and preference.
- `python3 scripts/ci-tests.py check` and the Python guard tests pass; the new classes are in the `data-backup` topic.
- `lintStableDebug` then `scripts/check-lint.py`: 1,056 errors, no new errors against `ci/lint-errors.json`.

## Device check (still to do)

Not yet run on a device. Suggested check: turn the switch on, copy `https://www.youtube.com/watch?v=dQw4w9WgXcQ&si=abc&t=42` from a browser, paste it into a message with the app's own Paste and with Ctrl+V, and open the keyboard's clipboard history; each should show `https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=42`. Copy a password from a password manager and confirm it is unchanged.

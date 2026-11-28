# Project notes

Android app "לקוחות ושירות" (ClientCallerID). Java, no AndroidX in app code, programmatic UI.
Source files are ASCII only: write Hebrew as \uXXXX escapes (helper: esc.py-style conversion).

## User guide — keep it current
The user asked that the user guide be updated with **every** change to the app.
- Source: `docs/guide/user-guide.html` (Hebrew, RTL). It is bundled into the app (assets) and
  attached to the GitHub release together with `user-guide.pdf`.
- On every feature change: update the relevant section, the version badge/footer, and the
  "מה חדש" table; then run `docs/guide/build-pdf.sh` and commit both files.

## Releases
Every push to `main` builds the APK and replaces release `latest`; other branches publish to `preview`.
Bump `versionCode` / `versionName` in `ClientCallerID/app/build.gradle` for each release.

## Privacy
Client data files (Excel/Access with names and ID numbers) must never be committed — the repo is public.

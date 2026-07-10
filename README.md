# Crab Do

A Kanban board and task management app with checklist subtasks, reminders, and real-time sync via Firestore. Available as an Android app and a web app.

## Apps

### Android
Kotlin Multiplatform + Jetpack Compose, with a local Room database that syncs to Firestore in the background.

**Build & run:**
```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Web
React + TypeScript + Tailwind CSS, reading/writing directly to Firestore.

```bash
cd webapp
npm install
npm run dev
```

## Features

- Kanban boards with drag-and-drop task cards
- Task detail view with title, description, and checklist subtasks
- Subtasks: drag to reorder, tap to rename, checkbox to complete
- Reminders with alarm or notification style, recurrence support
- Multiple colour themes (Deep Navy, Charcoal, Slate, Retro)
- Pinned board widget (Android)
- Real-time sync across devices via Firestore

## Setup

Both apps require a Firebase project with Firestore enabled.

- **Android:** place `google-services.json` in the `app/` directory
- **Web:** configure Firebase credentials in `webapp/src/firebase.ts`

## Web deploys

The web app deploys automatically to Firebase Hosting whenever changes to
`webapp/**`, `firebase.json`, or `.firebaserc` land on `main`, via the
[`Deploy Web` workflow](.github/workflows/deploy-web.yml) (it can also be run
manually from the Actions tab). The job uses the raw `firebase-tools` CLI, so
its `--only hosting` deploy target can later be extended to ship Firestore
rules/indexes and Cloud Functions from the same workflow.

### Required CI secrets

| Secret | Used by | What it is / how to get it |
|--------|---------|-----------------------------|
| `FIREBASE_SERVICE_ACCOUNT` | `Deploy Web` | A Google service-account JSON key with permission to deploy Hosting. |
| `GOOGLE_SERVICES_JSON` | `Release` (optional) | Base64-encoded `app/google-services.json` for a real Firebase-backed APK build. |
| `DEBUG_KEYSTORE` | `Release` (optional) | Base64-encoded debug keystore so Google Sign-In works in the release APK. |

**Getting `FIREBASE_SERVICE_ACCOUNT`:**

1. Firebase Console → ⚙️ **Project settings** → **Service accounts** →
   **Generate new private key**. (Or, in the Google Cloud console, use an
   existing service account that has the **Firebase Hosting Admin** role —
   add **Cloud Datastore / Firestore** and **Cloud Functions Admin** roles too
   if you later extend the deploy to rules and functions.)
2. Download the JSON key file.
3. In the GitHub repo: **Settings → Secrets and variables → Actions → New
   repository secret**. Name it `FIREBASE_SERVICE_ACCOUNT` and paste the **full
   JSON contents** (not base64 — the workflow writes it to a file verbatim).

The workflow writes that JSON to a temp file and points
`GOOGLE_APPLICATION_CREDENTIALS` at it, which is how `firebase-tools`
authenticates non-interactively — no `firebase login` or CI token needed.

## Releases & commit messages

Releases are cut automatically when commits land on `main`. The
[`Release` workflow](.github/workflows/release.yml) analyses the commit history,
bumps the semantic version, tags it, builds the APK, and publishes a GitHub
Release whose **notes are generated from the commit messages**.

Because of this, **commit messages must follow
[Conventional Commits](https://www.conventionalcommits.org/)**. The prefix both
determines the version bump and decides whether the commit appears in the
release notes — a commit without a recognised prefix is **omitted from the
changelog entirely**, which is how you end up with an empty release.

| Prefix | Version bump | Example |
|--------|--------------|---------|
| `feat:` | minor (1.1.0 → 1.2.0) | `feat: add dark mode toggle to settings` |
| `fix:` | patch (1.1.0 → 1.1.1) | `fix: stop reminders firing twice after snooze` |
| `feat!:` / `BREAKING CHANGE:` | major (1.1.0 → 2.0.0) | `feat!: drop support for the legacy sync format` |
| `ci:` / `chore:` / `docs:` / `refactor:` / `perf:` / `test:` | patch | `ci: cache Gradle packages between runs` |

Example commit:

```
feat: autosave task edits on close

Removes the explicit Save button; title and description now persist
on a debounced timer and when the editor closes.
```

> **Squash-merge note:** when a PR is squash-merged, the **PR title** becomes
> the commit subject on `main`, so the *PR title* is what must carry the
> Conventional Commits prefix.

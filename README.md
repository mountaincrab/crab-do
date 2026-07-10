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

## CI, releases & versioning

The three surfaces (`app/`, `webapp/`, `functions/`) are **versioned and
released independently**, each with its own semver tag and its own deploy job.
[release-please](https://github.com/googleapis/release-please) is the single
orchestrator ([`release-please.yml`](.github/workflows/release-please.yml)):

| Surface | Tag | Deploy job (reusable workflow) | What it does |
|---------|-----|--------------------------------|--------------|
| `app/` (Android) | `android-vX.Y.Z` | [`release-android.yml`](.github/workflows/release-android.yml) | Builds the APK, attaches it to the GitHub Release |
| `webapp/` | `webapp-vX.Y.Z` | [`deploy-web.yml`](.github/workflows/deploy-web.yml) | Builds the web app, deploys Firebase Hosting |
| `functions/` | `functions-vX.Y.Z` | [`deploy-functions.yml`](.github/workflows/deploy-functions.yml) | Deploys Cloud Functions + Firestore rules/indexes |

### How a release happens

1. You land Conventional-Commit commits on `main` (via merged PRs).
2. `release-please.yml` opens a **release PR per surface** — a changelog +
   version bump for that component — and keeps it updated as commits accumulate.
   Nothing ships until you merge it; the PR is the "release button".
3. You merge a release PR → on the next run, release-please creates that
   surface's tag + GitHub Release **and, in the same workflow run, calls the
   matching deploy job above** (gated on release-please's per-component
   `*--release_created` output). This is why no PAT is needed — the deploy runs
   as a job here rather than in a separate tag-triggered workflow (tags pushed
   by the default `GITHUB_TOKEN` don't trigger other workflows).

The deploy jobs are **reusable workflows** (`workflow_call`), so each can also
be run on its own from the **Actions tab** (`workflow_dispatch`) for a manual
redeploy — `release-android.yml` takes the tag to (re)attach the APK to.

**Which surface bumps is decided by the _path_ of the files a commit touched**
(release-please "manifest" mode): a commit under `webapp/**` bumps only
`webapp`, `app/**` bumps only `android`, etc. The Conventional-Commit **type**
decides the bump _size_. Config: [`release-please-config.json`](release-please-config.json)
+ [`.release-please-manifest.json`](.release-please-manifest.json).

> **Root/shared files** (`firestore.rules`, `firestore.indexes.json`,
> `firebase.json`) belong to no package, so a commit touching _only_ those won't
> auto-trigger a release. Group such changes into the same commit as the surface
> they support (rules/indexes ride the `functions` deploy), or run the relevant
> `Deploy …` workflow manually via **workflow_dispatch**.

### Commit messages (required: Conventional Commits)

The commit **type** determines the version bump _and_ whether the commit shows
up in the changelog — a commit without a recognised prefix is **omitted from the
release notes entirely**.

| Prefix | Version bump | Example |
|--------|--------------|---------|
| `feat:` | minor (1.1.0 → 1.2.0) | `feat: add dark mode toggle to settings` |
| `fix:` | patch (1.1.0 → 1.1.1) | `fix: stop reminders firing twice after snooze` |
| `feat!:` / `BREAKING CHANGE:` | major (1.1.0 → 2.0.0) | `feat!: drop support for the legacy sync format` |
| `ci:` / `chore:` / `docs:` / `refactor:` / `perf:` / `test:` | patch | `ci: cache Gradle packages between runs` |

> **Squash-merge note:** when a PR is squash-merged, the **PR title** becomes
> the commit subject on `main`, so the *PR title* is what must carry the prefix.

### Required CI secrets

Set these under **Settings → Secrets and variables → Actions**:

| Secret | Used by | What it is / how to get it |
|--------|---------|-----------------------------|
| `FIREBASE_SERVICE_ACCOUNT` | `deploy-web.yml`, `deploy-functions.yml` | A Google **service-account JSON key** authorised to deploy Hosting, Functions, and Firestore rules/indexes. |
| `GOOGLE_SERVICES_JSON` | `release-android.yml` (optional) | Base64-encoded `app/google-services.json` for a real Firebase-backed APK build. Falls back to a stub if unset. |
| `DEBUG_KEYSTORE` | `release-android.yml` (optional) | Base64-encoded debug keystore so Google Sign-In works in the built APK. |

Note there's **no PAT** — release-please and the deploy jobs all run on the
default `GITHUB_TOKEN`, so `FIREBASE_SERVICE_ACCOUNT` is the only secret you
must add for deploys to work (the Android ones are optional).

**Getting `FIREBASE_SERVICE_ACCOUNT`:**

1. Firebase Console → ⚙️ **Project settings** → **Service accounts** →
   **Generate new private key** (or use a Google Cloud service account with the
   **Firebase Hosting Admin**, **Cloud Functions Admin**, and **Cloud Datastore
   Owner** roles).
2. Download the JSON key file.
3. Paste the **full JSON contents** as the secret (not base64 — the workflows
   write it to a file verbatim and point `GOOGLE_APPLICATION_CREDENTIALS` at it,
   which is how `firebase-tools` authenticates non-interactively).

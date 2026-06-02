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

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

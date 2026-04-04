---
name: Firebase Setup
description: Firebase Auth fully wired into LoginActivity + Sandcastle; complete setup steps
type: project
---

## Firebase Console setup (manual)
1. console.firebase.google.com → Add project → "WatchNext"
2. Add Android app → package: `cosc.brocku.ca.watchnext`
3. Download `google-services.json` → place in `app/` folder
4. Authentication → Get started → enable **Email/Password**
5. Sync Gradle in Android Studio — won't build without `google-services.json`

## Gradle (already applied)
- `libs.versions.toml`: `firebaseBom = "33.7.0"`, `googleServices = "4.4.2"`, firebase-auth, firebase-firestore, google-services plugin
- `build.gradle.kts` (root): google-services plugin registered
- `app/build.gradle.kts`: google-services plugin applied + Firebase BOM + auth + firestore deps

## Files changed in the app
- `LoginActivity.java` — replaced fake SharedPreferences auth with real Firebase Auth (sign in + sign up)
- `MainActivity.java` — email in drawer header now from `FirebaseAuth.getCurrentUser().getEmail()`; sign out calls `FirebaseAuth.signOut()`
- `SandcastleClient.java` — new file; calls `register_user.php` on Sandcastle after signup to create Postgres profile
- `drawer_menu.xml` — added Sign Out item (`R.id.drawer_sign_out`)

## End-to-end auth flow
```
Sign up  → Firebase creates account → SandcastleClient → register_user.php → Postgres row created
Sign in  → Firebase verifies → MainActivity (profile already in Postgres)
Sign out → FirebaseAuth.signOut() → back to LoginActivity
```

## Key rule
- `firebase_uid` is the bridge — it's the FK in every Postgres table
- On every login, Firebase UID is available via `FirebaseAuth.getInstance().getCurrentUser().getUid()`
- Pass this UID to every Sandcastle PHP endpoint

## Role in the architecture
- Firebase Auth = identity / login
- Firestore = real-time/social (mood updates, notifications, follower feeds)
- Sandcastle/Postgres = persistent profile data, watchlists, history, ratings

**Why:** Instructor recommended Firebase alongside Sandcastle. Firebase handles auth cleanly; Firestore suits real-time social features.
**How to apply:** Never store auth state in SharedPreferences — always check `FirebaseAuth.getInstance().getCurrentUser()`.

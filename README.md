# SmartKart

Android shopping app with Firebase Phone Auth, Firestore, QR cart binding, and real-time cart updates.

## Prerequisites

- Android Studio (latest stable)
- JDK 11+
- A Firebase project with Phone Auth, Firestore, and Cloud Functions enabled
- Node.js 24+ (for Cloud Functions)

## Quick start (clone this repo)

This repo does **not** include secrets or Firebase config files. You must add them locally.

### 1. Copy secret templates

**Android (`local.properties`):**

```bash
cp local.properties.example local.properties
```

Edit `local.properties` and set:

| Key | Description |
|-----|-------------|
| `sdk.dir` | Path to your Android SDK (Android Studio often sets this automatically) |
| `CART_SECRET` | Shared secret for Cloud Functions — must match Firebase `CART_SECRET` |
| `BIND_CART_BASE_URL` | Full URL of `bindCartToUser` (must end with `/`) |
| `CLOUD_FUNCTIONS_BASE_URL` | Base URL for other functions like `deleteItemFromCart` (must end with `/`) |

Example:

```properties
sdk.dir=C\:\\Users\\You\\AppData\\Local\\Android\\Sdk
CART_SECRET=your_long_secret_string
BIND_CART_BASE_URL=https://your-bind-cart-url/
CLOUD_FUNCTIONS_BASE_URL=https://us-central1-your-project-id.cloudfunctions.net/
```

Gradle reads these values at build time and puts them into `BuildConfig`. They are **never** committed to git.

### 2. Add Firebase config (`google-services.json`)

1. Open [Firebase Console](https://console.firebase.google.com) → your project
2. Project settings → Your apps → Android app (`com.yourbusiness.smartkart`)
3. Download `google-services.json`
4. Place it at: `app/google-services.json`

See `app/google-services.json.example` for the expected file structure.

### 3. Cloud Functions secret

**Local development / emulator:**

```bash
cp functions/.secret.local.example functions/.secret.local
```

Edit `functions/.secret.local` and set `CART_SECRET` to the same value as in `local.properties`.

**Production deploy:**

```bash
firebase functions:secrets:set CART_SECRET
```

Enter the same secret when prompted. Redeploy functions after setting the secret:

```bash
cd functions
npm install
firebase deploy --only functions
```

### 4. Build and run

Open the project in Android Studio and run the `app` module, or:

```bash
./gradlew assembleDebug
```

## Project structure

| Area | Description |
|------|-------------|
| `app/` | Android app (Jetpack Compose, MVVM) |
| `functions/` | Firebase Cloud Functions backend |
| `local.properties` | Local secrets (gitignored) |
| `app/google-services.json` | Firebase Android config (gitignored) |

## Security notes

- **Never** commit `local.properties`, `google-services.json`, or `functions/.secret.local`
- **Never** hardcode API keys, secrets, or private URLs in Kotlin/Java source
- `local.properties.example` and `*.example` files are safe templates with placeholders only

## Features

- Phone authentication (Firebase Auth)
- User profile setup (Firestore `users`)
- Cart check routing (QR scanner vs cart screen)
- QR cart binding via Cloud Function
- Real-time cart session listener
- Remove item from cart via Cloud Function

## License

Add your license here if publishing publicly.

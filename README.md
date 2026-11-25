# udu's chat (Android / Kotlin + Jetpack Compose + Firebase)

**udu's chat** is a WhatsApp-style global messaging Android app built with Kotlin and Jetpack Compose (`com.uduschat.app`).

## Included Features
- **Valid International Phone Number Verification**: Country picker with 30+ countries (`+880`, `+91`, `+1`, `+44`, `+966`, `+971`, etc.), strict E.164 normalization, OTP verification flow, and profile setup.
- **Global Internet Connectivity (Firebase)**:
  - **Firebase Phone Auth** (real SMS OTP verification)
  - **Cloud Firestore** (real-time 1-on-1 & Group chats, user discovery by phone number anywhere in the world, delivery & blue read ticks, typing indicators, reactions, replies, edits, delete for me/everyone, disappearing messages, status stories, and call signaling)
  - **Firebase Storage** (profile photos, chat images, voice notes, documents, and status photos)
  - **Firebase Cloud Messaging (FCM)** (`UdusMessagingService`)
- **Out-of-the-Box Local Demo Mode**: Compiles, launches, and lets you test every feature immediately when `app/google-services.json` has not been added yet.
- **Chats & Messaging**:
  - Filters (`All`, `Unread`, `Favorites`, `Groups`), Search, Archived Chats, Starred Messages, Pin/Mute/Favorite/Mark Unread
  - Text, Gallery Photos, Camera Capture, View Once Photos, Real `.m4a` Voice Note Recording & Playback (`1x / 1.5x / 2x`), Documents, Location Pins, Contact Cards
  - Swipe-to-Reply, Long-press Reactions, Forward, Edit, Delete for Me / Everyone, Disappearing Timers (`Off / 24h / 7d / 90d`), Chat Wallpapers
  - Group Creation, Group Info, Add/Remove Participants, Admin Roles, Broadcast Messages
- **Updates Tab**: 24-hour Text & Photo Statuses, Interactive Story Viewer with Viewer Counts & Status Replies, Channels Follow/Unfollow
- **Calls Tab**: Voice & Video Call logs, Call-back, Incoming/Outgoing Call Overlay with Mute, Speaker, Video & Camera Switch toggles
- **Settings & Security**: Light/Dark/System Theme, Font Size, Default Wallpaper, Two-Step Verification 6-digit PIN lock, Privacy controls (`Last seen`, `Read receipts`, `Blocked contacts`)

## Enable Live Global Internet Mode (Firebase Setup)
1. Go to [Firebase Console](https://console.firebase.google.com/) and create a project.
2. Add an **Android App** with package name `com.uduschat.app`.
3. Download `google-services.json` and place it at `app/google-services.json` (see `app/google-services.json.template`).
4. In Firebase Console:
   - Enable **Authentication -> Sign-in method -> Phone**.
   - Create **Cloud Firestore** and deploy `firebase/firestore.rules`.
   - Enable **Firebase Storage** and deploy `firebase/storage.rules`.
   - Enable **Cloud Messaging** (FCM).
5. Rebuild (`.\gradlew.bat assembleDebug`) — `app/build.gradle.kts` automatically activates the `com.google.gms.google-services` plugin and switches from Demo Mode to Global Cloud Mode!

## Build & Test
```powershell
.\gradlew.bat testDebugUnitTest assembleDebug
```

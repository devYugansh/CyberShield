# CyberShield — AI Agent Briefing Document

> **Read this entire file before writing a single line of code.**
> This document is the single source of truth for what CyberShield is,
> why it exists, how it is built, and how you must work on it.

---

## 1. What Is CyberShield?

CyberShield is a **native Android mobile application** built for NIELIT (National Institute of Electronics & Information Technology), India. It is a **cyber security awareness and education platform** targeted at general Indian citizens — students, government employees, and the public — who have little to no prior knowledge of cyber security.

The app teaches cyber security concepts through:
- **Flashcard-based micro-lessons** (tap to flip, read, learn)
- **Per-lesson quizzes** (MCQ, immediate feedback, explanation)
- **Modular curriculum** (4+ modules, each with multiple lessons)
- **Progress tracking** (ring chart, per-lesson checkmarks)

Think of it as **Duolingo for Cyber Security**, built for India, by NIELIT.

---

## 2. Why Are We Building This?

India reported **14+ lakh cyber incidents in 2023**. Most victims are non-technical users who were unaware of basic threats like phishing, OTP fraud, and fake Wi-Fi. NIELIT mandated a lightweight, offline-capable mobile app that:

1. Requires **no internet after first login** (content is bundled)
2. Works on **low-end Android devices** (API 26+, ~2GB RAM)
3. Is available in **English** (Hindi localisation planned for v2)
4. Uses **Firebase phone OTP** for frictionless auth (no passwords)
5. Keeps **all progress local** (DataStore, no backend sync needed)

---

## 3. Tech Stack — Non-Negotiable Choices

| Layer | Technology | Reason |
|---|---|---|
| Language | **Kotlin** | Official Android language; null-safe |
| UI | **Jetpack Compose (Material 3)** | Declarative, no XML, modern |
| Navigation | **Navigation Compose 2.8+ (type-safe routes)** | No string routes, compile-time safe |
| DI | **Hilt** | Official Android DI; works with Compose |
| Auth | **Firebase Phone Auth** | SMS OTP, no password needed |
| Persistence | **DataStore Preferences** | Replaces SharedPreferences; coroutine-native |
| Content | **Bundled JSON in assets/** | Offline-first; no network call for lessons |
| JSON parsing | **kotlinx.serialization** | Kotlin-native, no Gson/Moshi |
| Image loading | **Coil** | Compose-native, lightweight |
| Min SDK | **API 26 (Android 8.0)** | Covers 95%+ active Indian devices |
| Target SDK | **API 35** | Latest stable |

**Do NOT introduce:** Retrofit, Room, Gson, Moshi, RxJava, LiveData, XML layouts, fragments, or any ML library. This app is intentionally simple.

---

## 4. Architecture

The app follows **MVVM + Repository pattern** with unidirectional data flow:

```
UI Layer (Composables)
    ↕  collectAsState()
ViewModel Layer (HiltViewModel)
    ↕  suspend fun / Flow
Repository Layer (ContentRepository, AuthRepository)
    ↕  DataStore / Firebase / assets/
Data Sources (modules.json, DataStore, Firebase Auth)
```

### Rules:
- **ViewModels never import any `androidx.compose.*`** — they are UI-agnostic
- **Composables never call repository directly** — always through ViewModel
- **All UI state is in a single `data class UiState`** per ViewModel
- **StateFlow, not LiveData** — this is a Compose-first project
- **`hiltViewModel()`** is always used inside composables, never `viewModel()`

---

## 5. Project Structure

```
app/src/main/
├── assets/
│   └── modules.json                  ← ALL lesson content lives here
│
├── java/com/nielit/cybershield/
│   ├── MainActivity.kt               ← Single activity; hosts NavGraph + Theme
│   │
│   ├── navigation/
│   │   └── NavGraph.kt               ← All routes defined here (type-safe)
│   │
│   ├── data/
│   │   ├── model/
│   │   │   └── Models.kt             ← All data classes (Module, Lesson, FlashCard, etc.)
│   │   └── repository/
│   │       ├── ContentRepository.kt  ← Reads modules.json + manages progress in DataStore
│   │       └── AuthRepository.kt     ← Firebase Phone Auth wrapper
│   │
│   ├── viewmodel/
│   │   ├── ThemeViewModel.kt         ← Dark/light mode; scoped to MainActivity
│   │   ├── SplashViewModel.kt        ← Firebase session check
│   │   ├── AuthViewModel.kt          ← Login + OTP state
│   │   ├── HomeViewModel.kt          ← Module list, progress, accordion, drawer
│   │   ├── FlashcardViewModel.kt     ← Card index, flip, quiz, lesson completion
│   │   └── SettingsViewModel.kt      ← Settings toggles (dark mode, notifications)
│   │
│   └── ui/
│       ├── theme/
│       │   ├── Color.kt              ← All design tokens (Navy, AccentBlue, etc.)
│       │   ├── Type.kt               ← Poppins + Nunito typography scale
│       │   ├── Shape.kt              ← Corner radius scale
│       │   └── Theme.kt             ← CyberShieldTheme() entry point
│       │
│       ├── components/
│       │   └── CommonComponents.kt   ← Reusable: TopBar, PrimaryButton, AvatarCircle,
│       │                                ToggleRow, DrawerNavItem, FeedbackBanner, etc.
│       │
│       └── screens/
│           ├── splash/SplashScreen.kt
│           ├── login/LoginScreen.kt
│           ├── otp/OtpVerifyScreen.kt
│           ├── home/HomeScreen.kt         ← Includes SideDrawerContent
│           ├── flashcard/FlashcardViewerScreen.kt
│           └── settings/SettingsScreen.kt
```

---

## 6. Screen Inventory

| # | Screen | Route | Status |
|---|---|---|---|
| 01 | Splash | `Route.Splash` | ✅ Built |
| 02 | Login (Phone) | `Route.Login` | ✅ Built |
| 03 | OTP Verify | `Route.OtpVerify` | ✅ Built |
| 04 | Home / Module Index | `Route.Home` | ✅ Built |
| 05 | Flashcard Viewer | `Route.FlashcardViewer` | ✅ Built |
| 06 | Quiz Card | (last page of FlashcardViewer) | ✅ Built |
| 07 | Side Drawer | (modal drawer inside HomeScreen) | ✅ Built |
| 08 | Settings | `Route.Settings` | 🔲 Pending |

---

## 7. Design Tokens (Never Hardcode Colors)

Always use `MaterialTheme.colorScheme.*` in composables.
Never write `Color(0xFF...)` directly inside a composable — define it in `Color.kt` first.

| Token Name | Hex | Usage |
|---|---|---|
| `Navy` | `#1B3A6B` | TopBar, headings, module card header |
| `AccentBlue` | `#2563EB` | CTAs, links, progress ring, active states |
| `SurfaceGrey` | `#F5F7FA` | Card backgrounds, screen background |
| `BorderGrey` | `#CBD5E1` | Card borders, dividers, inactive dots |
| `ErrorRed` | `#B91C1C` | Error text, wrong quiz answer |
| `ErrorRedLight` | `#FEE2E2` | Error banner background |
| `SuccessGreen` | `#166534` | Correct quiz answer, completed checkmark |
| `SuccessGreenLight` | `#DCFCE7` | Correct answer banner background |
| `AvatarTeal` | `#0D9488` | User avatar circle |

---

## 8. Content Structure (`modules.json`)

The entire app curriculum lives in `assets/modules.json`. Structure:

```
Module[]
  └── id, title, description, is_pro
  └── Lesson[]
        └── id, title
        └── FlashCard[]
              └── id, title (keyword), body (explanation)
        └── QuizData
              └── question, options[], correct_index, explanation
```

### Current modules (v1):
1. **Introduction to Cyber Security** — CIA Triad, threat types (free)
2. **Password & Account Security** — Strong passwords, 2FA (free)
3. **Safe Internet Practices** — HTTPS, public Wi-Fi, social engineering (free)
4. **Advanced Threats & Defence** — DDoS, SQL injection, zero-day (PRO)

To **add content**: edit `modules.json` only. No code changes needed.
To **add a module**: add a new object to the root array with a unique `id`.
To **add a lesson**: add to the `lessons` array of the target module.

---

## 9. Progress & State Management

### How progress works:
1. User completes a lesson by answering the quiz **correctly**
2. `FlashcardViewModel.checkAnswer()` calls `ContentRepository.markLessonComplete(moduleId, lessonId)`
3. Repository writes `lessonId` into `DataStore` key `"progress_<moduleId>"` (a `StringSet`)
4. `HomeViewModel` collects `ContentRepository.progressFlow()` → updates `progressMap`
5. `HomeScreen` recomposes → ring animates to new percentage → lesson row shows green checkmark

### DataStore keys:
| Key | Type | Stores |
|---|---|---|
| `"progress_<moduleId>"` | `StringSet` | Set of completed lesson IDs |
| `"app_theme_dark"` | `Boolean` | Dark mode preference |

---

## 10. Authentication Flow

```
App Launch
    ↓
SplashScreen → SplashViewModel checks Firebase.currentUser
    ├── User exists → navigate to Home (skip Login)
    └── No user   → navigate to Login
            ↓
        LoginScreen → enter 10-digit phone → tap "Get OTP"
            ↓
        Firebase sends SMS OTP
            ↓
        OtpVerifyScreen → 6-box input → tap "Verify OTP"
            ├── Correct → navigate to Home (clear backstack)
            └── Wrong   → shake animation, decrement attempts (max 3)
                          after 3 failures → "Resend OTP" enabled
```

**Important auth rules:**
- Phone number is stored as `+91XXXXXXXXXX` format internally
- Masked as `+91 98765 XXXXX` for display
- `verificationId` is passed as a nav argument to OtpVerifyScreen (type-safe route)
- After sign-out, entire backstack is cleared — user cannot press back to Home

---

## 11. Flashcard Viewer Flow

```
Lesson selected on HomeScreen
    ↓
FlashcardViewerScreen loads lesson via FlashcardViewModel.load(moduleId, lessonId)
    ↓
Card 1 (front: title, tap → back: explanation)
    → Card 2 → ... → Card N
    ↓
Quiz Card (last page — always index == cards.size)
    → User selects option
    → Taps "Check Answer"
    → Correct: green banner + "Next Lesson" + "Back to Modules"
    → Wrong:   red banner + correct option highlighted (no next lesson until retry? — TBD)
    ↓
If correct → ContentRepository.markLessonComplete() → DataStore updated
```

---

## 12. Naming Conventions

| Thing | Convention | Example |
|---|---|---|
| Composable functions | `PascalCase` | `FlashcardViewerScreen` |
| ViewModel | `PascalCase + ViewModel` | `HomeViewModel` |
| UI State class | `PascalCase + UiState` | `HomeUiState` |
| Route objects | `Route.PascalCase` | `Route.FlashcardViewer` |
| DataStore keys | `snake_case string` | `"progress_module_01"` |
| Module IDs | `module_NN` | `"module_01"` |
| Lesson IDs | `mNN_lNN` | `"m01_l02"` |
| Card IDs | `mNN_lNN_cNN` | `"m01_l02_c03"` |
| Color tokens | `PascalCase` | `Navy`, `AccentBlue` |
| Modifier params | always last, always named `modifier` | `modifier: Modifier = Modifier` |

---

## 13. Composable Rules

Every composable in this project must follow these rules:

```kotlin
// ✅ CORRECT — stateless, testable, previewable
@Composable
fun ModuleCard(
    module: Module,           // data passed in
    isExpanded: Boolean,      // state passed in
    onHeaderClick: () -> Unit, // events passed up
    modifier: Modifier = Modifier, // always last
) { ... }

// ❌ WRONG — composable reads from ViewModel directly
@Composable
fun ModuleCard(viewModel: HomeViewModel) { ... }
```

- **Stateless composables** receive all data and callbacks as parameters
- **Stateful composables** (screen-level) hold `viewModel = hiltViewModel()`
- Every screen has two composables: `XxxScreen()` (stateful) and `XxxContent()` (stateless)
- `modifier: Modifier = Modifier` is **always** the last parameter

---

## 14. What Still Needs to Be Built

### Immediate (v1 blockers):
- [ ] `SettingsScreen.kt` + `SettingsViewModel.kt` — toggles for dark mode, notifications, app version info
- [ ] `AuthRepository.kt` — Firebase Phone Auth wrapper (currently auth logic is inline in ViewModel stub)
- [ ] `SplashViewModel.kt` — Firebase session check
- [ ] `AuthViewModel.kt` — OTP request + verification state
- [ ] `AppModule.kt` (Hilt) — provides `DataStore`, `ContentRepository`, `AuthRepository`
- [ ] `CyberShieldApplication.kt` — `@HiltAndroidApp` Application class
- [ ] `AndroidManifest.xml` — `INTERNET` permission, `google-services.json` config, theme

### Nice to have (v1.1):
- [ ] Hindi localisation (`values-hi/strings.xml`)
- [ ] Lesson resume state (remember last card index if user exits mid-lesson)
- [ ] Notification for "continue learning" daily reminder
- [ ] Pro module unlock flow (currently PRO badge is display-only)

---

## 15. Hilt Dependency Graph

```
@HiltAndroidApp
CyberShieldApplication

@AndroidEntryPoint
MainActivity
    └── ThemeViewModel (@HiltViewModel)
            └── DataStore<Preferences>  (provided by AppModule)

HomeScreen
    └── HomeViewModel (@HiltViewModel)
            ├── ContentRepository
            │       ├── Context (@ApplicationContext)
            │       └── DataStore<Preferences>
            └── ThemeViewModel

FlashcardViewerScreen
    └── FlashcardViewModel (@HiltViewModel)
            └── ContentRepository

LoginScreen + OtpVerifyScreen
    └── AuthViewModel (@HiltViewModel)
            └── AuthRepository
                    └── FirebaseAuth.getInstance()
```

---

## 16. Key Files for an AI Agent to Know

If you are an AI agent working on this project, these are the files you will touch most often:

| Task | File(s) to read first | File(s) to edit |
|---|---|---|
| Add new screen | `NavGraph.kt`, any existing screen | New `XxxScreen.kt` + `XxxViewModel.kt` |
| Add lesson content | `Models.kt` | `assets/modules.json` |
| Fix a UI bug | `CommonComponents.kt`, affected screen | Affected screen `.kt` |
| Change a color | `Color.kt`, `Theme.kt` | `Color.kt` |
| Add a setting toggle | `SettingsScreen.kt`, `SettingsViewModel.kt` | Both + DataStore key in `AppModule` |
| Fix navigation bug | `NavGraph.kt` | `NavGraph.kt` |
| Debug progress not updating | `ContentRepository.kt`, `HomeViewModel.kt` | DataStore key name, `progressFlow()` |
| Add Hilt injection | `AppModule.kt` | `AppModule.kt` + target class |

---

## 17. DO NOT Rules for This Codebase

- ❌ Do NOT use `findViewById`, `ViewBinding`, or XML layouts — Compose only
- ❌ Do NOT use `SharedPreferences` — use DataStore
- ❌ Do NOT use `LiveData` — use `StateFlow` / `Flow`
- ❌ Do NOT call Firebase directly from a Composable — use ViewModel → Repository
- ❌ Do NOT hardcode strings in composables — use `stringResource()` or constants
- ❌ Do NOT hardcode colors as `Color(0xFF...)` inside composables — use theme tokens
- ❌ Do NOT create new Activities — this is a single-Activity app
- ❌ Do NOT use `GlobalScope` — use `viewModelScope` or `lifecycleScope`
- ❌ Do NOT add Room/SQLite — DataStore is sufficient for this app's data needs
- ❌ Do NOT add Retrofit — all content is bundled; no REST API exists

---

## 18. Package Name & Identifiers

| Field | Value |
|---|---|
| Application ID | `com.nielit.cybershield` |
| Package name | `com.nielit.cybershield` |
| App name | `CyberShield` |
| Organization | NIELIT (National Institute of Electronics & Information Technology) |
| Developer | Yugansh Sachdeva (primary), BHU |
| Min SDK | 26 |
| Target SDK | 35 |
| Version name | `1.0.0` |
| Version code | `1` |

---

## 19. Firebase Setup (Required Before First Run)

1. Go to [Firebase Console](https://console.firebase.google.com)
2. Create project → Add Android app → package `com.nielit.cybershield`
3. Enable **Phone Authentication** under Authentication → Sign-in methods
4. Download `google-services.json` → place at `app/google-services.json`
5. Add SHA-1 fingerprint of your debug keystore to Firebase project settings

Without `google-services.json`, the app will not compile.

---

## 20. Build & Run Checklist

Before running the app for the first time:

- [ ] `google-services.json` placed at `app/`
- [ ] `gradle.properties` has `android.useAndroidX=true`
- [ ] Font files in `app/src/main/res/font/`:
  - `poppins_regular.ttf`, `poppins_medium.ttf`, `poppins_semibold.ttf`, `poppins_bold.ttf`
  - `nunito_regular.ttf`, `nunito_medium.ttf`, `nunito_bold.ttf`
- [ ] `CyberShieldApplication.kt` created with `@HiltAndroidApp`
- [ ] `AndroidManifest.xml` declares `<uses-permission android:name="android.permission.INTERNET"/>`
- [ ] `AppModule.kt` (Hilt) provides `DataStore` instance

---

*Last updated: May 2026 | Maintained by: Yugansh Sachdeva, BHU*

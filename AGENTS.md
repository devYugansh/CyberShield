# CyberShield AI Agent Guidelines

## Project Overview

CyberShield is a native Android mobile application built for NIELIT (National Institute of Electronics & Information Technology), India. It is a cybersecurity awareness and education platform targeted at general Indian citizens with little to no prior knowledge of cyber security.

The app teaches concepts through flashcard-based micro-lessons, per-lesson quizzes, modular curriculum, and progress tracking. Think of it as Duolingo for Cyber Security, built for India by NIELIT.

### Tech Stack (Non-Negotiable)
- **Language**: Kotlin (official Android, null-safe)
- **UI**: Jetpack Compose (Material 3, declarative, no XML)
- **Navigation**: Navigation Compose 2.8+ (type-safe routes)
- **DI**: Hilt (official Android DI)
- **Auth**: Firebase Phone Auth (SMS OTP, no passwords)
- **Persistence**: DataStore Preferences (replaces SharedPreferences)
- **Content**: Bundled JSON in assets/ (offline-first)
- **JSON**: kotlinx.serialization (Kotlin-native)
- **Images**: Coil (Compose-native)
- **Min SDK**: API 26 (Android 8.0, covers 95%+ Indian devices)
- **Target SDK**: API 35 (latest stable)

**Do NOT introduce**: Retrofit, Room, Gson, Moshi, RxJava, LiveData, XML layouts, fragments, or ML libraries.

## Architecture Overview

The app follows MVVM + Repository pattern with unidirectional data flow:

```
UI Layer (Composables)
    ↕  collectAsState()
ViewModel Layer (HiltViewModel)
    ↕  suspend fun / Flow
Repository Layer (ContentRepository, AuthRepository)
    ↕  DataStore / Firebase / assets/
Data Sources (modules.json, DataStore, Firebase Auth)
```

### Core Layers
- **Domain**: Pure Kotlin models (`domain/model/Models.kt`) - `Module`, `Lesson`, `Flashcard`, `QuizQuestion`, `User`, progress tracking
- **Data**: Repositories with DataStore persistence (`data/repository/`) - `ContentRepository` (loads from `assets/modules.json`), `ThemeRepository`
- **UI**: Compose screens (`ui/screens/`), reusable components (`ui/components/`), ViewModels (`viewmodel/`), navigation (`navigation/Navigation.kt`)
- **DI**: Hilt modules (`di/AppModule.kt`) providing DataStore singleton

### Key Patterns
- **Navigation**: Routes object with string constants, composable NavHost with argument handling (e.g., `Routes.flashcard(moduleId, lessonId)`)
- **State Management**: ViewModels with sealed `UiState` classes (Loading/Success/Error), StateFlows, combine() for reactive data
- **Data Flow**: Repositories expose Flows, ViewModels transform to UI state, Screens collectAsState()
- **Theming**: Material 3 with custom color scheme (`ui/theme/`), dark/light support via `ThemeRepository`
- **Serialization**: Kotlinx Serialization for JSON parsing, `@SerialName` for snake_case API fields

### Authentication Flow
- Firebase Auth with phone OTP verification
- Screens: Splash → Login → OTP → Home
- Mock implementation in `AuthViewModel` (TODO: integrate real Firebase calls)

## Development Workflows

### Building & Running
```bash
./gradlew assembleDebug          # Build APK
./gradlew installDebug           # Install on device
./gradlew connectedDebugTest     # Run instrumented tests
```

### Key Gradle Configurations
- **Parallel builds**: `org.gradle.parallel=true` in `gradle.properties`
- **Version catalog**: Dependencies managed in `gradle/libs.versions.toml`
- **Plugins**: AGP 8.7.3, Kotlin 2.1.0, Hilt 2.51.1, KSP 2.1.0-1.0.29

### Content Management
- Static content in `app/src/main/assets/modules.json`
- Structure: modules → lessons → flashcards + optional quiz
- Progress persisted per-module in DataStore as `progress_<moduleId>` → Set<String> of completed lesson IDs

## Project Conventions

### Code Style
- **Package naming**: `com.nielit.cybershield.{data,di,domain,navigation,ui,viewmodel}`
- **File structure**: One concept per file (e.g., `ViewModels.kt` contains multiple ViewModel classes)
- **Naming**: PascalCase classes, camelCase functions, UPPER_SNAKE constants
- **Comments**: Section headers with `───` separators (e.g., `// ── UI State ──────────────────────────────────────────────────────────────`)

### UI Patterns
- **Components**: Reusable Composables in `ui/components/CommonComponents.kt` (CsTopBar, CsPrimaryButton, ProgressRing, etc.)
- **Screens**: Feature-based directories (`ui/screens/{splash,login,home,flashcard,settings}`)
- **Colors**: Custom palette in `ui/theme/Color.kt` (Navy, AccentBlue, SurfaceGrey, ErrorRed, etc.)
- **Shapes**: 4dp grid system (12dp cards, 8dp small, 50% pills)

### Data Patterns
- **Models**: Separate domain (camelCase) and data (snake_case with @SerialName) models
- **Persistence**: DataStore Preferences for key-value storage, no SQL database
- **Repository**: Singletons with lazy loading (ContentRepository.modules), Flow-based reactive queries

### Composable Rules
- Stateless composables receive all data and callbacks as parameters
- Stateful composables (screen-level) hold `viewModel = hiltViewModel()`
- Every screen has two composables: `XxxScreen()` (stateful) and `XxxContent()` (stateless)
- `modifier: Modifier = Modifier` is always the last parameter
- Never hardcode colors as `Color(0xFF...)` inside composables — use theme tokens

### Naming Conventions
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

### Testing
- Unit tests in `app/src/test/`, instrumented in `app/src/androidTest/`
- JUnit 4.13.2, Espresso 3.6.1, Compose testing BOM

## Integration Points

### External Dependencies
- **Firebase Auth**: Phone authentication (currently mocked)
- **Google Services**: `google-services.json` for Firebase config
- **Coil**: Image loading (compose integration)
- **Accompanist**: Pager for swipeable flashcards

### File References
- `app/src/main/assets/modules.json`: Content structure with 4 modules (Intro, Passwords, Safe Browsing, Advanced)
- `app/src/main/res/values/strings.xml`: App strings (app_name, etc.)
- `gradle/libs.versions.toml`: Centralized dependency versions
- `app/proguard-rules.pro`: Obfuscation rules (minifyEnabled=false in debug)

## Common Tasks

### Adding New Content
1. Edit `modules.json` with new module/lesson structure
2. Update domain models if needed
3. Test loading in `HomeViewModel.loadContent()`

### Implementing Auth
1. Replace mock calls in `AuthViewModel` with real Firebase Auth
2. Handle verification callbacks in Login/OTP screens
3. Update User model with real auth state

### UI Changes
1. Add components to `CommonComponents.kt` following existing patterns
2. Use theme colors/shapes from `ui/theme/`
3. Follow Material 3 guidelines with custom CyberShield branding

### Navigation Updates
1. Add routes to `Routes` object
2. Add composable to `CyberShieldNavHost`
3. Handle arguments with `navArgument()` and `backStack.arguments?.getString()`

## DO NOT Rules
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

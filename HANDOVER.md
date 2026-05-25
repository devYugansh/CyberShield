# CyberShield Development Handover

## 📖 Project Context
- **Purpose**: A cybersecurity awareness and education platform for Indian citizens (built for NIELIT).
- **Core Concept**: "Duolingo for Cyber Security" using flashcard-based micro-lessons and quizzes.
- **Key Tech Stack**: Kotlin (2.1.0), Jetpack Compose, Hilt (2.54), DataStore, Firebase Auth, and kotlinx.serialization.
- **Architecture**: MVVM + Repository pattern with a single-activity Navigation Compose setup.
- **Content Strategy**: Bundled JSON for offline-first, but updated via a custom GitHub-based OTA (Over-The-Air) system.

## 🎯 Current Goal
Establishing a stable build environment and a verified Realtime OTA content update system via GitHub.

## 🛠️ Current State of Code
- **Build Status**: Stable.
- **OTA System**: Fully functional for both Text and Images.
    - JSON is fetched from the `ota/` folder in the main repo.
    - Images are scanned from JSON and downloaded to `filesDir/images/` if missing.
- **Image Strategy**: Hybrid approach using `ImageUtils.getImageSource()`. Priority: Internal Storage -> Bundled Drawables.
- **Data Layer**: `ContentRepository` uses `StateFlow` for reactive updates.

## 📂 Files Actively Edited
- `app/src/main/java/com/nielit/cybershield/data/remote/ContentUpdateManager.kt`: Added image extraction and download logic.
- `app/src/main/java/com/nielit/cybershield/util/ImageUtils.kt`: Created for image resolution logic.
- `app/src/main/java/com/nielit/cybershield/ui/screens/flashcard/FlashcardViewerScreen.kt`: Updated `FlashCard` and `FullScreenImage` to support OTA images.
- `HANDOVER.md`: Updated with OTA image strategy.
- `ota/images/`: New local folder created for hosting update assets.

## ❌ Failed Attempts / Blockers
- **Hilt 2.59.2 Upgrade**: Failed because it requires AGP 9.0.0+, but the project is currently on AGP 8.7.3. Downgraded to **Hilt 2.54**, which is the "sweet spot" supporting both Kotlin 2.1.0 and AGP 8.x.
- **Private Repo OTA**: Initial test failed with 404 errors. Resolved by making the GitHub repository **Public** to allow `raw.githubusercontent.com` access without auth tokens.

## ⏭️ Next Steps
1. **Firebase Auth**: Move from mocked login to real Firebase Phone Authentication.
2. **Progress Verification**: Ensure that when an OTA update happens, the user's "Lesson Completion" progress (stored in DataStore) remains mapped correctly to the new JSON IDs.
3. **Content Expansion**: Add Unit 5 and 6 to `ota/units.json`.
4. **Code Review**: Start using Pull Requests for all new features to maintain the high quality established in this session.

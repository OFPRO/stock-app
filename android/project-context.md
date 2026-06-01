# App2 — Android Application

## Stack
- **Language:** Kotlin 2.0.21
- **UI:** Jetpack Compose + Material 3
- **Architecture:** Clean Architecture multi-module
- **DI:** Hilt (Dagger)
- **Database:** Room
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 35 (Android 15)
- **Build:** Gradle 8.9 + AGP 8.5.2

## Module Structure
```
App2/
├── app/                  → Entry point, DI, navigation
├── core/
│   ├── data/             → Repositories, Room DAOs, network
│   ├── domain/           → Use cases, domain models (pure Kotlin)
│   └── ui/               → Design system, theme, common composables
```

## Architecture Rules
- `core:domain` → pure Kotlin, no Android dependencies
- `core:data` → depends on `core:domain`, uses Room
- `core:ui` → depends on Compose, theme + design system
- `app` → depends on all `core/*` modules, navigation host

## Build Commands
```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release AAB/APK
./gradlew test                   # Unit tests
./gradlew lint                   # Static analysis
```

## Key Dependencies
- Hilt 2.52 (DI)
- Room 2.6.1 (local database)
- Navigation Compose 2.8.4
- Compose BOM 2024.12.01
- Coroutines 1.9.0

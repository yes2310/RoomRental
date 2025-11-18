# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**BangBilliJa** (방빌리자) is an Android room rental/reservation application built with Java and Firebase Authentication. The app allows users to browse available rooms, make reservations through a calendar interface, manage their bookings, and check in using QR codes.

**Application ID**: `com.example.bangbillija`
**Package**: `com.example.bangbillija`

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean

# Install debug build to connected device
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest
```

## Architecture

### Core Architecture Pattern

The app uses a **single-Activity architecture** with fragment-based navigation:
- `MainActivity` hosts all fragments and implements the `Navigator` interface
- Fragment transitions managed through `FragmentManager` with custom animations
- Shared state managed through `SharedReservationViewModel` (Activity-scoped)
- Navigation synchronized with `BottomNavigationView` and `MaterialToolbar`

### Key Components

**Navigation Flow**:
- Entry point: `LoginActivity` (launcher activity with Firebase Auth)
- Main hub: `MainActivity` with 5 primary fragments accessible via bottom navigation
- Detail views: Added to back stack with fade animations (defined in `res/anim/`)
- The `Navigator` interface (`ui/Navigator.java`) provides navigation callbacks for inter-fragment communication

**State Management**:
- `SharedReservationViewModel` is the single source of truth for reservation-related state
- Uses `MediatorLiveData` to automatically refresh time slots when room or date changes
- All fragments observe this shared ViewModel to react to selection changes

**Time Slot Calculation**:
- `SlotEngine` (`service/SlotEngine.java`) generates available/reserved time slots for a given date
- Operating hours: 9:00 AM - 9:00 PM (defined as `DAY_START` and `DAY_END`)
- Merges existing reservations with available gaps to create complete daily schedule

### Directory Structure

```
app/src/main/java/com/example/bangbillija/
├── core/                    # Shared ViewModels
│   └── SharedReservationViewModel.java
├── data/                    # Repository layer
│   ├── RoomRepository.java
│   ├── ReservationRepository.java
│   └── FakeDataSource.java  # Mock data (no real backend yet)
├── model/                   # Data models (immutable POJOs)
│   ├── Room.java
│   ├── Reservation.java
│   ├── TimeSlot.java
│   ├── RoomStatus.java
│   └── ReservationStatus.java
├── service/                 # Business logic
│   ├── AuthManager.java     # Firebase Auth wrapper (singleton)
│   ├── SlotEngine.java      # Time slot calculation engine
│   └── FirestoreManager.java # Firestore database operations
├── ui/                      # UI layer
│   ├── MainActivity.java    # Fragment host + Navigator
│   ├── Navigator.java       # Navigation interface
│   ├── auth/               # LoginActivity
│   ├── rooms/              # Room browsing
│   ├── calendar/           # Date/time selection
│   ├── reservations/       # My reservations & detail views
│   └── checkin/            # QR check-in
└── util/                    # Utilities
    ├── SimpleTextWatcher.java
    └── QRCodeUtil.java      # QR code generation using ZXing
```

## Firebase Configuration

This project uses Firebase Authentication for user management:
- Plugin: `com.google.gms.google-services` (version 4.4.0)
- Firebase BOM: 33.2.0
- Services used: `firebase-auth`, `firebase-analytics`
- `google-services.json` must be present in `app/` directory (not tracked in git)

**AuthManager Pattern**:
- Singleton wrapper around `FirebaseAuth`
- Provides callback-based API (`Completion` interface) for sign-in/sign-up
- Sets user display name during registration via `UserProfileChangeRequest`

## Data Layer Notes

**Current Implementation**:
- All data is currently mocked via `FakeDataSource.java`
- No real backend or database integration yet
- Repositories use `MutableLiveData` to simulate reactive data sources

**When implementing real backend**:
- Replace `FakeDataSource` calls in repositories with actual API/database calls
- Consider using Room database for local caching
- Update repositories to fetch data asynchronously
- Preserve the LiveData pattern for UI reactivity

## View Binding

This project uses View Binding (enabled in `app/build.gradle.kts`):
- Binding classes are auto-generated for each layout XML
- Pattern: `ActivityMainBinding`, `FragmentRoomListBinding`, etc.
- Access with: `ActivityMainBinding.inflate(layoutInflater)` in Activities
- Access with: `FragmentRoomListBinding.inflate(inflater, container, false)` in Fragments

## Build Configuration

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Java Version**: 11
- **Dependencies managed via**: Version catalog (`libs.versions.toml`)
- **ViewBinding**: Enabled
- **ProGuard**: Disabled in release builds (can be enabled later)

## Testing Structure

- Unit tests: `app/src/test/java/`
- Instrumented tests: `app/src/androidTest/` (uses AndroidJUnitRunner)
- Test dependencies: JUnit, Espresso, AndroidX Test

## Important Patterns

1. **Models are immutable**: All model classes use final fields with no setters
2. **Singletons for services**: `AuthManager`, `RoomRepository`, `ReservationRepository` use getInstance() pattern
3. **Fragment lifecycle**: Fragments observe LiveData in `onViewCreated()`, clean up in `onDestroyView()`
4. **Back stack management**: Only detail fragments are added to back stack; main fragments replace without back stack
5. **Toolbar title sync**: Managed centrally via `SharedReservationViewModel.toolbarTitle` LiveData

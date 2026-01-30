# Sleep Logger Android App

A simple Android app to log and track sleep patterns using a specific data format.

## Data Format
```
date-----sleep_time+time_to_fall_asleep-------wake_time
Example: 8-1-26-----11:39pm+30min-------6:45am
```

## Features

### Core Features (MVP)
- ✅ Single entry input dialog
- ✅ Bulk input for multiple entries
- ✅ Sleep entry list with cards
- ✅ Automatic sleep duration calculation
- ✅ Data persistence with Room database
- ✅ Edit and delete functionality
- ✅ Search and filter entries

### Technical Implementation
- **Architecture**: MVVM with Repository pattern
- **UI**: Jetpack Compose with Material 3
- **Database**: Room for local persistence
- **Language**: Kotlin
- **Minimum SDK**: API 24 (Android 7.0)

## Project Structure
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/sleeplogger/app/
│   │   │   ├── MainActivity.kt
│   │   │   ├── SleepEntry.kt
│   │   │   ├── database/
│   │   │   │   ├── SleepDao.kt
│   │   │   │   ├── SleepDatabase.kt
│   │   │   │   └── Converters.kt
│   │   │   ├── repository/
│   │   │   │   └── SleepRepository.kt
│   │   │   ├── viewmodel/
│   │   │   │   └── SleepViewModel.kt
│   │   │   └── ui/
│   │   │       └── components/
│   │   │           ├── SleepEntryCard.kt
│   │   │           ├── AddEntryDialog.kt
│   │   │           └── BulkInputDialog.kt
│   │   ├── res/
│   │   │   └── values/
│   │   │       ├── strings.xml
│   │   │       ├── colors.xml
│   │   │       └── themes.xml
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── gradlew.bat
```

## Build Instructions

### Prerequisites
- Android Studio (latest version)
- Android SDK
- Java 8 or higher

### Building the App
1. Open the project in Android Studio
2. Sync the project with Gradle files
3. Connect an Android device or start an emulator
4. Run the app

### Command Line Build
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### APK Location
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Usage

### Adding Single Entry
1. Tap the `+` floating action button
2. Fill in the date, sleep time, time to fall asleep, and wake time
3. Tap "Save"

### Bulk Input
1. Tap the `📝` floating action button
2. Enter multiple entries, one per line, using the format:
   ```
   date-----sleep_time+minutes-------wake_time
   ```
3. Example:
   ```
   8-1-26-----11:39pm+30min-------6:45am
   8-1-27-----12:15am+15min-------7:30am
   ```
4. Tap "Save Entries"

### Searching Entries
- A search bar is available at the top of the screen.
- Start typing in the search bar to filter entries by date, sleep time, wake time, or total sleep.
- The list updates in real-time as you type.

### Managing Entries
- **View**: All entries are displayed in reverse chronological order
- **Edit**: Tap on an entry to expand it, then tap "Edit"
- **Delete**: Tap on an entry to expand it, then tap "Delete"

## Data Format Details

### Date Format
- Format: `M-d-yy` (e.g., `8-1-26` for August 1, 2026)
- Month and day can be 1 or 2 digits
- Year should be 2 digits

### Time Format
- 12-hour format with `am`/`pm` suffix
- Examples: `11:39pm`, `6:45am`, `12:15am`
- Minutes are optional (e.g., `11pm`)

### Fall Asleep Duration
- Format: `Xmin` or `Xh Ymin`
- Examples: `30min`, `1h 15min`, `45min`

## Sleep Duration Calculation
The app automatically calculates total sleep duration by:
1. Converting sleep time to minutes
2. Adding time to fall asleep
3. Calculating difference with wake time
4. Handling overnight sleep (when wake time is earlier than sleep time)
5. Formatting result as "Xh Ym"

## Future Enhancements

### Phase 4: Enhanced Features
- Statistics (average sleep duration, bedtime, wake time)
- Visualizations (charts and graphs)
- Export/Import functionality

### Phase 5: UI/UX Improvements
- Material Design 3 theming
- Dark mode support
- Date/time picker integration

### Phase 6: Advanced Features
- Notifications and reminders
- Sleep insights and recommendations
- Integration with health services

## Dependencies
- Jetpack Compose for UI
- Room Database for persistence
- Material 3 for design
- ViewModel for state management
- Navigation Compose for navigation

## License
This project is open source and available under the MIT License.

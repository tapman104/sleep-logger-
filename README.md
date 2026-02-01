# Sleep Logger Android App

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.10-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-BOM%202023.10.01-blue.svg)](https://developer.android.com/jetpack/compose)

A simple, privacy-focused Android app to log and track sleep patterns using a specific data format.

## Features

### Core Features (MVP)
- **Single Entry Input**: Individual sleep entry with validation
- **Bulk Input**: Import multiple entries at once
- **Automatic Calculations**: Sleep duration with overnight handling
- **Data Persistence**: Local Room database storage
- **Edit/Delete**: Full CRUD operations
- **Material 3 UI**: Modern, clean interface

### Data Format
```
date-----sleep_time+time_to_fall_asleep-------wake_time
Example: 8-1-26-----11:39pm+30min-------6:45am
```

## Screenshots

<!-- Add screenshots here when available -->
*Coming soon*

## Getting Started

### Prerequisites
- Android Studio (latest version)
- Android SDK (API 24+)
- Java 8 or higher

### Installation

#### From Google Play Store
<!-- Add link when published -->
*Coming soon to Google Play Store*

#### From Source
1. Clone the repository:
   ```bash
   git clone https://github.com/tapman104/SleepLogger.git
   cd SleepLogger
   ```

2. Open in Android Studio

3. Build and run the app

#### Download APK
Download the latest release from the [Releases](https://github.com/tapman104/SleepLogger/releases) page.

## Technical Stack

- **Architecture**: MVVM with Repository pattern
- **UI**: Jetpack Compose with Material 3
- **Database**: Room for local persistence
- **Language**: Kotlin
- **Minimum SDK**: API 24 (Android 7.0)
- **Target SDK**: API 34

## Usage

### Adding Single Entry
1. Tap the `+` floating action button
2. Fill in the date, sleep time, time to fall asleep, and wake time
3. Tap "Save"

### Bulk Input
1. Tap the `📝` floating action button
2. Enter multiple entries, one per line
3. Use the format: `date-----sleep_time+minutes-------wake_time`
4. Tap "Save Entries"

### Managing Entries
- **View**: All entries displayed in reverse chronological order
- **Edit**: Tap on an entry to expand, then tap "Edit"
- **Delete**: Tap on an entry to expand, then tap "Delete"

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

## Building

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### APK Location
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Testing

### Running Tests
```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest
```

### Test Coverage
- Unit tests for data parsing and calculations
- UI tests for user flows
- Manual testing on various devices

## Contributing

We welcome contributions! Please read our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Quick Start
1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Roadmap

### Phase 1: Analytics Foundation
- [ ] Statistics screen with averages
- [ ] Sleep duration charts
- [ ] Date/time picker integration

### Phase 2: Enhanced UX
- [ ] Dark mode support
- [ ] Export functionality
- [ ] Search and filter

### Phase 3: Advanced Features
- [ ] Sleep quality tracking
- [ ] Smart notifications
- [ ] Health Connect integration

See [Issues](https://github.com/tapman104/SleepLogger/issues) for detailed feature requests.

## Privacy & Security

- **Local Storage**: All data stored locally on device
- **No Internet**: No network connectivity for data
- **No Tracking**: No analytics or user tracking
- **Open Source**: Full code transparency

Read our [Security Policy](SECURITY.md) for more information.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Android Developers](https://developer.android.com) for excellent documentation
- [Material Design](https://material.io) for design guidelines
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for modern UI toolkit

## Support

If you find this app useful, consider supporting its development:

- ☕ [Buy Me a Coffee](https://www.buymeacoffee.com/tapman104)
- ⭐ Star the repository on GitHub

## 📞 Contact

- 📧 Email: support@sleeplogger.app
- 🐛 [Report Issues](https://github.com/tapman104/SleepLogger/issues)
- 💬 [Discussions](https://github.com/tapman104/SleepLogger/discussions)
- 👨‍💻 [Developer: tapman104](https://github.com/tapman104)

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=tapman104/SleepLogger&type=Date)](https://star-history.com/#tapman104/SleepLogger&Date)

---

**Made with ❤️ for better sleep tracking** 🌙

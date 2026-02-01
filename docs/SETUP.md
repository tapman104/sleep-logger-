# Development Setup Guide

## Prerequisites

### Required Software
- **Android Studio**: Latest stable version (Arctic Fox or later)
- **Java Development Kit**: JDK 17 or later
- **Android SDK**: API 24 (Android 7.0) minimum, API 34 target
- **Git**: For version control

### System Requirements
- **RAM**: 8GB minimum (16GB recommended)
- **Storage**: 10GB free space
- **OS**: Windows 10/11, macOS 10.15+, or Linux

## Installation Steps

### 1. Clone the Repository
```bash
git clone https://github.com/tapman104/SleepLogger.git
cd SleepLogger
```

### 2. Open in Android Studio
1. Launch Android Studio
2. Select "Open an existing project"
3. Navigate to the cloned directory
4. Select the `SleepLogger` folder
5. Wait for Gradle sync to complete

### 3. Configure SDK
1. Go to **File → Settings → Appearance & Behavior → System Settings → Android SDK**
2. Ensure you have:
   - Android SDK Platform 34 (or latest)
   - Android SDK Build-Tools 34.0.0 (or latest)
   - Android SDK Platform-Tools
   - Android SDK Tools

### 4. Create Virtual Device (Optional)
1. Go to **Tools → Device Manager**
2. Click "Create device"
3. Select a phone model (e.g., Pixel 6)
4. Select a system image (API 30+ recommended)
5. Finish setup

## Project Configuration

### Gradle Settings
The project uses:
- **Gradle Version**: 8.4
- **Kotlin Version**: 1.9.10
- **Compose BOM**: 2023.10.01

### Build Variants
- **debug**: Development build with debugging enabled
- **release**: Production build optimized for distribution

## Running the App

### On Emulator
1. Select an emulator from the device dropdown
2. Click the green "Run" button (or `Shift + F10`)
3. Wait for build and installation

### On Physical Device
1. Enable Developer Options on your device
2. Enable USB Debugging
3. Connect device via USB
4. Select device from dropdown
5. Click "Run"

## Building APKs

### Debug APK
```bash
./gradlew assembleDebug
```
Location: `app/build/outputs/apk/debug/app-debug.apk`

### Release APK
```bash
./gradlew assembleRelease
```
Location: `app/build/outputs/apk/release/app-release.apk`

### Signed APK
1. Go to **Build → Generate Signed Bundle / APK**
2. Select APK
3. Create or use existing keystore
4. Follow the signing wizard

## Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Run Lint
```bash
./gradlew lint
```

## Project Structure Overview

```
app/
├── src/
│   ├── main/
│   │   ├── java/com/sleeplogger/app/
│   │   │   ├── MainActivity.kt          # Main activity
│   │   │   ├── SleepEntry.kt           # Data model
│   │   │   ├── database/               # Room database
│   │   │   ├── repository/             # Data repository
│   │   │   ├── viewmodel/              # ViewModels
│   │   │   └── ui/components/          # Compose UI
│   │   ├── res/                        # Resources
│   │   └── AndroidManifest.xml        # App manifest
│   ├── test/                          # Unit tests
│   └── androidTest/                   # Instrumented tests
├── build.gradle.kts                   # App build config
└── proguard-rules.pro                 # ProGuard rules
```

## Development Workflow

### 1. Feature Development
1. Create feature branch: `git checkout -b feature/your-feature`
2. Make changes
3. Run tests: `./gradlew test connectedAndroidTest`
4. Run lint: `./gradlew lint`
5. Commit changes
6. Push branch
7. Create pull request

### 2. Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Add comments for complex logic
- Keep composables small and focused

### 3. Testing Requirements
- Unit tests for business logic
- UI tests for user flows
- Manual testing on different devices

## Common Issues

### Gradle Sync Issues
- **Solution**: Check internet connection, clear Gradle cache
```bash
./gradlew clean build --refresh-dependencies
```

### Build Errors
- **Solution**: Check SDK installation, update build tools
- Verify `local.properties` has correct SDK path

### Emulator Issues
- **Solution**: Enable hardware acceleration in BIOS
- Use x86 images for better performance

## Debugging

### Using Android Studio Debugger
1. Set breakpoints by clicking in the gutter
2. Click the "Debug" button (or `Shift + F9`)
3. Use Debug window to inspect variables

### View Database
1. Go to **View → Tool Windows → Database Inspector**
2. Select running app
3. Browse sleep_entries table

### Logcat
1. Go to **View → Tool Windows → Logcat**
2. Filter by package name: `com.sleeplogger.app`
3. View real-time logs

## Performance Tips

### Build Performance
- Enable Gradle daemon
- Use incremental compilation
- Allocate more memory to Gradle

### App Performance
- Use Compose performance tools
- Monitor memory usage
- Profile with Android Profiler

## Contributing

Before contributing:
1. Read [CONTRIBUTING.md](../CONTRIBUTING.md)
2. Fork the repository
3. Create feature branch
4. Write tests
5. Submit pull request

## Getting Help

- **Documentation**: [README.md](../README.md)
- **Issues**: [GitHub Issues](https://github.com/tapman104/SleepLogger/issues)
- **Discussions**: [GitHub Discussions](https://github.com/tapman104/SleepLogger/discussions)

## Next Steps

After setup:
1. Run the app to verify installation
2. Explore the codebase
3. Try adding a test entry
4. Read the [API Documentation](API.md)
5. Check the [Roadmap](../README.md#roadmap)

Happy coding! 🚀

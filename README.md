# MineConsole (Remote Server Manager)

MineConsole is an open-source remote server management client designed for Minecraft administrators to monitor live consoles, execute secure RCON terminal commands, and configure role-based access for guest moderators.

This workspace consists of a **native double-architecture**:
1. **`MineConsole-iOS/`**: A native Apple Swift 5.10 + SwiftUI + SwiftData codebase utilizing `LocalAuthentication` and `Security` Keychains that is ready to compile in Xcode.
2. **`MineConsole-Android/`**: A native Android Kotlin 2.0 + Jetpack Compose + Room Database + EncryptedSharedPreferences codebase ready to compile in Android Studio.

---

## Codebase Structure

```
c:\Users\Admin\Desktop\MCconsole\
├── MineConsole-iOS/                 # Native Apple iOS App Files
│   ├── MineConsoleApp.swift         # Core entry point (sets up SwiftData & FaceID lock)
│   ├── Models/
│   │   └── ServerProfile.swift      # SwiftData profile schema (persists RCON meta)
│   ├── Network/
│   │   └── RCONClient.swift         # Network.framework asynchronous RCONTCP socket client
│   ├── Security/
│   │   └── KeychainHelper.swift     # Keychain generic password encryptor/wrapper
│   └── Views/
│       ├── DashboardView.swift      # Server profile grid listing and setup wizard sheet
│       ├── ConsoleView.swift        # Auto-scrolling logs terminal & custom hotkey keypads
│       ├── SettingsView.swift       # Biometric configuration & share token generator
│       └── FaceIDAuthView.swift     # Biometric verification overlay splash with circular scanners
└── MineConsole-Android/             # Native Android Kotlin App Files
    ├── build.gradle.kts             # Top-level build config
    ├── settings.gradle.kts          # Module loading
    ├── gradle.properties            # JVM configs
    └── app/
        ├── build.gradle.kts         # Dependency configurations (Room, Compose, Crypto)
        └── src/main/
            ├── AndroidManifest.xml  # Security permissions declarations
            └── java/com/raul1x9/mineconsole/
                ├── MainActivity.kt  # Host activity, routing & biometric unlock
                ├── models/          # Server profile entities
                ├── data/            # Room local database DAO
                ├── security/        # EncryptedSharedPreferences key crypto
                ├── network/         # Asynchronous little-endian coroutine TCP RconClient
                ├── viewmodels/      # MainViewModel bridging DB and UI state
                └── views/           # Jetpack Compose Screens (Dashboard, Console, Settings, Biometric)
```

---

## 📱 Component 1: Native iOS App Setup & Sideloading

You can run and install the native iOS app in two ways: by downloading the pre-compiled `.ipa` file directly, or by building the code yourself.

### Method A: Install Pre-Compiled IPA via Sideloadly (No Mac Required)

If you don't have a Mac or a paid Apple Developer Account, you can sideload the pre-compiled app for free on Windows:

1. **Download the IPA**: Go to the **Releases** tab on this GitHub repository and download the latest compiled `MineConsole.ipa` file.
2. **Install Sideloadly**: Download and install **[Sideloadly](https://sideloadly.io/)** on your Windows PC. Also ensure you have the desktop version of [iTunes](https://www.apple.com/itunes/) installed.
3. **Connect Device**: Plug your iPhone into your PC via USB and trust the computer.
4. **Sideload**:
   * Open Sideloadly (it will automatically detect your iPhone).
   * Drag the downloaded `MineConsole.ipa` into the IPA slot.
   * Enter your free Apple ID email address under "Apple Account".
   * Click **Start**, and enter your Apple ID password and 2FA code when prompted.
5. **Trust the Certificate**:
   * On your iPhone, go to **Settings > General > VPN & Device Management**.
   * Under "Developer App", tap your Apple ID and tap **Trust**.
   * Open the **MineConsole** app on your iPhone and enjoy your 7-day personal installation!

### Method B: Build the Native App via Xcode (Requires Mac)

To compile the source code yourself:
1. Open **Xcode** on a macOS machine.
2. Create a new **SwiftUI Application** project named `MineConsole` (Bundle prefix: `com.raul1x9`).
3. Add the files inside `MineConsole-iOS/` into the Xcode project navigator (keeping the folder layout).
4. Go to project settings, and under **Signing & Capabilities**:
   * Add the **Keychain Sharing** capability.
5. In your target settings, ensure that **Generate Info.plist File** is enabled (`GENERATE_INFOPLIST_FILE = YES`) and the following keys are added:
   * `NSFaceIDUsageDescription`: *"MineConsole requires FaceID to secure RCON administrator permissions."*
   * `NSLocalNetworkUsageDescription`: *"MineConsole requires local network access to communicate securely with Minecraft RCON servers over your network or Tailscale VPN."*
6. Press `Cmd + R` to compile and run on the iOS Simulator or a physical Apple device.

---

## 🤖 Component 2: Native Android App Setup & APK Compilation

To build and run the native Android Kotlin client on your Windows PC:

### Prerequisites
*   Install **[Android Studio](https://developer.android.com/studio)** on your Windows PC.
*   An Android Device (connected via USB with **USB Debugging** enabled under Developer Options) or a running Virtual Device (Emulator).

### Compilation Steps
1. **Open the Project**:
   * Launch **Android Studio**.
   * Select **Open** and choose the `c:\Users\Admin\Desktop\MCconsole\MineConsole-Android` folder.
2. **Build and Sync**:
   * Wait for Android Studio to index your project and run the **Gradle Sync** automatically.
   * Ensure that the sync completes successfully without any errors.
3. **Compile the APK**:
   * From the top menu bar, select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   * Once finished, Android Studio will display a bubble notification: *"APK(s) generated successfully: Locate."*
   * Click **Locate** to open your folder and retrieve your clean, compiled **`app-debug.apk`**!
4. **Deploy to Device**:
   * You can install this `.apk` directly onto any Android phone.
   * If your phone is connected via USB, select your phone in Android Studio's device dropdown list at the top and click the green **Run (Play)** button (`Shift + F10`) to compile and launch the app in real-time!

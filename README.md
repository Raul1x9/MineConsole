# MineConsole (Remote Server Manager)

MineConsole is an open-source remote server management client designed for Minecraft administrators to monitor live consoles, execute secure RCON terminal commands, and configure role-based access for guest moderators.

This workspace consists of a **double-layer architecture**:
1. **`MineConsole-iOS/`**: A native Apple Swift 5.10 + SwiftUI + SwiftData codebase utilizing `LocalAuthentication` and `Security` Keychains that is ready to compile in Xcode.
2. **`MineConsole-WebSimulator/`**: An interactive web-based simulator designed inside a glassmorphism iPhone shell. This allows you to fully test the visual styling, simulated logs, FaceID scans, Tailscale disconnect alerts, and role-based sharing before going to iOS. It also contains an **optional Node.js TCP Proxy** allowing you to link the simulator directly to *real* live Minecraft RCON servers!

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
└── MineConsole-WebSimulator/        # Interactive iOS Simulator & RCON Web Gateway
    ├── index.html                   # HTML5 layout (includes glassmorphic iPhone container)
    ├── style.css                    # Premium styles (FaceID concentric scanning, blinking cursor)
    ├── app.js                       # Logic (audio synthesizer, Minecraft logs generator, command parser)
    ├── package.json                 # Node server requirements
    └── server.js                    # Local websocket-to-RCON proxy bridge
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
   * Click **Start**, and enter your Apple ID password and 2FA code when prompted (Sideloadly communicates directly with Apple's secure servers to sign the app locally).
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

## 🌐 Component 2: Interactive Web Simulator & Proxy

The web simulator functions completely standalone in the browser (running simulations of Minecraft terminal logs, RCON commands, Tailscale VPN drops, and guest links).

### Running Offline (Standalone Simulator)
1. Open the [index.html](file:///c:/Users/Admin/Desktop/MCconsole/MineConsole-WebSimulator/index.html) file directly in any modern browser.
2. Experience:
   - **FaceID Unlock**: Press "Authenticate System" to watch the biometric grid scanning sweep with audio synth sounds.
   - **Interactive Console**: Click on "SURVIVAL SERVER" to load the live log stream. Type commands like `/op notch`, `/kick steve`, `/ban herobrine`, `/stop`, `/tps` to inspect active mock response outputs!
   - **VPN Interruption Alerts**: Toggle the **Tailscale VPN status** switch in the left control panel to see the console break instantly and raise the top warning banner.
   - **Guest Roles Lab**: Select a role (Viewer, Moderator, Admin), generate a secure link token, and press "Launch Client Link" to view custom constraints!

### Running Live (Real RCON Proxy Connection)
If you want to connect the web-based MineConsole directly to a **real, active Minecraft server** over RCON:
1. Open your terminal in the `MineConsole-WebSimulator` directory:
   ```bash
   cd MineConsole-WebSimulator
   ```
2. Install the necessary lightweight WebSocket packages:
   ```bash
   npm install
   ```
3. Boot the local gateway server:
   ```bash
   npm start
   ```
4. Open `http://localhost:3000` in your web browser.
5. You will see **Proxy Mode: Connected** (green) in the left panel.
6. Click the "+" icon inside the iPhone Simulator dashboard to add your real server parameters (IP, Port, and RCON Password).
7. Tap the server card. The Node proxy will immediately establish a TCP connection, authenticate RCON, and stream **real live logs** right into your simulator, executing real server commands!

---

## 🤖 Component 3: Android App Setup & APK Compilation

Since the web simulator is a responsive, highly polished mobile interface, you can package it into a native Android app (`.apk`) using **Capacitor** in just a few steps directly on your Windows PC:

### Prerequisites
*   Install **[Android Studio](https://developer.android.com/studio)** on your Windows PC.
*   Make sure you have **Node.js** installed.

### Packaging Steps
1. Open your terminal in the web simulator folder:
   ```bash
   cd MineConsole-WebSimulator
   ```
2. Install Capacitor Core and CLI:
   ```bash
   npm install @capacitor/core @capacitor/cli
   ```
3. Initialize the Capacitor app config:
   ```bash
   npx cap init MineConsole com.raul1x9.mineconsole --web-dir=.
   ```
   *(When prompted, set **Web asset directory** to `.` since your HTML, CSS, and JS are in the root of the simulator folder).*
4. Install the Android platform and add it to your project:
   ```bash
   npm install @capacitor/android
   npx cap add android
   ```
5. Open the project in **Android Studio**:
   ```bash
   npx cap open android
   ```
6. **Compile your APK**:
   * Inside Android Studio, wait for Gradle to finish syncing.
   * From the top menu, select **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
   * Android Studio will compile the code and show a popup saying: *"APK(s) generated successfully: Locate."*
   * Click **Locate** to find your compiled `app-debug.apk` file!
   * Transfer this `.apk` file to your Android phone, install it, and run the simulator as a native app!


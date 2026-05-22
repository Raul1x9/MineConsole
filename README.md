# MineConsole (Remote Server Manager)

MineConsole is an ultra-premium remote server management client designed for Minecraft administrators to monitor live consoles, execute secure RCON terminal commands, and configure role-based access for guest moderators.

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

## 📱 Component 1: Native iOS App Compilation

To run the native iOS app:
1. Open **Xcode** on a macOS machine.
2. Create a new **SwiftUI Application** project named `MineConsole`.
3. Add the files inside `MineConsole-iOS/` into the Xcode project navigator (keeping the folder layout).
4. Go to project settings, and under **Signing & Capabilities**:
   - Add **Keychain Sharing** (if running shared group profiles).
5. In your `Info.plist`, ensure you declare the following security strings:
   - `NSFaceIDUsageDescription`: *"MineConsole requires FaceID to secure RCON administrator permissions."*
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

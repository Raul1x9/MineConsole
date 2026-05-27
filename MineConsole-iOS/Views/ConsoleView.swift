import SwiftUI
import UIKit

struct ConsoleView: View {
    let server: ServerProfile
    @StateObject private var rcon = RCONClient()
    @ObservedObject private var logManager = ConsoleLogManager.shared
    
    @State private var commandInput = ""
    @State private var commandHistory: [String] = []
    @State private var showingHistory = false
    
    // Theme preferences
    @AppStorage("appTheme") private var appTheme = "System"
    @AppStorage("appAccentColor") private var appAccentColor = "Green"
    @Environment(\.colorScheme) private var systemColorScheme
    
    // Haptic Feedbacks
    private let impactLight = UIImpactFeedbackGenerator(style: .light)
    private let impactRigid = UIImpactFeedbackGenerator(style: .rigid)
    private let notificationFeedback = UINotificationFeedbackGenerator()
    
    // Standard MC Commands for Autocomplete
    private let autocompleteCommands = [
        "/help", "/give", "/gamemode", "/tp", "/time", "/weather", "/difficulty", "/spawnpoint",
        "/setworldspawn", "/gamerule", "/clear", "/effect", "/enchant", "/experience", "/xp",
        "/kill", "/say", "/tell", "/msg", "/w", "/summon", "/fill", "/clone", "/locate", "/locatebiome",
        "/seed", "/kick", "/ban", "/pardon", "/ban-ip", "/pardon-ip", "/op", "/deop", "/whitelist",
        "/list", "/save-all", "/save-off", "/save-on", "/stop", "/tellraw", "/title", "/attribute", "/bossbar"
    ]
    
    private var isDark: Bool {
        switch appTheme {
        case "Light": return false
        case "Dark": return true
        default: return systemColorScheme == .dark
        }
    }
    
    private var colors: ThemeColors {
        ThemeManager.getThemeColors(themeName: appTheme, isSystemDark: systemColorScheme == .dark)
    }
    
    private var accentColor: Color {
        ThemeManager.getAccentColor(name: appAccentColor)
    }
    
    private var filteredSuggestions: [String] {
        guard !commandInput.isEmpty else { return [] }
        let trimmed = commandInput.lowercased()
        return autocompleteCommands.filter { cmd in
            cmd.lowercased().hasPrefix(trimmed) && cmd.lowercased() != trimmed
        }
    }
    
    var body: some View {
        ZStack {
            colors.background.ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Connection Status Header Bar
                HStack {
                    Circle()
                        .fill(rcon.isAuthenticated ? accentColor : Color.red)
                        .frame(width: 8, height: 8)
                        .shadow(color: (rcon.isAuthenticated ? accentColor : Color.red).opacity(0.8), radius: 4)
                    
                    Text(rcon.isAuthenticated ? "CONNECTED - \(server.ip)" : "DISCONNECTED")
                        .font(.custom("Courier-Bold", size: 10))
                        .foregroundColor(rcon.isAuthenticated ? accentColor : .red)
                    
                    Spacer()
                    
                    Text("ROLE: \(server.sharedRole.uppercased())")
                        .font(.custom("Courier", size: 10))
                        .foregroundColor(colors.subText)
                }
                .padding()
                .background(colors.cardBackground)
                
                // Real-time scrolling terminal screen
                ScrollViewReader { proxy in
                    ScrollView {
                        VStack(alignment: .leading, spacing: 6) {
                            let serverLogs = logManager.getLogs(for: server.id)
                            ForEach(0..<serverLogs.count, id: \.self) { index in
                                Text(serverLogs[index])
                                    .font(.custom("Courier", size: 12))
                                    .foregroundColor(getLogColor(for: serverLogs[index]))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .id(index)
                            }
                        }
                        .padding()
                    }
                    .onChange(of: logManager.getLogs(for: server.id).count) {
                        if let lastIndex = logManager.getLogs(for: server.id).indices.last {
                            withAnimation {
                                proxy.scrollTo(lastIndex, anchor: .bottom)
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(isDark ? Color(white: 0.02) : Color.white)
                .border(colors.border, width: 1)
                
                // Fast-Access Preset Command Keypad (only for Mod & Admin)
                if server.sharedRole != "Viewer" {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            PresetButton(label: "/list", accentColor: accentColor, colors: colors) { sendPresetCommand("/list") }
                            PresetButton(label: "/tps", accentColor: accentColor, colors: colors) { sendPresetCommand("/tps") }
                            PresetButton(label: "/weather clear", accentColor: accentColor, colors: colors) { sendPresetCommand("/weather clear") }
                            PresetButton(label: "/time set day", accentColor: accentColor, colors: colors) { sendPresetCommand("/time set day") }
                            if server.sharedRole == "Admin" {
                                PresetButton(label: "/say Alert!", accentColor: accentColor, colors: colors) { sendPresetCommand("/say [Admin Alert]") }
                            }
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 8)
                    }
                    .background(colors.cardBackground)
                }
                
                // Horizontal Autocomplete suggestions bar
                if !filteredSuggestions.isEmpty {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 8) {
                            ForEach(filteredSuggestions, id: \.self) { suggestion in
                                Button(action: {
                                    impactLight.impactOccurred()
                                    commandInput = suggestion + " "
                                }) {
                                    Text(suggestion)
                                        .font(.custom("Courier-Bold", size: 12))
                                        .foregroundColor(accentColor)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(accentColor.opacity(0.1))
                                        .cornerRadius(6)
                                        .overlay(RoundedRectangle(cornerRadius: 6).stroke(accentColor.opacity(0.3), lineWidth: 1))
                                }
                            }
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 8)
                    }
                    .background(colors.cardBackground)
                    .transition(.opacity.combined(with: .move(edge: .bottom)))
                }
                
                // Bottom Input Console Pane
                HStack(spacing: 12) {
                    if server.sharedRole == "Viewer" {
                        // Viewer Mode UI Blocked
                        HStack {
                            Image(systemName: "lock.fill")
                               .foregroundColor(colors.subText)
                            Text("CONSOLE IS VIEW-ONLY (RESTRICTED)")
                                .font(.custom("Courier-Bold", size: 11))
                                .foregroundColor(colors.subText)
                        }
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(colors.border)
                    } else {
                        // Interactive RCON Command Terminal Box
                        Button(action: { showingHistory = true }) {
                            Image(systemName: "clock.arrow.circlepath")
                                .foregroundColor(accentColor)
                                .font(.system(size: 20))
                        }
                        
                        TextField("TYPE COMMAND...", text: $commandInput)
                            .font(.custom("Courier", size: 14))
                            .padding(.vertical, 10)
                            .padding(.horizontal, 12)
                            .background(colors.border)
                            .cornerRadius(6)
                            .foregroundColor(colors.text)
                            .textInputAutocapitalization(.never)
                            .disableAutocorrection(true)
                            .onSubmit(executeCommand)
                        
                        Button(action: executeCommand) {
                            Image(systemName: "paperplane.fill")
                                .foregroundColor(isDark ? .black : .white)
                                .padding(10)
                                .background(accentColor)
                                .cornerRadius(6)
                        }
                    }
                }
                .padding()
                .background(colors.cardBackground)
            }
        }
        .navigationTitle(server.name.uppercased())
        .navigationBarTitleDisplayMode(.inline)
        .onAppear {
            connectToRCON()
            UIApplication.shared.isIdleTimerDisabled = true
        }
        .onDisappear {
            disconnectFromRCON()
            UIApplication.shared.isIdleTimerDisabled = false
        }
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(action: {
                    impactRigid.impactOccurred()
                    ConsoleLogManager.shared.clearLogs(for: server.id)
                }) {
                    Image(systemName: "trash")
                        .foregroundColor(.red)
                }
            }
        }
        .sheet(isPresented: $showingHistory) {
            CommandHistorySheet(history: $commandHistory, selectedCommand: $commandInput, isPresented: $showingHistory, colors: colors, accentColor: accentColor)
        }
    }
    
    private func connectToRCON() {
        // Read decrypted password from keychain
        guard let pass = KeychainHelper.shared.readString(service: "MineConsole", account: server.keychainKey) else {
            ConsoleLogManager.shared.addLog(for: server.id, message: "[Error] Keychain reference missing password credentials.")
            return
        }
        
        rcon.connect(host: server.ip, port: server.rconPort, password: pass, serverId: server.id)
    }
    
    private func disconnectFromRCON() {
        rcon.disconnect()
    }
    
    private func sendPresetCommand(_ cmd: String) {
        commandInput = cmd
        executeCommand()
    }
    
    private func executeCommand() {
        let cmd = commandInput.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cmd.isEmpty else { return }
        
        // Haptic feedback
        impactRigid.impactOccurred()
        
        // Permission restrictions check for Moderator
        if server.sharedRole == "Moderator" {
            let destructiveCommands = ["/stop", "/ban", "/op", "/deop", "/whitelist"]
            let lowerCmd = cmd.lowercased()
            
            for dCmd in destructiveCommands {
                if lowerCmd.hasPrefix(dCmd) {
                    notificationFeedback.notificationOccurred(.error)
                    ConsoleLogManager.shared.addLog(for: server.id, message: "[Security Alert] Command \(dCmd) blocked: Moderator privilege level exceeded.")
                    commandInput = ""
                    return
                }
            }
        }
        
        ConsoleLogManager.shared.addLog(for: server.id, message: "> \(cmd)")
        
        // Send command through TCP socket client
        rcon.sendCommand(cmd) { response, error in
            DispatchQueue.main.async {
                if let error = error {
                    self.notificationFeedback.notificationOccurred(.error)
                    ConsoleLogManager.shared.addLog(for: self.server.id, message: "[Error] Command delivery failed: \(error.localizedDescription)")
                } else {
                    self.impactLight.impactOccurred()
                    if let cmdHistoryLast = self.commandHistory.last, cmdHistoryLast == cmd {
                        // avoid duplicate history entries
                    } else {
                        self.commandHistory.append(cmd)
                    }
                }
            }
        }
        
        commandInput = ""
    }
    
    // Dynamic color coding for logs
    private func getLogColor(for line: String) -> Color {
        if line.hasPrefix(">") {
            return isDark ? accentColor : Color(red: 0.0, green: 0.5, blue: 0.2)
        } else if line.contains("[Error]") || line.contains("[Security Alert]") {
            return isDark ? .red : Color(red: 0.8, green: 0.0, blue: 0.0)
        } else if line.contains("[System]") {
            return isDark ? Color(red: 1.0, green: 0.8, blue: 0.0) : Color(red: 0.7, green: 0.4, blue: 0.0)
        } else if line.contains("joined the game") || line.contains("left the game") {
            return isDark ? Color(red: 0.4, green: 0.7, blue: 1.0) : Color(red: 0.0, green: 0.3, blue: 0.8)
        }
        return colors.text
    }
}

// Preset Button Cell
struct PresetButton: View {
    let label: String
    let accentColor: Color
    let colors: ThemeColors
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.custom("Courier-Bold", size: 11))
                .foregroundColor(accentColor)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(accentColor.opacity(0.1))
                .cornerRadius(4)
                .overlay(RoundedRectangle(cornerRadius: 4).stroke(accentColor.opacity(0.3), lineWidth: 1))
        }
    }
}

// Command History popup sheet
struct CommandHistorySheet: View {
    @Binding var history: [String]
    @Binding var selectedCommand: String
    @Binding var isPresented: Bool
    let colors: ThemeColors
    let accentColor: Color
    
    var body: some View {
        NavigationStack {
            ZStack {
                colors.background.ignoresSafeArea()
                
                if history.isEmpty {
                    VStack {
                        Image(systemName: "clock")
                            .font(.system(size: 40))
                            .foregroundColor(colors.subText)
                        Text("HISTORY_EMPTY")
                            .font(.custom("Courier-Bold", size: 14))
                            .foregroundColor(colors.subText)
                            .padding(.top, 10)
                    }
                } else {
                    List {
                        ForEach(history, id: \.self) { cmd in
                            Button(action: {
                                selectedCommand = cmd
                                isPresented = false
                            }) {
                                HStack {
                                    Image(systemName: "terminal")
                                        .foregroundColor(accentColor)
                                    Text(cmd)
                                        .font(.custom("Courier", size: 14))
                                        .foregroundColor(colors.text)
                                    Spacer()
                                }
                            }
                            .listRowBackground(colors.cardBackground)
                        }
                    }
                    .background(colors.background)
                    .scrollContentBackground(.hidden)
                }
            }
            .navigationTitle("COMMAND_HISTORY")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("CLOSE") { isPresented = false }
                        .font(.custom("Courier-Bold", size: 12))
                        .foregroundColor(accentColor)
                }
            }
        }
    }
}

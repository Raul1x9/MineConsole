import SwiftUI
import UIKit

struct ConsoleView: View {
    let server: ServerProfile
    @StateObject private var rcon = RCONClient()
    
    @State private var commandInput = ""
    @State private var commandHistory: [String] = []
    @State private var showingHistory = false
    
    // Haptic Feedbacks
    private let impactLight = UIImpactFeedbackGenerator(style: .light)
    private let impactRigid = UIImpactFeedbackGenerator(style: .rigid)
    private let notificationFeedback = UINotificationFeedbackGenerator()
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Connection Status Header Bar
                HStack {
                    Circle()
                        .fill(rcon.isAuthenticated ? Color.green : Color.red)
                        .frame(width: 8, height: 8)
                        .shadow(color: (rcon.isAuthenticated ? Color.green : Color.red).opacity(0.8), radius: 4)
                    
                    Text(rcon.isAuthenticated ? "CONNECTED - \(server.ip)" : "DISCONNECTED")
                        .font(.custom("Courier-Bold", size: 10))
                        .foregroundColor(rcon.isAuthenticated ? .green : .red)
                    
                    Spacer()
                    
                    Text("ROLE: \(server.sharedRole.uppercased())")
                        .font(.custom("Courier", size: 10))
                        .foregroundColor(.white.opacity(0.6))
                }
                .padding()
                .background(Color.white.opacity(0.02))
                
                // Real-time scrolling terminal screen
                ScrollViewReader { proxy in
                    ScrollView {
                        VStack(alignment: .leading, spacing: 6) {
                            ForEach(0..<rcon.logStream.count, id: \.self) { index in
                                Text(rcon.logStream[index])
                                    .font(.custom("Courier", size: 12))
                                    .foregroundColor(getLogColor(for: rcon.logStream[index]))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                    .id(index)
                            }
                        }
                        .padding()
                    }
                    .onChange(of: rcon.logStream.count) {
                        if let lastIndex = rcon.logStream.indices.last {
                            withAnimation {
                                proxy.scrollTo(lastIndex, anchor: .bottom)
                            }
                        }
                    }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(Color(white: 0.02))
                .border(Color.white.opacity(0.05), width: 1)
                
                // Fast-Access Preset Command Keypad (only for Mod & Admin)
                if server.sharedRole != "Viewer" {
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 10) {
                            PresetButton(label: "/list") { sendPresetCommand("/list") }
                            PresetButton(label: "/tps") { sendPresetCommand("/tps") }
                            PresetButton(label: "/weather clear") { sendPresetCommand("/weather clear") }
                            PresetButton(label: "/time set day") { sendPresetCommand("/time set day") }
                            if server.sharedRole == "Admin" {
                                PresetButton(label: "/say Alert!") { sendPresetCommand("/say [Admin Alert]") }
                            }
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 8)
                    }
                    .background(Color.white.opacity(0.01))
                }
                
                // Bottom Input Console Pane
                HStack(spacing: 12) {
                    if server.sharedRole == "Viewer" {
                        // Viewer Mode UI Blocked
                        HStack {
                            Image(systemName: "lock.fill")
                                .foregroundColor(.white.opacity(0.4))
                            Text("CONSOLE IS VIEW-ONLY (RESTRICTED)")
                                .font(.custom("Courier-Bold", size: 11))
                                .foregroundColor(.white.opacity(0.4))
                        }
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color.white.opacity(0.04))
                    } else {
                        // Interactive RCON Command Terminal Box
                        Button(action: { showingHistory = true }) {
                            Image(systemName: "clock.arrow.circlepath")
                                .foregroundColor(.green)
                                .font(.system(size: 20))
                        }
                        
                        TextField("TYPE COMMAND...", text: $commandInput)
                            .font(.custom("Courier", size: 14))
                            .padding(.vertical, 10)
                            .padding(.horizontal, 12)
                            .background(Color.white.opacity(0.05))
                            .cornerRadius(6)
                            .foregroundColor(.white)
                            .textInputAutocapitalization(.never)
                            .disableAutocorrection(true)
                            .onSubmit(executeCommand)
                        
                        Button(action: executeCommand) {
                            Image(systemName: "paperplane.fill")
                                .foregroundColor(.black)
                                .padding(10)
                                .background(Color.green)
                                .cornerRadius(6)
                        }
                    }
                }
                .padding()
                .background(Color.white.opacity(0.02))
            }
        }
        .navigationTitle(server.name.uppercased())
        .navigationBarTitleDisplayMode(.inline)
        .onAppear(perform: connectToRCON)
        .onDisappear(perform: disconnectFromRCON)
        .sheet(isPresented: $showingHistory) {
            CommandHistorySheet(history: $commandHistory, selectedCommand: $commandInput, isPresented: $showingHistory)
        }
    }
    
    private func connectToRCON() {
        // Read decrypted password from keychain
        guard let pass = KeychainHelper.shared.readString(service: "MineConsole", account: server.keychainKey) else {
            rcon.logStream.append("[Error] Keychain reference missing password credentials.")
            return
        }
        
        rcon.connect(host: server.ip, port: server.rconPort, password: pass)
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
                    rcon.logStream.append("[Security Alert] Command \(dCmd) blocked: Moderator privilege level exceeded.")
                    commandInput = ""
                    return
                }
            }
        }
        
        rcon.logStream.append("> \(cmd)")
        
        // Send command through TCP socket client
        rcon.sendCommand(cmd) { response, error in
            DispatchQueue.main.async {
                if let error = error {
                    self.notificationFeedback.notificationOccurred(.error)
                    self.rcon.logStream.append("[Error] Command delivery failed: \(error.localizedDescription)")
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
            return .green
        } else if line.contains("[Error]") || line.contains("[Security Alert]") {
            return .red
        } else if line.contains("[System]") {
            return .yellow
        } else if line.contains("joined the game") || line.contains("left the game") {
            return .blue
        }
        return .white
    }
}

// Preset Button Cell
struct PresetButton: View {
    let label: String
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.custom("Courier-Bold", size: 11))
                .foregroundColor(.green)
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(Color.green.opacity(0.1))
                .cornerRadius(4)
                .overlay(RoundedRectangle(cornerRadius: 4).stroke(Color.green.opacity(0.3), lineWidth: 1))
        }
    }
}

// Command History popup sheet
struct CommandHistorySheet: View {
    @Binding var history: [String]
    @Binding var selectedCommand: String
    @Binding var isPresented: Bool
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()
                
                if history.isEmpty {
                    VStack {
                        Image(systemName: "clock")
                            .font(.system(size: 40))
                            .foregroundColor(.white.opacity(0.2))
                        Text("HISTORY_EMPTY")
                            .font(.custom("Courier-Bold", size: 14))
                            .foregroundColor(.white.opacity(0.4))
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
                                        .foregroundColor(.green)
                                    Text(cmd)
                                        .font(.custom("Courier", size: 14))
                                        .foregroundColor(.white)
                                    Spacer()
                                }
                            }
                            .listRowBackground(Color.white.opacity(0.02))
                        }
                    }
                    .background(Color.black)
                    .scrollContentBackground(.hidden)
                }
            }
            .navigationTitle("COMMAND_HISTORY")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("CLOSE") { isPresented = false }
                        .font(.custom("Courier-Bold", size: 12))
                        .foregroundColor(.green)
                }
            }
        }
    }
}

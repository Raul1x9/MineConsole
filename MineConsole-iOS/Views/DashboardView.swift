import SwiftUI
import SwiftData

struct DashboardView: View {
    @Environment(\.modelContext) private var modelContext
    @Query(sort: \ServerProfile.creationDate, order: .reverse) private var servers: [ServerProfile]
    
    @State private var showingAddSheet = false
    @State private var showingSettings = false
    @State private var selectedServer: ServerProfile? = nil
    
    // Add Server Fields
    @State private var newName = ""
    @State private var newIP = ""
    @State private var newPort = "25575"
    @State private var newPassword = ""
    
    // Tailscale Mock State
    @State private var tailscaleConnected = true
    
    // Theme preferences
    @AppStorage("appTheme") private var appTheme = "System"
    @AppStorage("appAccentColor") private var appAccentColor = "Green"
    @Environment(\.colorScheme) private var systemColorScheme
    
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
    
    var body: some View {
        NavigationStack {
            ZStack {
                if isDark {
                    LinearGradient(colors: [Color(white: 0.05), Color(white: 0.12)], startPoint: .top, endPoint: .bottom)
                        .ignoresSafeArea()
                } else {
                    colors.background.ignoresSafeArea()
                }
                
                VStack(spacing: 20) {
                    // Tailscale Alert Banner
                    if !tailscaleConnected {
                        HStack {
                            Image(systemName: "exclamationmark.triangle.fill")
                                .foregroundColor(.orange)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("TAILSCALE DISCONNECTED")
                                    .font(.custom("Courier-Bold", size: 12))
                                    .foregroundColor(.white)
                                Text("Verify secure WireGuard tunneling is enabled.")
                                    .font(.custom("Courier", size: 10))
                                    .foregroundColor(.white.opacity(0.7))
                            }
                            Spacer()
                            Button("DISMISS") {
                                withAnimation {
                                    tailscaleConnected = true
                                }
                            }
                            .font(.custom("Courier-Bold", size: 10))
                            .foregroundColor(.orange)
                        }
                        .padding()
                        .background(Color.orange.opacity(0.15))
                        .cornerRadius(8)
                        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.orange.opacity(0.3), lineWidth: 1))
                        .padding(.horizontal)
                        .transition(.move(edge: .top).combined(with: .opacity))
                    }
                    
                    if servers.isEmpty {
                        // Empty State Terminal aesthetic
                        VStack(spacing: 20) {
                            Image(systemName: "terminal")
                                .font(.system(size: 60))
                                .foregroundColor(accentColor.opacity(0.5))
                                .padding(.top, 40)
                            
                            Text("NO_SERVERS_DETECTED")
                                .font(.custom("Courier-Bold", size: 16))
                                .foregroundColor(accentColor)
                            
                            Text("Link your first Minecraft RCON server below.")
                                .font(.custom("Courier", size: 12))
                                .foregroundColor(colors.subText)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 40)
                            
                            Button(action: { showingAddSheet = true }) {
                                HStack {
                                    Image(systemName: "plus.square")
                                    Text("ADD SERVER PROFILE")
                                }
                                .font(.custom("Courier-Bold", size: 14))
                                .foregroundColor(isDark ? .black : .white)
                                .padding()
                                .background(accentColor)
                                .cornerRadius(8)
                            }
                        }
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(colors.cardBackground)
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(colors.border, lineWidth: 1))
                        .padding()
                    } else {
                        // Server Listing
                        ScrollView {
                            VStack(spacing: 16) {
                                ForEach(servers) { server in
                                    NavigationLink(value: server) {
                                        ServerRowView(server: server, tailscaleConnected: tailscaleConnected, colors: colors, accentColor: accentColor, isDark: isDark)
                                    }
                                    .swipeActions(edge: .trailing) {
                                        Button(role: .destructive) {
                                            deleteServer(server)
                                        } label: {
                                            Label("Delete", systemImage: "trash")
                                        }
                                    }
                                }
                            }
                            .padding(.horizontal)
                        }
                    }
                    
                    Spacer()
                }
                .padding(.top)
            }
            .navigationTitle("MINE_CONSOLE")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(action: { showingSettings = true }) {
                        Image(systemName: "gearshape")
                            .foregroundColor(accentColor)
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: { showingAddSheet = true }) {
                        Image(systemName: "plus")
                            .foregroundColor(accentColor)
                    }
                }
            }
            .navigationDestination(for: ServerProfile.self) { server in
                ConsoleView(server: server)
            }
            .sheet(isPresented: $showingAddSheet) {
                AddServerSheet(isPresented: $showingAddSheet, onSave: addServer)
            }
            .sheet(isPresented: $showingSettings) {
                SettingsView(tailscaleConnected: $tailscaleConnected)
            }
        }
    }
    
    private func addServer(name: String, ip: String, port: Int, pass: String) {
        let key = "mineconsole.password.\(UUID().uuidString)"
        
        // Save password to Keychain secure enclave
        _ = KeychainHelper.shared.saveString(pass, service: "MineConsole", account: key)
        
        // Save server metadata to SwiftData db
        let newServer = ServerProfile(name: name, ip: ip, rconPort: port, keychainKey: key)
        modelContext.insert(newServer)
        
        try? modelContext.save()
    }
    
    private func deleteServer(_ server: ServerProfile) {
        // Clean password from keychain
        KeychainHelper.shared.delete(service: "MineConsole", account: server.keychainKey)
        
        // Remove from SwiftData db
        modelContext.delete(server)
        try? modelContext.save()
    }
}

// Add Server Sheet Component
struct AddServerSheet: View {
    @Binding var isPresented: Bool
    var onSave: (String, String, Int, String) -> Void
    
    @AppStorage("appTheme") private var appTheme = "System"
    @AppStorage("appAccentColor") private var appAccentColor = "Green"
    @Environment(\.colorScheme) private var systemColorScheme
    
    @State private var name = ""
    @State private var ip = ""
    @State private var port = "25575"
    @State private var password = ""
    @State private var showError = false
    
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
    
    var body: some View {
        NavigationStack {
            ZStack {
                colors.background.ignoresSafeArea()
                
                VStack(spacing: 20) {
                    ScrollView {
                        VStack(spacing: 16) {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("SERVER PROFILE NAME")
                                    .font(.custom("Courier-Bold", size: 12))
                                    .foregroundColor(accentColor)
                                TextField("e.g. My Survival Server", text: $name)
                                    .font(.custom("Courier", size: 14))
                                    .padding()
                                    .background(colors.border)
                                    .cornerRadius(8)
                                    .foregroundColor(colors.text)
                            }
                            
                            VStack(alignment: .leading, spacing: 6) {
                                Text("SERVER IP / ADDRESS")
                                    .font(.custom("Courier-Bold", size: 12))
                                    .foregroundColor(accentColor)
                                TextField("e.g. 100.111.45.98", text: $ip)
                                    .font(.custom("Courier", size: 14))
                                    .padding()
                                    .background(colors.border)
                                    .cornerRadius(8)
                                    .foregroundColor(colors.text)
                                    .keyboardType(.numbersAndPunctuation)
                            }
                            
                            VStack(alignment: .leading, spacing: 6) {
                                Text("RCON TCP PORT")
                                    .font(.custom("Courier-Bold", size: 12))
                                    .foregroundColor(accentColor)
                                TextField("25575", text: $port)
                                    .font(.custom("Courier", size: 14))
                                    .padding()
                                    .background(colors.border)
                                    .cornerRadius(8)
                                    .foregroundColor(colors.text)
                                    .keyboardType(.numberPad)
                            }
                            
                            VStack(alignment: .leading, spacing: 6) {
                                Text("RCON PASSWORD")
                                    .font(.custom("Courier-Bold", size: 12))
                                    .foregroundColor(accentColor)
                                SecureField("••••••••", text: $password)
                                    .font(.custom("Courier", size: 14))
                                    .padding()
                                    .background(colors.border)
                                    .cornerRadius(8)
                                    .foregroundColor(colors.text)
                            }
                        }
                        .padding()
                    }
                    
                    if showError {
                        Text("Fill in all parameters correctly.")
                            .font(.custom("Courier", size: 12))
                            .foregroundColor(.red)
                    }
                    
                    Button(action: saveAction) {
                        Text("SAVE CONNECTION PROFILE")
                            .font(.custom("Courier-Bold", size: 14))
                            .foregroundColor(isDark ? .black : .white)
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(accentColor)
                            .cornerRadius(8)
                    }
                    .padding()
                }
            }
            .navigationTitle("NEW_CONNECTION")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("CANCEL") { isPresented = false }
                        .font(.custom("Courier-Bold", size: 12))
                        .foregroundColor(accentColor)
                }
            }
        }
    }
    
    private func saveAction() {
        guard !name.isEmpty, !ip.isEmpty, !password.isEmpty, let portInt = Int(port) else {
            showError = true
            return
        }
        onSave(name, ip, portInt, password)
        isPresented = false
    }
}

// Sub-component for individual server row to prevent Swift compiler timeout errors
struct ServerRowView: View {
    let server: ServerProfile
    let tailscaleConnected: Bool
    let colors: ThemeColors
    let accentColor: Color
    let isDark: Bool
    
    var body: some View {
        HStack(spacing: 16) {
            // Glowing Status Orb
            Circle()
                .fill(tailscaleConnected ? accentColor : Color.red)
                .frame(width: 12, height: 12)
                .shadow(color: (tailscaleConnected ? accentColor : Color.red).opacity(0.8), radius: 6)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(server.name.uppercased())
                    .font(.custom("Courier-Bold", size: 16))
                    .foregroundColor(colors.text)
                
                Text("\(server.ip):\(String(server.rconPort))")
                    .font(.custom("Courier", size: 12))
                    .foregroundColor(colors.subText)
            }
            
            Spacer()
            
            // Role Badge
            Text(server.sharedRole.uppercased())
                .font(.custom("Courier", size: 10))
                .foregroundColor(accentColor)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(accentColor.opacity(0.1))
                .cornerRadius(4)
                .overlay(RoundedRectangle(cornerRadius: 4).stroke(accentColor.opacity(0.3), lineWidth: 1))
            
            Image(systemName: "chevron.right")
                .foregroundColor(colors.subText)
        }
        .padding()
        .background(colors.cardBackground)
        .cornerRadius(12)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(colors.border, lineWidth: 1))
    }
}

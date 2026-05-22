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
    
    var body: some View {
        NavigationStack {
            ZStack {
                // Sleek Deep Gray/Black Gradient Background
                LinearGradient(colors: [Color(white: 0.05), Color(white: 0.12)], startPoint: .top, endPoint: .bottom)
                    .ignoresSafeArea()
                
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
                                .foregroundColor(.green.opacity(0.5))
                                .padding(.top, 40)
                            
                            Text("NO_SERVERS_DETECTED")
                                .font(.custom("Courier-Bold", size: 16))
                                .foregroundColor(.green)
                            
                            Text("Link your first Minecraft RCON server below.")
                                .font(.custom("Courier", size: 12))
                                .foregroundColor(.white.opacity(0.6))
                                .multilineTextAlignment(.center)
                                .padding(.horizontal, 40)
                            
                            Button(action: { showingAddSheet = true }) {
                                HStack {
                                    Image(systemName: "plus.square")
                                    Text("ADD SERVER PROFILE")
                                }
                                .font(.custom("Courier-Bold", size: 14))
                                .foregroundColor(.black)
                                .padding()
                                .background(Color.green)
                                .cornerRadius(8)
                            }
                        }
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color.white.opacity(0.02))
                        .cornerRadius(12)
                        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.05), lineWidth: 1))
                        .padding()
                    } else {
                        // Server Listing
                        ScrollView {
                            VStack(spacing: 16) {
                                ForEach(servers) { server in
                                    NavigationLink(value: server) {
                                        ServerRowView(server: server, tailscaleConnected: tailscaleConnected)
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
                            .foregroundColor(.green)
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button(action: { showingAddSheet = true }) {
                        Image(systemName: "plus")
                            .foregroundColor(.green)
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
    
    @State private var name = ""
    @State private var ip = ""
    @State private var port = "25575"
    @State private var password = ""
    @State private var showError = false
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()
                
                VStack(spacing: 20) {
                    ScrollView {
                        VStack(spacing: 16) {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("SERVER PROFILE NAME")
                                    .font(.custom("Courier-Bold", size: 12))
                                    .foregroundColor(.green)
                                TextField("e.g. My Survival Server", text: $name)
                                    .font(.custom("Courier", size: 14))
                                    .padding()
                                    .background(Color.white.opacity(0.05))
                                    .cornerRadius(8)
                                    .foregroundColor(.white)
                            }
                            
                            VStack(alignment: .leading, spacing: 6) {
                                Text("SERVER IP / ADDRESS")
                                    .font(.custom("Courier-Bold", size: 12))
                                    .foregroundColor(.green)
                                TextField("e.g. 100.111.45.98", text: $ip)
                                    .font(.custom("Courier", size: 14))
                                    .padding()
                                    .background(Color.white.opacity(0.05))
                                    .cornerRadius(8)
                                    .foregroundColor(.white)
                                    .keyboardType(.numbersAndPunctuation)
                            }
                            
                            VStack(alignment: .leading, spacing: 6) {
                                Text("RCON TCP PORT")
                                    .font(.custom("Courier-Bold", size: 12))
                                    .foregroundColor(.green)
                                TextField("25575", text: $port)
                                    .font(.custom("Courier", size: 14))
                                    .padding()
                                    .background(Color.white.opacity(0.05))
                                    .cornerRadius(8)
                                    .foregroundColor(.white)
                                    .keyboardType(.numberPad)
                            }
                            
                            VStack(alignment: .leading, spacing: 6) {
                                Text("RCON PASSWORD")
                                    .font(.custom("Courier-Bold", size: 12))
                                    .foregroundColor(.green)
                                SecureField("••••••••", text: $password)
                                    .font(.custom("Courier", size: 14))
                                    .padding()
                                    .background(Color.white.opacity(0.05))
                                    .cornerRadius(8)
                                    .foregroundColor(.white)
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
                            .foregroundColor(.black)
                            .padding()
                            .frame(maxWidth: .infinity)
                            .background(Color.green)
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
                        .foregroundColor(.green)
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
    
    var body: some View {
        HStack(spacing: 16) {
            // Glowing Status Orb
            Circle()
                .fill(tailscaleConnected ? Color.green : Color.red)
                .frame(width: 12, height: 12)
                .shadow(color: (tailscaleConnected ? Color.green : Color.red).opacity(0.8), radius: 6)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(server.name.uppercased())
                    .font(.custom("Courier-Bold", size: 16))
                    .foregroundColor(.white)
                
                Text("\(server.ip):\(String(server.rconPort))")
                    .font(.custom("Courier", size: 12))
                    .foregroundColor(.white.opacity(0.5))
            }
            
            Spacer()
            
            // Role Badge
            Text(server.sharedRole.uppercased())
                .font(.custom("Courier", size: 10))
                .foregroundColor(.green)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color.green.opacity(0.1))
                .cornerRadius(4)
                .overlay(RoundedRectangle(cornerRadius: 4).stroke(Color.green.opacity(0.3), lineWidth: 1))
            
            Image(systemName: "chevron.right")
                .foregroundColor(.white.opacity(0.3))
        }
        .padding()
        .background(Color.white.opacity(0.03))
        .cornerRadius(12)
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.white.opacity(0.05), lineWidth: 1))
    }
}


import SwiftUI

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    
    // Security states
    @AppStorage("biometricsEnabled") private var biometricsEnabled = false
    @Binding var tailscaleConnected: Bool
    
    // Sharing Generator fields
    @State private var selectedRole = "Viewer"
    @State private var generatedToken = ""
    @State private var isCopied = false
    
    let roles = ["Viewer", "Moderator", "Admin"]
    
    var body: some View {
        NavigationStack {
            ZStack {
                Color.black.ignoresSafeArea()
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        
                        // Security Section
                        VStack(alignment: .leading, spacing: 12) {
                            Text("SECURITY_ENVELOPE")
                                .font(.custom("Courier-Bold", size: 12))
                                .foregroundColor(.green)
                                .padding(.horizontal)
                            
                            VStack(spacing: 0) {
                                Toggle(isOn: $biometricsEnabled) {
                                    HStack {
                                        Image(systemName: "faceid")
                                            .foregroundColor(.green)
                                            .frame(width: 24)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text("BIOMETRIC LOCK")
                                                .font(.custom("Courier-Bold", size: 14))
                                                .foregroundColor(.white)
                                            Text("Require FaceID on startup")
                                                .font(.custom("Courier", size: 10))
                                                .foregroundColor(.white.opacity(0.5))
                                        }
                                    }
                                }
                                .toggleStyle(SwitchToggleStyle(tint: .green))
                                .padding()
                                
                                Divider().background(Color.white.opacity(0.1))
                                
                                Toggle(isOn: $tailscaleConnected) {
                                    HStack {
                                        Image(systemName: "network")
                                            .foregroundColor(.green)
                                            .frame(width: 24)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text("TAILSCALE VPNTUNNEL")
                                                .font(.custom("Courier-Bold", size: 14))
                                                .foregroundColor(.white)
                                            Text("Toggle safe Tailscale mock routing")
                                                .font(.custom("Courier", size: 10))
                                                .foregroundColor(.white.opacity(0.5))
                                        }
                                    }
                                }
                                .toggleStyle(SwitchToggleStyle(tint: .green))
                                .padding()
                            }
                            .background(Color.white.opacity(0.03))
                            .cornerRadius(10)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.05), lineWidth: 1))
                            .padding(.horizontal)
                        }
                        
                        // Friend Sharing Sheet (Guest Links)
                        VStack(alignment: .leading, spacing: 12) {
                            Text("GUEST_ACCESS_GENERATOR")
                                .font(.custom("Courier-Bold", size: 12))
                                .foregroundColor(.green)
                                .padding(.horizontal)
                            
                            VStack(alignment: .leading, spacing: 16) {
                                Text("Produce specialized temporary profiles to let colleagues connect without disclosing your main RCON security credentials.")
                                    .font(.custom("Courier", size: 11))
                                    .foregroundColor(.white.opacity(0.6))
                                    .padding(.bottom, 4)
                                
                                // Picker
                                VStack(alignment: .leading, spacing: 6) {
                                    Text("ASSIGN PRIVILEGE ROLE")
                                        .font(.custom("Courier-Bold", size: 10))
                                        .foregroundColor(.green.opacity(0.8))
                                    
                                    Picker("Role", selection: $selectedRole) {
                                        ForEach(roles, id: \.self) { role in
                                            Text(role.uppercased()).tag(role)
                                        }
                                    }
                                    .pickerStyle(SegmentedPickerStyle())
                                    .background(Color.white.opacity(0.05))
                                    .cornerRadius(6)
                                }
                                
                                Button(action: generateLink) {
                                    Text("GENERATE GUEST TOKEN")
                                        .font(.custom("Courier-Bold", size: 12))
                                        .foregroundColor(.black)
                                        .padding(.vertical, 12)
                                        .frame(maxWidth: .infinity)
                                        .background(Color.green)
                                        .cornerRadius(6)
                                }
                                
                                if !generatedToken.isEmpty {
                                    VStack(alignment: .leading, spacing: 8) {
                                        Text("SECURE ACCESS LINK")
                                            .font(.custom("Courier-Bold", size: 10))
                                            .foregroundColor(.green.opacity(0.8))
                                        
                                        HStack {
                                            Text(generatedToken)
                                                .font(.custom("Courier", size: 11))
                                                .foregroundColor(.white)
                                                .lineLimit(1)
                                                .truncationMode(.middle)
                                            
                                            Spacer()
                                            
                                            Button(action: copyToClipboard) {
                                                Image(systemName: isCopied ? "checkmark" : "doc.on.doc")
                                                    .foregroundColor(isCopied ? .green : .white.opacity(0.6))
                                            }
                                        }
                                        .padding()
                                        .background(Color.white.opacity(0.04))
                                        .cornerRadius(6)
                                        .overlay(RoundedRectangle(cornerRadius: 6).stroke(Color.white.opacity(0.1), lineWidth: 1))
                                    }
                                    .transition(.opacity.combined(with: .move(edge: .bottom)))
                                }
                            }
                            .padding()
                            .background(Color.white.opacity(0.03))
                            .cornerRadius(10)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(Color.white.opacity(0.05), lineWidth: 1))
                            .padding(.horizontal)
                        }
                    }
                    .padding(.vertical)
                }
            }
            .navigationTitle("SETTINGS_BOARD")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("CLOSE") { dismiss() }
                        .font(.custom("Courier-Bold", size: 12))
                        .foregroundColor(.green)
                }
            }
        }
    }
    
    private func generateLink() {
        withAnimation {
            let mockKey = UUID().uuidString.prefix(8).lowercased()
            generatedToken = "mineconsole://guest?key=\(mockKey)&role=\(selectedRole.lowercased())"
            isCopied = false
        }
    }
    
    private func copyToClipboard() {
        UIPasteboard.general.string = generatedToken
        withAnimation {
            isCopied = true
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.0) {
            withAnimation {
                isCopied = false
            }
        }
    }
}

#Preview {
    SettingsView(tailscaleConnected: .constant(true))
}

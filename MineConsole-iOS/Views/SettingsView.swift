import SwiftUI

struct SettingsView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var systemColorScheme
    
    // Theme preferences
    @AppStorage("appTheme") private var appTheme = "System"
    @AppStorage("appAccentColor") private var appAccentColor = "Green"
    
    // Security states
    @AppStorage("biometricsEnabled") private var biometricsEnabled = false
    @Binding var tailscaleConnected: Bool
    
    // Sharing Generator fields
    @State private var selectedRole = "Viewer"
    @State private var generatedToken = ""
    @State private var isCopied = false
    
    let roles = ["Viewer", "Moderator", "Admin"]
    
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
                
                ScrollView {
                    VStack(alignment: .leading, spacing: 24) {
                        
                        // Security Section
                        VStack(alignment: .leading, spacing: 12) {
                            Text("SECURITY_ENVELOPE")
                                .font(.custom("Courier-Bold", size: 12))
                                .foregroundColor(accentColor)
                                .padding(.horizontal)
                            
                            VStack(spacing: 0) {
                                Toggle(isOn: $biometricsEnabled) {
                                    HStack {
                                        Image(systemName: "faceid")
                                            .foregroundColor(accentColor)
                                            .frame(width: 24)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text("BIOMETRIC LOCK")
                                                .font(.custom("Courier-Bold", size: 14))
                                                .foregroundColor(colors.text)
                                            Text("Require FaceID on startup")
                                                .font(.custom("Courier", size: 10))
                                                .foregroundColor(colors.subText)
                                        }
                                    }
                                }
                                .toggleStyle(SwitchToggleStyle(tint: accentColor))
                                .padding()
                                
                                Divider().background(colors.border)
                                
                                Toggle(isOn: $tailscaleConnected) {
                                    HStack {
                                        Image(systemName: "network")
                                            .foregroundColor(accentColor)
                                            .frame(width: 24)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text("TAILSCALE VPNTUNNEL")
                                                .font(.custom("Courier-Bold", size: 14))
                                                .foregroundColor(colors.text)
                                            Text("Toggle safe Tailscale mock routing")
                                                .font(.custom("Courier", size: 10))
                                                .foregroundColor(colors.subText)
                                        }
                                    }
                                }
                                .toggleStyle(SwitchToggleStyle(tint: accentColor))
                                .padding()
                            }
                            .background(colors.cardBackground)
                            .cornerRadius(10)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(colors.border, lineWidth: 1))
                            .padding(.horizontal)
                        }
                        
                        // Theme Personalization Section
                        VStack(alignment: .leading, spacing: 12) {
                            Text("THEME_PERSONALIZATION")
                                .font(.custom("Courier-Bold", size: 12))
                                .foregroundColor(accentColor)
                                .padding(.horizontal)
                            
                            VStack(alignment: .leading, spacing: 16) {
                                Text("APPEARANCE MODE")
                                    .font(.custom("Courier-Bold", size: 10))
                                    .foregroundColor(colors.subText)
                                
                                Picker("Appearance Mode", selection: $appTheme) {
                                    ForEach(AppTheme.allCases) { theme in
                                        Text(theme.rawValue.uppercased()).tag(theme.rawValue)
                                    }
                                }
                                .pickerStyle(SegmentedPickerStyle())
                                .background(colors.border)
                                .cornerRadius(6)
                                
                                Text("ACCENT COLOR")
                                    .font(.custom("Courier-Bold", size: 10))
                                    .foregroundColor(colors.subText)
                                
                                HStack(spacing: 8) {
                                    ForEach(AppAccentColor.allCases) { accent in
                                        Button(action: {
                                            withAnimation {
                                                appAccentColor = accent.rawValue
                                            }
                                        }) {
                                            ZStack {
                                                RoundedRectangle(cornerRadius: 6)
                                                    .fill(accent.color)
                                                    .frame(height: 32)
                                                
                                                if appAccentColor == accent.rawValue {
                                                    RoundedRectangle(cornerRadius: 6)
                                                        .stroke(colors.text, lineWidth: 3)
                                                        .frame(height: 32)
                                                } else {
                                                    RoundedRectangle(cornerRadius: 6)
                                                        .stroke(Color.clear, lineWidth: 1)
                                                        .frame(height: 32)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            .padding()
                            .background(colors.cardBackground)
                            .cornerRadius(10)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(colors.border, lineWidth: 1))
                            .padding(.horizontal)
                        }
                        
                        // Friend Sharing Sheet (Guest Links)
                        VStack(alignment: .leading, spacing: 12) {
                            Text("GUEST_ACCESS_GENERATOR")
                                .font(.custom("Courier-Bold", size: 12))
                                .foregroundColor(accentColor)
                                .padding(.horizontal)
                            
                            VStack(alignment: .leading, spacing: 16) {
                                Text("Produce specialized temporary profiles to let colleagues connect without disclosing your main RCON security credentials.")
                                    .font(.custom("Courier", size: 11))
                                    .foregroundColor(colors.subText)
                                    .padding(.bottom, 4)
                                
                                // Picker
                                VStack(alignment: .leading, spacing: 6) {
                                    Text("ASSIGN PRIVILEGE ROLE")
                                        .font(.custom("Courier-Bold", size: 10))
                                        .foregroundColor(accentColor.opacity(0.8))
                                    
                                    Picker("Role", selection: $selectedRole) {
                                        ForEach(roles, id: \.self) { role in
                                            Text(role.uppercased()).tag(role)
                                        }
                                    }
                                    .pickerStyle(SegmentedPickerStyle())
                                    .background(colors.border)
                                    .cornerRadius(6)
                                }
                                
                                Button(action: generateLink) {
                                    Text("GENERATE GUEST TOKEN")
                                        .font(.custom("Courier-Bold", size: 12))
                                        .foregroundColor(isDark ? .black : .white)
                                        .padding(.vertical, 12)
                                        .frame(maxWidth: .infinity)
                                        .background(accentColor)
                                        .cornerRadius(6)
                                }
                                
                                if !generatedToken.isEmpty {
                                    VStack(alignment: .leading, spacing: 8) {
                                        Text("SECURE ACCESS LINK")
                                            .font(.custom("Courier-Bold", size: 10))
                                            .foregroundColor(accentColor.opacity(0.8))
                                        
                                        HStack {
                                            Text(generatedToken)
                                                .font(.custom("Courier", size: 11))
                                                .foregroundColor(colors.text)
                                                .lineLimit(1)
                                                .truncationMode(.middle)
                                            
                                            Spacer()
                                            
                                            Button(action: copyToClipboard) {
                                                Image(systemName: isCopied ? "checkmark" : "doc.on.doc")
                                                    .foregroundColor(isCopied ? accentColor : colors.subText)
                                            }
                                        }
                                        .padding()
                                        .background(colors.border)
                                        .cornerRadius(6)
                                        .overlay(RoundedRectangle(cornerRadius: 6).stroke(colors.border, lineWidth: 1))
                                    }
                                    .transition(.opacity.combined(with: .move(edge: .bottom)))
                                }
                            }
                            .padding()
                            .background(colors.cardBackground)
                            .cornerRadius(10)
                            .overlay(RoundedRectangle(cornerRadius: 10).stroke(colors.border, lineWidth: 1))
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
                        .foregroundColor(accentColor)
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

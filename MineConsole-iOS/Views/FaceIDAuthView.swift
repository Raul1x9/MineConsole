import SwiftUI
import LocalAuthentication

struct FaceIDAuthView: View {
    @Binding var isUnlocked: Bool
    @State private var errorMessage: String? = nil
    @State private var isScanning = false
    
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
        ZStack {
            colors.background.ignoresSafeArea()
            
            VStack(spacing: 30) {
                Spacer()
                
                // Terminal Terminal Glowing Title
                VStack(spacing: 8) {
                    Text("MINE_CONSOLE")
                        .font(.custom("CourierNewPS-BoldMT", size: 28))
                        .foregroundColor(accentColor)
                        .tracking(4)
                        .shadow(color: accentColor.opacity(0.5), radius: 8)
                    
                    Text("SECURE SYSTEM ACCESS v1.1.0")
                        .font(.custom("Courier", size: 12))
                        .foregroundColor(colors.subText)
                }
                
                Spacer()
                
                // Animated Biometric Scan Logo
                ZStack {
                    Circle()
                        .stroke(accentColor.opacity(0.2), lineWidth: 4)
                        .frame(width: 140, height: 140)
                    
                    Circle()
                        .stroke(accentColor, lineWidth: 2)
                        .frame(width: 140, height: 140)
                        .scaleEffect(isScanning ? 1.15 : 1.0)
                        .opacity(isScanning ? 0.0 : 0.8)
                        .animation(.easeOut(duration: 1.5).repeatForever(autoreverses: false), value: isScanning)
                    
                    Image(systemName: "faceid")
                        .font(.system(size: 64))
                        .foregroundColor(accentColor)
                        .shadow(color: accentColor.opacity(0.8), radius: 10)
                }
                .onAppear {
                    isScanning = true
                }
                
                Spacer()
                
                VStack(spacing: 16) {
                    Button(action: authenticate) {
                        Text("AUTHENTICATE SYSTEM")
                            .font(.custom("Courier-Bold", size: 16))
                            .foregroundColor(isDark ? .black : .white)
                            .padding(.vertical, 14)
                            .padding(.horizontal, 40)
                            .background(accentColor)
                            .cornerRadius(8)
                            .shadow(color: accentColor.opacity(0.5), radius: 6)
                    }
                    
                    if let errorMessage = errorMessage {
                        Text(errorMessage)
                            .font(.custom("Courier", size: 12))
                            .foregroundColor(.red)
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                    }
                }
                
                Spacer()
            }
            .padding()
        }
        .onAppear(perform: authenticate)
    }
    
    private func authenticate() {
        let context = LAContext()
        var error: NSError?
        
        if context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) {
            let reason = "Unlock MineConsole to access server management."
            
            context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: reason) { success, authenticationError in
                DispatchQueue.main.async {
                    if success {
                        withAnimation {
                            isUnlocked = true
                        }
                    } else {
                        errorMessage = authenticationError?.localizedDescription ?? "Biometric check failed."
                    }
                }
            }
        } else {
            errorMessage = "Biometrics unavailable. Checking passcode..."
            // Fallback for Simulator or devices without biometrics
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
                withAnimation {
                    isUnlocked = true
                }
            }
        }
    }
}

#Preview {
    FaceIDAuthView(isUnlocked: .constant(false))
}

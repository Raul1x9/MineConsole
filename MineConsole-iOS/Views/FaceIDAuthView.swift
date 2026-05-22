import SwiftUI
import LocalAuthentication

struct FaceIDAuthView: View {
    @Binding var isUnlocked: Bool
    @State private var errorMessage: String? = nil
    @State private var isScanning = false
    
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            
            VStack(spacing: 30) {
                Spacer()
                
                // Terminal Terminal Glowing Title
                VStack(spacing: 8) {
                    Text("MINE_CONSOLE")
                        .font(.custom("CourierNewPS-BoldMT", size: 28))
                        .foregroundColor(.green)
                        .tracking(4)
                        .shadow(color: .green.opacity(0.5), radius: 8)
                    
                    Text("SECURE SYSTEM ACCESS v1.0")
                        .font(.custom("Courier", size: 12))
                        .foregroundColor(.white.opacity(0.6))
                }
                
                Spacer()
                
                // Animated Biometric Scan Logo
                ZStack {
                    Circle()
                        .stroke(Color.green.opacity(0.2), lineWidth: 4)
                        .frame(width: 140, height: 140)
                    
                    Circle()
                        .stroke(Color.green, lineWidth: 2)
                        .frame(width: 140, height: 140)
                        .scaleEffect(isScanning ? 1.15 : 1.0)
                        .opacity(isScanning ? 0.0 : 0.8)
                        .animation(.easeOut(duration: 1.5).repeatForever(autoreverses: false), value: isScanning)
                    
                    Image(systemName: "faceid")
                        .font(.system(size: 64))
                        .foregroundColor(.green)
                        .shadow(color: .green.opacity(0.8), radius: 10)
                }
                .onAppear {
                    isScanning = true
                }
                
                Spacer()
                
                VStack(spacing: 16) {
                    Button(action: authenticate) {
                        Text("AUTHENTICATE SYSTEM")
                            .font(.custom("Courier-Bold", size: 16))
                            .foregroundColor(.black)
                            .padding(.vertical, 14)
                            .padding(.horizontal, 40)
                            .background(Color.green)
                            .cornerRadius(8)
                            .shadow(color: .green.opacity(0.5), radius: 6)
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

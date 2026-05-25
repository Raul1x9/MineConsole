import SwiftUI
import SwiftData

@main
struct MineConsoleApp: App {
    @AppStorage("biometricsEnabled") private var biometricsEnabled = false
    @State private var isUnlocked = false
    
    // Configure SwiftData Database Container
    var sharedModelContainer: ModelContainer = {
        let schema = Schema([
            ServerProfile.self,
        ])
        let modelConfiguration = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)

        do {
            return try ModelContainer(for: schema, configurations: [modelConfiguration])
        } catch {
            fatalError("Could not create ModelContainer: \(error.localizedDescription)")
        }
    }()

    public init() {
        LocalNetworkPermissionHelper.triggerPrompt()
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if biometricsEnabled && !isUnlocked {
                    // FaceID Lock overlay
                    FaceIDAuthView(isUnlocked: $isUnlocked)
                } else {
                    // Normal app Dashboard
                    DashboardView()
                }
            }
            .preferredColorScheme(.dark)
        }
        .modelContainer(sharedModelContainer)
    }
}

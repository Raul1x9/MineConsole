import SwiftUI

enum AppTheme: String, CaseIterable, Identifiable {
    case system = "System"
    case light = "Light"
    case dark = "Dark"
    
    var id: String { self.rawValue }
    
    var colorScheme: ColorScheme? {
        switch self {
        case .light: return .light
        case .dark: return .dark
        case .system: return nil
        }
    }
}

enum AppAccentColor: String, CaseIterable, Identifiable {
    case green = "Green"
    case blue = "Blue"
    case red = "Red"
    case purple = "Purple"
    case orange = "Orange"
    
    var id: String { self.rawValue }
    
    var color: Color {
        switch self {
        case .blue: return Color(red: 0.0, green: 0.6, blue: 1.0)      // #0099FF
        case .red: return Color(red: 1.0, green: 0.2, blue: 0.2)       // #FF3333
        case .purple: return Color(red: 0.68, green: 0.35, blue: 1.0)  // #AE59FF
        case .orange: return Color(red: 1.0, green: 0.6, blue: 0.0)    // #FF9900
        case .green: return Color(red: 0.0, green: 1.0, blue: 0.4)     // #00FF66 (Green Default)
        }
    }
}

struct ThemeColors {
    let background: Color
    let cardBackground: Color
    let text: Color
    let subText: Color
    let border: Color
}

class ThemeManager {
    static func getAccentColor(name: String) -> Color {
        return AppAccentColor(rawValue: name)?.color ?? AppAccentColor.green.color
    }
    
    static func getThemeColors(themeName: String, isSystemDark: Bool) -> ThemeColors {
        let isDark: Bool
        switch themeName {
        case "Light":
            isDark = false
        case "Dark":
            isDark = true
        default:
            isDark = isSystemDark
        }
        
        if isDark {
            return ThemeColors(
                background: Color(red: 12/255, green: 12/255, blue: 12/255), // #0C0C0C
                cardBackground: Color(red: 30/255, green: 30/255, blue: 30/255), // #1E1E1E
                text: .white,
                subText: .white.opacity(0.5),
                border: .white.opacity(0.05)
            )
        } else {
            return ThemeColors(
                background: Color(red: 245/255, green: 245/255, blue: 245/255), // #F5F5F5
                cardBackground: .white,
                text: Color(red: 26/255, green: 26/255, blue: 26/255), // #1A1A1A
                subText: Color(red: 26/255, green: 26/255, blue: 26/255).opacity(0.6),
                border: Color(red: 26/255, green: 26/255, blue: 26/255).opacity(0.1)
            )
        }
    }
}

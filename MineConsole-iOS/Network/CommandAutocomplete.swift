import Foundation

public final class CommandAutocomplete {
    private static let rootCommands = [
        "/ban", "/ban-ip", "/banlist", "/clear", "/deop", "/difficulty",
        "/effect", "/gamemode", "/gamerule", "/give", "/help", "/kick",
        "/kill", "/list", "/op", "/pardon", "/pardon-ip", "/say", "/stop",
        "/summon", "/time", "/tp", "/weather", "/whitelist"
    ]
    
    private static let effects = [
        "speed", "slowness", "haste", "mining_fatigue", "strength",
        "instant_health", "instant_damage", "jump_boost", "regeneration",
        "resistance", "fire_resistance", "water_breathing", "invisibility",
        "blindness", "night_vision", "hunger", "weakness", "poison",
        "wither", "health_boost", "absorption", "saturation"
    ]
    
    private static let items = [
        "diamond", "emerald", "gold_ingot", "iron_ingot", "coal",
        "wood", "stone", "dirt", "grass_block", "cobblestone"
    ]
    
    private static let entities = [
        "zombie", "skeleton", "spider", "creeper", "enderman",
        "witch", "slime", "iron_golem", "villager", "cow", "pig",
        "sheep", "chicken"
    ]
    
    private static let gamerules = [
        "commandBlockOutput", "doDaylightCycle", "doEntityDrops", "doFireTick",
        "doMobLoot", "doMobSpawning", "doTileDrops", "keepInventory",
        "logAdminCommands", "mobGriefing", "naturalRegeneration",
        "randomTickSpeed", "sendCommandFeedback", "showDeathMessages"
    ]
    
    private static let selectors = ["@p", "@a", "@r", "@s", "@e"]
    
    public static func getSuggestions(for input: String) -> [String] {
        // If empty, suggest all root commands
        if input.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return rootCommands
        }
        
        let parts = input.components(separatedBy: " ")
        guard !parts.isEmpty else { return rootCommands }
        
        let count = parts.count
        let currentTyped = parts.last ?? ""
        
        // 1. Root command suggestions
        if count == 1 {
            let filterTerm = currentTyped.lowercased()
            // If they didn't start with a slash, check both with and without slash
            return rootCommands.filter { cmd in
                cmd.lowercased().hasPrefix(filterTerm) || 
                cmd.replacingOccurrences(of: "/", with: "").lowercased().hasPrefix(filterTerm)
            }
        }
        
        let root = parts[0].lowercased()
        
        // 2. Second argument suggestions
        if count == 2 {
            var rawSuggestions: [String] = []
            switch root {
            case "/gamemode", "gamemode":
                rawSuggestions = ["survival", "creative", "adventure", "spectator"]
            case "/difficulty", "difficulty":
                rawSuggestions = ["peaceful", "easy", "normal", "hard"]
            case "/time", "time":
                rawSuggestions = ["set", "add", "query"]
            case "/weather", "weather":
                rawSuggestions = ["clear", "rain", "thunder"]
            case "/whitelist", "whitelist":
                rawSuggestions = ["add", "remove", "list", "on", "off"]
            case "/effect", "effect":
                rawSuggestions = ["give", "clear"]
            case "/gamerule", "gamerule":
                rawSuggestions = gamerules
            case "/banlist", "banlist":
                rawSuggestions = ["ips", "players"]
            case "/give", "give", "/clear", "clear", "/kick", "kick", "/kill", "kill", "/op", "op", "/deop", "deop", "/ban", "ban", "/pardon", "pardon", "/tp", "tp":
                rawSuggestions = selectors
            case "/list", "list":
                rawSuggestions = ["uuids"]
            default:
                break
            }
            return rawSuggestions.filter { $0.lowercased().hasPrefix(currentTyped.lowercased()) }
        }
        
        let secondArg = parts[1].lowercased()
        
        // 3. Third argument suggestions
        if count == 3 {
            var rawSuggestions: [String] = []
            if root == "/time" || root == "time" {
                if secondArg == "set" {
                    rawSuggestions = ["day", "night", "noon", "midnight"]
                }
            } else if root == "/gamerule" || root == "gamerule" {
                if gamerules.contains(parts[1]) {
                    if parts[1] == "randomTickSpeed" {
                        rawSuggestions = ["3", "10", "100"]
                    } else {
                        rawSuggestions = ["true", "false"]
                    }
                }
            } else if root == "/whitelist" || root == "whitelist" {
                if secondArg == "add" || secondArg == "remove" {
                    rawSuggestions = selectors
                }
            } else if root == "/effect" || root == "effect" {
                rawSuggestions = selectors
            } else if root == "/give" || root == "give" {
                rawSuggestions = items
            } else if root == "/clear" || root == "clear" {
                rawSuggestions = items
            } else if root == "/tp" || root == "tp" {
                rawSuggestions = selectors + ["~ ~ ~"]
            } else if root == "/weather" || root == "weather" {
                rawSuggestions = ["300", "600", "12000"]
            }
            return rawSuggestions.filter { $0.lowercased().hasPrefix(currentTyped.lowercased()) }
        }
        
        // 4. Fourth argument suggestions
        if count == 4 {
            var rawSuggestions: [String] = []
            if (root == "/effect" || root == "effect") && secondArg == "give" {
                rawSuggestions = effects
            } else if root == "/give" || root == "give" {
                rawSuggestions = ["1", "64"]
            }
            return rawSuggestions.filter { $0.lowercased().hasPrefix(currentTyped.lowercased()) }
        }
        
        // 5. Fifth argument suggestions
        if count == 5 {
            var rawSuggestions: [String] = []
            if (root == "/effect" || root == "effect") && secondArg == "give" {
                rawSuggestions = ["10", "30", "60", "9999", "infinite"]
            }
            return rawSuggestions.filter { $0.lowercased().hasPrefix(currentTyped.lowercased()) }
        }
        
        return []
    }
}

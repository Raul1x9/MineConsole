import Foundation

public final class CommandAutocomplete {
    private static let rootCommands = [
        "/advancement", "/attribute", "/ban", "/ban-ip", "/banlist", "/bossbar",
        "/clear", "/clone", "/damage", "/data", "/datapack", "/debug", "/defaultgamemode",
        "/deop", "/difficulty", "/effect", "/enchant", "/execute", "/experience", "/fill",
        "/fillbiome", "/forceload", "/function", "/gamemode", "/gamerule", "/give", "/help",
        "/item", "/jfr", "/kick", "/kill", "/list", "/locate", "/loot", "/me", "/msg",
        "/op", "/pardon", "/pardon-ip", "/particle", "/perf", "/place", "/playsound",
        "/publish", "/recipe", "/reload", "/ride", "/say", "/schedule", "/scoreboard",
        "/seed", "/setblock", "/setidletimeout", "/setworldspawn", "/spawnpoint", "/spectate",
        "/spreadplayers", "/stop", "/stopsound", "/summon", "/tag", "/team", "/teammsg",
        "/teleport", "/tell", "/tellraw", "/time", "/title", "/tp", "/trigger",
        "/weather", "/whitelist", "/worldborder", "/xp"
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
    
    private static let enchantments = [
        "sharpness", "unbreaking", "efficiency", "protection", "mending",
        "fortune", "silk_touch", "power", "punch", "infinity", "smite",
        "bane_of_arthropods", "knockback", "fire_aspect", "looting",
        "respiration", "aqua_affinity", "thorns", "depth_strider",
        "frost_walker", "feather_falling"
    ]
    
    private static let gamerules = [
        "commandBlockOutput", "doDaylightCycle", "doEntityDrops", "doFireTick",
        "doMobLoot", "doMobSpawning", "doTileDrops", "keepInventory",
        "logAdminCommands", "mobGriefing", "naturalRegeneration",
        "randomTickSpeed", "sendCommandFeedback", "showDeathMessages",
        "doWeatherCycle", "maxCommandChainLength", "spawnRadius",
        "playersSleepingPercentage"
    ]
    
    private static let selectors = ["@p", "@a", "@r", "@s", "@e"]
    
    public static func getSuggestions(for input: String) -> [String] {
        if input.trimingCharacters(in: .whitespacesAndNewlines).isEmpty {
            return rootCommands
        }
        
        let parts = input.components(separatedBy: " ")
        guard !parts.isEmpty else { return rootCommands }
        
        let count = parts.count
        let currentTyped = parts.last ?? ""
        
        // 1. Root command suggestions
        if count == 1 {
            let filterTerm = currentTyped.lowercased()
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
            case "/give", "give", "/clear", "clear", "/kick", "kick", "/kill", "kill", "/op", "op", "/deop", "deop", "/ban", "ban", "/pardon", "pardon", "/tp", "tp", "/teleport", "teleport":
                rawSuggestions = selectors
            case "/list", "list":
                rawSuggestions = ["uuids"]
            case "/advancement", "advancement":
                rawSuggestions = ["grant", "revoke"]
            case "/bossbar", "bossbar":
                rawSuggestions = ["add", "remove", "list", "set", "get"]
            case "/data", "data":
                rawSuggestions = ["get", "merge", "modify", "remove"]
            case "/datapack", "datapack":
                rawSuggestions = ["enable", "disable", "list"]
            case "/enchant", "enchant":
                rawSuggestions = selectors
            case "/execute", "execute":
                rawSuggestions = ["as", "at", "if", "run", "unless", "positioned", "rotated", "align", "anchored", "in", "store"]
            case "/experience", "experience", "/xp", "xp":
                rawSuggestions = ["add", "set", "query"]
            case "/locate", "locate":
                rawSuggestions = ["structure", "biome", "poi"]
            case "/loot", "loot":
                rawSuggestions = ["give", "insert", "replace", "spawn"]
            case "/recipe", "recipe":
                rawSuggestions = ["give", "take"]
            case "/scoreboard", "scoreboard":
                rawSuggestions = ["objectives", "players", "teams"]
            case "/tag", "tag":
                rawSuggestions = selectors
            case "/team", "team":
                rawSuggestions = ["add", "empty", "join", "leave", "list", "modify", "remove"]
            case "/title", "title":
                rawSuggestions = selectors
            case "/worldborder", "worldborder":
                rawSuggestions = ["add", "set", "damage", "get", "warning"]
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
            } else if root == "/tp" || root == "tp" || root == "/teleport" || root == "teleport" {
                rawSuggestions = selectors + ["~ ~ ~"]
            } else if root == "/weather" || root == "weather" {
                rawSuggestions = ["300", "600", "12000"]
            } else if root == "/enchant" || root == "enchant" {
                rawSuggestions = enchantments
            } else if root == "/advancement" || root == "advancement" {
                rawSuggestions = selectors
            } else if root == "/bossbar" || root == "bossbar" {
                if secondArg == "set" || secondArg == "get" {
                    rawSuggestions = ["minecraft:bar_id"]
                }
            } else if root == "/tag" || root == "tag" {
                rawSuggestions = ["add", "remove", "list"]
            } else if root == "/title" || root == "title" {
                rawSuggestions = ["clear", "reset", "title", "subtitle", "actionbar", "times"]
            }
            return rawSuggestions.filter { $0.lowercased().hasPrefix(currentTyped.lowercased()) }
        }
        
        let thirdArg = parts[2].lowercased()
        
        // 4. Fourth argument suggestions
        if count == 4 {
            var rawSuggestions: [String] = []
            if (root == "/effect" || root == "effect") && secondArg == "give" {
                rawSuggestions = effects
            } else if root == "/give" || root == "give" {
                rawSuggestions = ["1", "64"]
            } else if root == "/enchant" || root == "enchant" {
                rawSuggestions = ["1", "5"]
            } else if (root == "/tag" || root == "tag") && (thirdArg == "add" || thirdArg == "remove") {
                rawSuggestions = ["my_tag"]
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

extension String {
    func trimingCharacters(in set: CharacterSet) -> String {
        return self.trimmingCharacters(in: set)
    }
}

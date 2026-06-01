package com.raul1x9.mineconsole.network

object CommandAutocomplete {
    private val rootCommands = listOf(
        "/ban", "/ban-ip", "/banlist", "/clear", "/deop", "/difficulty",
        "/effect", "/gamemode", "/gamerule", "/give", "/help", "/kick",
        "/kill", "/list", "/op", "/pardon", "/pardon-ip", "/say", "/stop",
        "/summon", "/time", "/tp", "/weather", "/whitelist"
    )

    private val effects = listOf(
        "speed", "slowness", "haste", "mining_fatigue", "strength",
        "instant_health", "instant_damage", "jump_boost", "regeneration",
        "resistance", "fire_resistance", "water_breathing", "invisibility",
        "blindness", "night_vision", "hunger", "weakness", "poison",
        "wither", "health_boost", "absorption", "saturation"
    )

    private val items = listOf(
        "diamond", "emerald", "gold_ingot", "iron_ingot", "coal",
        "wood", "stone", "dirt", "grass_block", "cobblestone"
    )

    private val entities = listOf(
        "zombie", "skeleton", "spider", "creeper", "enderman",
        "witch", "slime", "iron_golem", "villager", "cow", "pig",
        "sheep", "chicken"
    )

    private val gamerules = listOf(
        "commandBlockOutput", "doDaylightCycle", "doEntityDrops", "doFireTick",
        "doMobLoot", "doMobSpawning", "doTileDrops", "keepInventory",
        "logAdminCommands", "mobGriefing", "naturalRegeneration",
        "randomTickSpeed", "sendCommandFeedback", "showDeathMessages"
    )

    private val selectors = listOf("@p", "@a", "@r", "@s", "@e")

    fun getSuggestions(input: String): List<String> {
        // If empty, suggest all root commands
        if (input.trim().isEmpty()) {
            return rootCommands
        }

        val parts = input.split(" ")
        if (parts.isEmpty()) return rootCommands

        val count = parts.size
        val currentTyped = parts.last()

        // 1. Root command suggestions
        if (count == 1) {
            val filterTerm = currentTyped.lowercase()
            return rootCommands.filter { cmd ->
                cmd.lowercase().startsWith(filterTerm) ||
                        cmd.replace("/", "").lowercase().startsWith(filterTerm)
            }
        }

        val root = parts[0].lowercase()

        // 2. Second argument suggestions
        if (count == 2) {
            var rawSuggestions = emptyList<String>()
            when (root) {
                "/gamemode", "gamemode" -> rawSuggestions = listOf("survival", "creative", "adventure", "spectator")
                "/difficulty", "difficulty" -> rawSuggestions = listOf("peaceful", "easy", "normal", "hard")
                "/time", "time" -> rawSuggestions = listOf("set", "add", "query")
                "/weather", "weather" -> rawSuggestions = listOf("clear", "rain", "thunder")
                "/whitelist", "whitelist" -> rawSuggestions = listOf("add", "remove", "list", "on", "off")
                "/effect", "effect" -> rawSuggestions = listOf("give", "clear")
                "/gamerule", "gamerule" -> rawSuggestions = gamerules
                "/banlist", "banlist" -> rawSuggestions = listOf("ips", "players")
                "/give", "give", "/clear", "clear", "/kick", "kick", "/kill", "kill", "/op", "op", "/deop", "deop", "/ban", "ban", "/pardon", "pardon", "/tp", "tp" -> rawSuggestions = selectors
                "/list", "list" -> rawSuggestions = listOf("uuids")
            }
            return rawSuggestions.filter { it.lowercase().startsWith(currentTyped.lowercase()) }
        }

        val secondArg = parts[1].lowercase()

        // 3. Third argument suggestions
        if (count == 3) {
            var rawSuggestions = emptyList<String>()
            if (root == "/time" || root == "time") {
                if (secondArg == "set") {
                    rawSuggestions = listOf("day", "night", "noon", "midnight")
                }
            } else if (root == "/gamerule" || root == "gamerule") {
                if (gamerules.contains(parts[1])) {
                    rawSuggestions = if (parts[1] == "randomTickSpeed") {
                        listOf("3", "10", "100")
                    } else {
                        listOf("true", "false")
                    }
                }
            } else if (root == "/whitelist" || root == "whitelist") {
                if (secondArg == "add" || secondArg == "remove") {
                    rawSuggestions = selectors
                }
            } else if (root == "/effect" || root == "effect") {
                rawSuggestions = selectors
            } else if (root == "/give" || root == "give") {
                rawSuggestions = items
            } else if (root == "/clear" || root == "clear") {
                rawSuggestions = items
            } else if (root == "/tp" || root == "tp") {
                rawSuggestions = selectors + listOf("~ ~ ~")
            } else if (root == "/weather" || root == "weather") {
                rawSuggestions = listOf("300", "600", "12000")
            }
            return rawSuggestions.filter { it.lowercase().startsWith(currentTyped.lowercase()) }
        }

        // 4. Fourth argument suggestions
        if (count == 4) {
            var rawSuggestions = emptyList<String>()
            if ((root == "/effect" || root == "effect") && secondArg == "give") {
                rawSuggestions = effects
            } else if (root == "/give" || root == "give") {
                rawSuggestions = listOf("1", "64")
            }
            return rawSuggestions.filter { it.lowercase().startsWith(currentTyped.lowercase()) }
        }

        // 5. Fifth argument suggestions
        if (count == 5) {
            var rawSuggestions = emptyList<String>()
            if ((root == "/effect" || root == "effect") && secondArg == "give") {
                rawSuggestions = listOf("10", "30", "60", "9999", "infinite")
            }
            return rawSuggestions.filter { it.lowercase().startsWith(currentTyped.lowercase()) }
        }

        return emptyList()
    }
}

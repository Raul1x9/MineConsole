package com.raul1x9.mineconsole.network

object CommandAutocomplete {
    private val rootCommands = listOf(
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

    private val enchantments = listOf(
        "sharpness", "unbreaking", "efficiency", "protection", "mending",
        "fortune", "silk_touch", "power", "punch", "infinity", "smite",
        "bane_of_arthropods", "knockback", "fire_aspect", "looting",
        "respiration", "aqua_affinity", "thorns", "depth_strider",
        "frost_walker", "feather_falling"
    )

    private val particles = listOf(
        "ambient_entity_effect", "angry_villager", "block", "bubble", "cloud", "crit",
        "damage_indicator", "dragon_breath", "dripping_lava", "dripping_water",
        "enchant", "enchanted_hit", "end_rod", "explosion", "falling_lava",
        "falling_water", "firework", "flame", "happy_villager", "heart",
        "instant_effect", "item", "lava", "mycelium", "nautilus", "note", "poof",
        "portal", "rain", "smoke", "sneeze", "soul", "soul_fire_flame", "spit",
        "splash", "squid_ink", "totem_of_undying", "underwater", "witch"
    )

    private val gamerules = listOf(
        "commandBlockOutput", "doDaylightCycle", "doEntityDrops", "doFireTick",
        "doMobLoot", "doMobSpawning", "doTileDrops", "keepInventory",
        "logAdminCommands", "mobGriefing", "naturalRegeneration",
        "randomTickSpeed", "sendCommandFeedback", "showDeathMessages",
        "doWeatherCycle", "maxCommandChainLength", "spawnRadius",
        "playersSleepingPercentage"
    )

    private val selectors = listOf("@p", "@a", "@r", "@s", "@e")

    fun getSuggestions(input: String): List<String> {
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
                "/give", "give", "/clear", "clear", "/kick", "kick", "/kill", "kill", "/op", "op", "/deop", "deop", "/ban", "ban", "/pardon", "pardon", "/tp", "tp", "/teleport", "teleport" -> rawSuggestions = selectors
                "/list", "list" -> rawSuggestions = listOf("uuids")
                "/advancement", "advancement" -> rawSuggestions = listOf("grant", "revoke")
                "/bossbar", "bossbar" -> rawSuggestions = listOf("add", "remove", "list", "set", "get")
                "/data", "data" -> rawSuggestions = listOf("get", "merge", "modify", "remove")
                "/datapack", "datapack" -> rawSuggestions = listOf("enable", "disable", "list")
                "/enchant", "enchant" -> rawSuggestions = selectors
                "/execute", "execute" -> rawSuggestions = listOf("as", "at", "if", "run", "unless", "positioned", "rotated", "align", "anchored", "in", "store")
                "/experience", "experience", "/xp", "xp" -> rawSuggestions = listOf("add", "set", "query")
                "/locate", "locate" -> rawSuggestions = listOf("structure", "biome", "poi")
                "/loot", "loot" -> rawSuggestions = listOf("give", "insert", "replace", "spawn")
                "/recipe", "recipe" -> rawSuggestions = listOf("give", "take")
                "/scoreboard", "scoreboard" -> rawSuggestions = listOf("objectives", "players", "teams")
                "/tag", "tag" -> rawSuggestions = selectors
                "/team", "team" -> rawSuggestions = listOf("add", "empty", "join", "leave", "list", "modify", "remove")
                "/title", "title" -> rawSuggestions = selectors
                "/worldborder", "worldborder" -> rawSuggestions = listOf("add", "set", "damage", "get", "warning")
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
            } else if (root == "/tp" || root == "tp" || root == "/teleport" || root == "teleport") {
                rawSuggestions = selectors + listOf("~ ~ ~")
            } else if (root == "/weather" || root == "weather") {
                rawSuggestions = listOf("300", "600", "12000")
            } else if (root == "/enchant" || root == "enchant") {
                rawSuggestions = enchantments
            } else if (root == "/advancement" || root == "advancement") {
                rawSuggestions = selectors
            } else if (root == "/bossbar" || root == "bossbar") {
                if (secondArg == "set" || secondArg == "get") {
                    rawSuggestions = listOf("minecraft:bar_id")
                }
            } else if (root == "/tag" || root == "tag") {
                rawSuggestions = listOf("add", "remove", "list")
            } else if (root == "/title" || root == "title") {
                rawSuggestions = listOf("clear", "reset", "title", "subtitle", "actionbar", "times")
            }
            return rawSuggestions.filter { it.lowercase().startsWith(currentTyped.lowercase()) }
        }

        val thirdArg = parts[2].lowercase()

        // 4. Fourth argument suggestions
        if (count == 4) {
            var rawSuggestions = emptyList<String>()
            if ((root == "/effect" || root == "effect") && secondArg == "give") {
                rawSuggestions = effects
            } else if (root == "/give" || root == "give") {
                rawSuggestions = listOf("1", "64")
            } else if ((root == "/enchant" || root == "enchant")) {
                rawSuggestions = listOf("1", "5")
            } else if ((root == "/tag" || root == "tag") && (thirdArg == "add" || thirdArg == "remove")) {
                rawSuggestions = listOf("my_tag")
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

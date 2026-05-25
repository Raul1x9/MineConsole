package com.raul1x9.mineconsole.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "server_profiles")
data class ServerProfile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    var name: String,
    var ip: String,
    var rconPort: Int,
    var keychainKey: String,
    var sharedRole: String = "Owner",
    val creationDate: Long = System.currentTimeMillis()
)

package com.raul1x9.mineconsole.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.raul1x9.mineconsole.models.ServerProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM server_profiles ORDER BY creationDate DESC")
    fun getAllServersFlow(): Flow<List<ServerProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerProfile)

    @Delete
    suspend fun deleteServer(server: ServerProfile)
}

@Database(entities = [ServerProfile::class], version = 1, exportSchema = false)
abstract class ServerDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao

    companion object {
        @Volatile
        private var INSTANCE: ServerDatabase? = null

        fun getInstance(context: Context): ServerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ServerDatabase::class.java,
                    "mineconsole_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

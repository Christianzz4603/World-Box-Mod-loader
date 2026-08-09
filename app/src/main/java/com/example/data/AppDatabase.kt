package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.LogDao
import com.example.data.dao.ModDao
import com.example.data.dao.ProfileDao
import com.example.data.entities.LauncherLogEntity
import com.example.data.entities.ModEntity
import com.example.data.entities.ProfileEntity
import com.example.data.entities.ProfileModCrossRef

@Database(
    entities = [
        ModEntity::class,
        ProfileEntity::class,
        ProfileModCrossRef::class,
        LauncherLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun modDao(): ModDao
    abstract fun profileDao(): ProfileDao
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "worldbox_launcher_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

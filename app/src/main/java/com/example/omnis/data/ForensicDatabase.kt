package com.example.omnis.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.omnis.model.ForensicCase

@Database(entities = [ForensicCase::class], version = 1, exportSchema = false)
abstract class ForensicDatabase : RoomDatabase() {
    abstract fun forensicDao(): ForensicDao

    companion object {
        @Volatile
        private var INSTANCE: ForensicDatabase? = null

        fun getDatabase(context: Context): ForensicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ForensicDatabase::class.java,
                    "verum_omnis_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

package com.amisapps.amismind.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.amisapps.amismind.data.database.dao.NoteDao
import com.amisapps.amismind.data.database.model.Note

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "amismind_database" // Database name
                )
                // Wipes and rebuilds instead of migrating if no Migration object.
                // Migration is not covered in this basic example.
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }
}

package com.athr.karaoketv.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [SongEntity::class], version = 2, exportSchema = true)
abstract class KaraokeDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao

    companion object {
        /** Adds the manual title/artist correction flag, keeping play history. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN swapped INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var instance: KaraokeDatabase? = null

        fun get(context: Context): KaraokeDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KaraokeDatabase::class.java,
                    "karaoke.db",
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { instance = it }
            }
    }
}

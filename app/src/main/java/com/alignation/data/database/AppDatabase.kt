package com.alignation.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alignation.data.model.AlignmentEvent
import com.alignation.data.model.UserSettings

@Database(
    entities = [AlignmentEvent::class, UserSettings::class],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alignmentEventDao(): AlignmentEventDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add user_settings table - preserves all existing alignment_events
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS user_settings (
                        id INTEGER NOT NULL PRIMARY KEY DEFAULT 1,
                        treatmentStartDate INTEGER NOT NULL,
                        treatmentWeeks INTEGER NOT NULL DEFAULT 16,
                        dailyAllowanceMinutes INTEGER NOT NULL DEFAULT 120,
                        maxAllowanceMinutes INTEGER NOT NULL DEFAULT 180,
                        maxGraceMinutes INTEGER NOT NULL DEFAULT 30,
                        createdAt INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}

package com.alignation.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.alignation.data.model.AlignerSet
import com.alignation.data.model.AlignmentEvent
import com.alignation.data.model.AuditLogEntry
import com.alignation.data.model.PhotoSet
import com.alignation.data.model.UserSettings

@Database(
    entities = [
        AlignmentEvent::class,
        UserSettings::class,
        AlignerSet::class,
        AuditLogEntry::class,
        PhotoSet::class
    ],
    version = 4,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alignmentEventDao(): AlignmentEventDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun alignerSetDao(): AlignerSetDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun photoSetDao(): PhotoSetDao

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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add aligner_sets table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS aligner_sets (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        setNumber INTEGER NOT NULL,
                        startedAt INTEGER NOT NULL,
                        notes TEXT
                    )
                """)

                // Insert initial set (Set 1, started at migration time)
                val now = System.currentTimeMillis()
                database.execSQL(
                    "INSERT INTO aligner_sets (setNumber, startedAt) VALUES (1, ?)",
                    arrayOf(now)
                )

                // Add audit_log table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS audit_log (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        timestamp INTEGER NOT NULL,
                        action TEXT NOT NULL,
                        entityType TEXT NOT NULL,
                        entityId INTEGER NOT NULL,
                        oldValue TEXT,
                        newValue TEXT
                    )
                """)

                // Add photo_sets table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS photo_sets (
                        id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        takenAt INTEGER NOT NULL,
                        alignerSetNumber INTEGER NOT NULL,
                        frontPhotoPath TEXT,
                        leftPhotoPath TEXT,
                        rightPhotoPath TEXT,
                        notes TEXT
                    )
                """)

                // Add new settings columns
                database.execSQL("ALTER TABLE user_settings ADD COLUMN alertSound1hUri TEXT")
                database.execSQL("ALTER TABLE user_settings ADD COLUMN alertSound15mBeforeSoftUri TEXT")
                database.execSQL("ALTER TABLE user_settings ADD COLUMN alertSound15mBeforeHardUri TEXT")
                database.execSQL("ALTER TABLE user_settings ADD COLUMN alertSound5mBeforeHardUri TEXT")
                database.execSQL("ALTER TABLE user_settings ADD COLUMN enableGraceTime INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE user_settings ADD COLUMN enableAlarm1h INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE user_settings ADD COLUMN enableAlarm15mBeforeSoft INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE user_settings ADD COLUMN enableAlarm15mBeforeHard INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE user_settings ADD COLUMN enableAlarm5mBeforeHard INTEGER NOT NULL DEFAULT 1")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE user_settings ADD COLUMN enableAlarm30m INTEGER NOT NULL DEFAULT 1")
            }
        }
    }
}

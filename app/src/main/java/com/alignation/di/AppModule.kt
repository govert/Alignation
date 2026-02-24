package com.alignation.di

import android.content.Context
import androidx.room.Room
import com.alignation.data.database.AlignerSetDao
import com.alignation.data.database.AlignmentEventDao
import com.alignation.data.database.AppDatabase
import com.alignation.data.database.AuditLogDao
import com.alignation.data.database.PhotoSetDao
import com.alignation.data.database.UserSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "alignation_database"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
    }

    @Provides
    @Singleton
    fun provideAlignmentEventDao(database: AppDatabase): AlignmentEventDao {
        return database.alignmentEventDao()
    }

    @Provides
    @Singleton
    fun provideUserSettingsDao(database: AppDatabase): UserSettingsDao {
        return database.userSettingsDao()
    }

    @Provides
    @Singleton
    fun provideAlignerSetDao(database: AppDatabase): AlignerSetDao {
        return database.alignerSetDao()
    }

    @Provides
    @Singleton
    fun provideAuditLogDao(database: AppDatabase): AuditLogDao {
        return database.auditLogDao()
    }

    @Provides
    @Singleton
    fun providePhotoSetDao(database: AppDatabase): PhotoSetDao {
        return database.photoSetDao()
    }
}

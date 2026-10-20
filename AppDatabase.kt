package com.dermify.dermify.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dermify.dermify.curation.ContributionOutbox
import com.dermify.dermify.curation.ContributionOutboxDao

@Database(
    entities = [SkinScan::class, ContributionOutbox::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun skinScanDao(): SkinScanDao

    abstract fun contributionOutboxDao(): ContributionOutboxDao

    companion object {
        /**
         * Adds the contribution outbox. Written out rather than left to the
         * destructive fallback because the fallback would wipe local scan
         * history on upgrade, and because an outbox that silently loses queued
         * consents defeats the point of having one.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `contribution_outbox` (
                        `contributionId` TEXT NOT NULL,
                        `imagePath` TEXT NOT NULL,
                        `phash` TEXT NOT NULL,
                        `predictedClass` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `consentVersion` TEXT NOT NULL,
                        `appVersion` TEXT NOT NULL,
                        `createdAtDate` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `attemptCount` INTEGER NOT NULL,
                        `lastError` TEXT,
                        PRIMARY KEY(`contributionId`)
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val existing = INSTANCE
                if (existing != null) return existing
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dermify_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

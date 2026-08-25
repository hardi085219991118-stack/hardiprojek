package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.FarmDao
import com.example.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        PhotoEvidenceEntity::class,
        ReportDispatchHistoryEntity::class,
        CoopEntity::class,
        PartnerEntity::class,
        CycleEntity::class,
        DailyLogEntity::class,
        MortalityLogEntity::class,
        FeedStockEntity::class,
        WeightSampleEntity::class,
        MedicineEntity::class,
        ExpenseEntity::class,
        HarvestEntity::class,
        FarmProfileEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun farmDao(): FarmDao

    companion object {
        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE expenses ADD COLUMN transactionType TEXT NOT NULL DEFAULT 'OUT'")
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE daily_logs ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE coops ADD COLUMN photoUri TEXT NOT NULL DEFAULT ''")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sejahtera_bersama_farm.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    // Prevent force-close on legacy installs when an older, unknown schema is found.
                    // Current supported migrations are still preferred; destructive fallback is only
                    // used by Room when no valid migration path exists.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

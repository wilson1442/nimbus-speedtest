package cloud.g3h.nimbus.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TestResult::class], version = 2, exportSchema = false)
abstract class NimbusDatabase : RoomDatabase() {

    abstract fun testResultDao(): TestResultDao

    companion object {
        @Volatile
        private var instance: NimbusDatabase? = null

        fun get(context: Context): NimbusDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NimbusDatabase::class.java,
                    "nimbus.db"
                )
                    .addMigrations(
                        object : androidx.room.migration.Migration(1, 2) {
                            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                                db.execSQL("ALTER TABLE test_results ADD COLUMN min_ping_ms REAL NOT NULL DEFAULT 0")
                                db.execSQL("ALTER TABLE test_results ADD COLUMN peak_mbps REAL NOT NULL DEFAULT 0")
                            }
                        }
                    )
                    .build().also { instance = it }
            }
    }
}

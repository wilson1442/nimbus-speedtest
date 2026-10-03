package cloud.g3h.nimbus.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TestResult::class], version = 1, exportSchema = false)
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
                ).build().also { instance = it }
            }
    }
}

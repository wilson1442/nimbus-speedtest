package cloud.g3h.nimbus.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TestResultDao {

    @Insert
    suspend fun insert(result: TestResult): Long

    @Query("SELECT * FROM test_results ORDER BY timestamp DESC LIMIT 1")
    suspend fun latest(): TestResult?

    @Query("SELECT * FROM test_results WHERE timestamp >= :sinceMs ORDER BY timestamp DESC")
    suspend fun since(sinceMs: Long): List<TestResult>

    @Query(
        """
        SELECT
          COALESCE(AVG(download_mbps), 0) AS avgDownloadMbps,
          COALESCE(AVG(upload_mbps), 0) AS avgUploadMbps,
          COALESCE(AVG(ping_ms), 0) AS avgPingMs,
          COUNT(*) AS count,
          COALESCE(MIN(download_mbps), 0) AS minDownloadMbps,
          COALESCE(MAX(download_mbps), 0) AS maxDownloadMbps
        FROM test_results
        WHERE timestamp >= :sinceMs AND (COALESCE(:vpnFilter, -1) = -1 OR vpn_active = :vpnFilter)
        """
    )
    suspend fun stats(sinceMs: Long, vpnFilter: Int? = null): RangeStats?

    @Query("DELETE FROM test_results")
    suspend fun deleteAll()
}

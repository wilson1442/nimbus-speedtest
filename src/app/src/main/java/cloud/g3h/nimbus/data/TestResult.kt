package cloud.g3h.nimbus.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "test_results")
data class TestResult(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "download_mbps") val downloadMbps: Double,
    @ColumnInfo(name = "upload_mbps") val uploadMbps: Double,
    @ColumnInfo(name = "ping_ms") val pingMs: Double,
    @ColumnInfo(name = "jitter_ms") val jitterMs: Double,
    @ColumnInfo(name = "packet_loss_pct") val packetLossPct: Double,
    @ColumnInfo(name = "connection_type") val connectionType: String,
    @ColumnInfo(name = "vpn_active") val vpnActive: Boolean,
    @ColumnInfo(name = "wan_ip") val wanIp: String?,
    @ColumnInfo(name = "server_name") val serverName: String
)

/** Aggregate stats for a time range, computed by the DAO. */
data class RangeStats(
    val avgDownloadMbps: Double,
    val avgUploadMbps: Double,
    val avgPingMs: Double,
    val count: Int,
    val minDownloadMbps: Double,
    val maxDownloadMbps: Double
)

/** One sample inside a run, used by sparklines and the background graphics. */
data class Sample(val phase: Phase, val value: Double, val atMs: Long)

enum class Phase { PING, DOWNLOAD, UPLOAD }

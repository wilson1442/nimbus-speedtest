package cloud.g3h.nimbus

import android.app.Application
import cloud.g3h.nimbus.data.NimbusDatabase
import cloud.g3h.nimbus.engine.LibreSpeedEngine
import cloud.g3h.nimbus.engine.SpeedTestEngine
import cloud.g3h.nimbus.net.ConnectionMonitor

/** Manual DI graph (kept simple per spec §1). */
class NimbusApp : Application() {

    lateinit var database: NimbusDatabase
        private set

    lateinit var connectionMonitor: ConnectionMonitor
        private set

    lateinit var speedTestEngine: SpeedTestEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = NimbusDatabase.get(this)
        connectionMonitor = ConnectionMonitor(this)
        speedTestEngine = LibreSpeedEngine()
        connectionMonitor.start()
    }
}

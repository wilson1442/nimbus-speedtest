package cloud.g3h.nimbus.net

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the curated server catalogue: the UI renders these labels verbatim and
 * the engine hits these URLs, so a typo or duplicate would ship a broken choice.
 */
class SpeedServersTest {

    @Test
    fun `catalogue is not empty and every url is https with no trailing slash`() {
        assertTrue(SpeedServers.ALL.isNotEmpty())
        SpeedServers.ALL.forEach { s ->
            assertTrue("${s.label} url must be https: ${s.url}", s.url.startsWith("https://"))
            assertTrue("${s.label} label must not be blank", s.label.isNotBlank())
            assertTrue("${s.label} region must not be blank", s.region.isNotBlank())
            assertEquals("${s.label} url must not end in /", s.url, s.url.trimEnd('/'))
        }
    }

    @Test
    fun `urls and labels are unique`() {
        val urls = SpeedServers.ALL.map { it.url }
        val labels = SpeedServers.ALL.map { it.label }
        assertEquals("duplicate url in catalogue", urls.size, urls.toSet().size)
        assertEquals("duplicate label in catalogue", labels.size, labels.toSet().size)
    }

    @Test
    fun `covers the advertised regions`() {
        val regions = SpeedServers.ALL.map { it.region }.toSet()
        listOf("US · East", "US · Central", "US · West", "Europe", "Asia").forEach {
            assertTrue("missing region $it", it in regions)
        }
    }

    @Test
    fun `new york default is in the catalogue`() {
        // The build-time default must be selectable so the picker can tick it.
        assertTrue(SpeedServers.ALL.any { it.url == "https://nyc.speedtest.clouvider.net/backend" })
    }

    @Test
    fun `forUrl matches exact, ignores trailing slash and case`() {
        val nyc = "https://nyc.speedtest.clouvider.net/backend"
        assertEquals("New York, US", SpeedServers.forUrl(nyc)?.label)
        assertEquals("New York, US", SpeedServers.forUrl("$nyc/")?.label)
        assertEquals("New York, US", SpeedServers.forUrl(nyc.uppercase())?.label)
        assertNull(SpeedServers.forUrl("https://example.com/backend"))
        assertNull(SpeedServers.forUrl(""))
    }

    @Test
    fun `displayName falls back to host for unknown urls`() {
        assertEquals("Auto (nearest)", SpeedServers.displayName(""))
        assertEquals("London, UK", SpeedServers.displayName("https://lon.speedtest.clouvider.net/backend"))
        assertEquals("custom.example.net", SpeedServers.displayName("https://custom.example.net/backend"))
    }

    @Test
    fun `every preset carries a flag drawable`() {
        SpeedServers.ALL.forEach { s ->
            assertTrue("${s.label} is missing a flag resource", s.flag != 0)
        }
        assertTrue("custom endpoints need a fallback flag", SpeedServers.CUSTOM_FLAG != 0)
    }

    @Test
    fun `shortName drops the country suffix`() {
        assertEquals("New York", SpeedServers.ALL.first { it.label == "New York, US" }.shortName)
        assertEquals("Grand Rapids", SpeedServers.ALL.first { it.label == "Grand Rapids, US" }.shortName)
        assertEquals("Tokyo", SpeedServers.ALL.first { it.label == "Tokyo, JP" }.shortName)
    }
}

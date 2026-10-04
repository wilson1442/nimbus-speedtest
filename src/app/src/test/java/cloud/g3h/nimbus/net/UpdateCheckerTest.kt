package cloud.g3h.nimbus.net

import org.junit.Assert.*
import org.junit.Test

/** Parser tests for the GitHub Releases update feed (no network). */
class UpdateCheckerTest {

    private val sample = """
    {
      "tag_name": "v1.2.0",
      "name": "Nimbus Speed Test v1.2.0",
      "body": "Realtime bandwidth gauges.\n\nnimbus-versionCode=5",
      "assets": [
        { "name": "notes.txt", "browser_download_url": "https://x/notes.txt", "size": 100 },
        { "name": "nimbus-speedtest-1.2.0-signed.apk", "browser_download_url": "https://x/apk", "size": 8162592 }
      ]
    }
    """.trimIndent()

    @Test
    fun `parses signed apk asset and version code from body`() {
        val info = UpdateChecker.parseRelease(sample)
        assertNotNull(info)
        assertEquals("v1.2.0", info!!.tagName)
        assertEquals(5, info.versionCode)
        assertEquals("nimbus-speedtest-1.2.0-signed.apk", info.assetName)
        assertEquals("https://x/apk", info.assetUrl)
        assertEquals(8162592L, info.assetSize)
    }

    @Test
    fun `ignores assets without the signed apk`() {
        val json = """{"tag_name":"v1.0.0","name":"x","body":"nimbus-versionCode=1",
            "assets":[{"name":"notes.txt","browser_download_url":"u","size":1}]}"""
        assertNull(UpdateChecker.parseRelease(json))
    }

    @Test
    fun `missing version code line yields zero`() {
        val json = """{"tag_name":"v1.0.0","name":"x","body":"no code here",
            "assets":[{"name":"a-signed.apk","browser_download_url":"u","size":1}]}"""
        assertEquals(0, UpdateChecker.parseRelease(json)!!.versionCode)
    }

    @Test
    fun `malformed json yields null`() {
        assertNull(UpdateChecker.parseRelease("{nope"))
    }

    // --- auto-update throttle gate ---

    @Test
    fun `never-checked is always due`() {
        assertTrue(UpdateChecker.isCheckDue(0L, nowMillis = 1_000_000L))
    }

    @Test
    fun `check inside the interval is not due`() {
        val interval = UpdateChecker.CHECK_INTERVAL_MS
        val last = 10_000_000L
        assertFalse(UpdateChecker.isCheckDue(last, last + interval - 1))
    }

    @Test
    fun `check past the interval is due`() {
        val interval = UpdateChecker.CHECK_INTERVAL_MS
        val last = 10_000_000L
        assertTrue(UpdateChecker.isCheckDue(last, last + interval))
        assertTrue(UpdateChecker.isCheckDue(last, last + interval * 3))
    }

    @Test
    fun `default interval is six hours`() {
        assertEquals(6L * 3600_000, UpdateChecker.CHECK_INTERVAL_MS)
        // 5h59m after a check -> not due; 6h -> due
        val last = 1_000_000L
        assertFalse(UpdateChecker.isCheckDue(last, last + 5L * 3600_000 + 59L * 60_000))
        assertTrue(UpdateChecker.isCheckDue(last, last + 6L * 3600_000))
    }
}

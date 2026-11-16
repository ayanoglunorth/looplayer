package app.looplayer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceVideosTest {
    private fun row(title: String, bucket: Long, added: Long, name: String = "F$bucket") =
        DeviceVideos.Row(DeviceVideo("content://v/$title", title, 1000, 10, added), bucket, name)

    @Test fun `groups by folder, newest folder first, episodes in natural order`() {
        val folders = DeviceVideos.group(
            listOf(row("Show E10", 1, 5), row("Show E2", 1, 4), row("Film", 2, 9), row("Show E1", 1, 3)),
        )
        assertEquals(listOf("F2", "F1"), folders.map { it.name })
        assertEquals(listOf("Show E1", "Show E2", "Show E10"), folders[1].videos.map { it.title })
    }

    @Test fun `empty files are not listed`() {
        val broken = DeviceVideos.Row(DeviceVideo("content://v/x", "x", 0, 0, 1), 1, "F1")
        assertEquals(emptyList<DeviceFolder>(), DeviceVideos.group(listOf(broken)))
    }
}

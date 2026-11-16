package app.looplayer.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A video found on the device by the system media index. */
data class DeviceVideo(val uri: String, val title: String, val durationMs: Long, val sizeBytes: Long, val addedSec: Long)

/** The videos that live in one folder of the device. */
data class DeviceFolder(val id: Long, val name: String, val videos: List<DeviceVideo>)

/** Every video on the device, grouped by folder, read from MediaStore (no file browsing needed). */
object DeviceVideos {

    val permission: String
        get() = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_VIDEO else Manifest.permission.READ_EXTERNAL_STORAGE

    fun hasAccess(context: Context) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    suspend fun load(context: Context): List<DeviceFolder> = withContext(Dispatchers.IO) {
        val base = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val cols = arrayOf(
            MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE, MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_ID, MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
        )
        val rows = mutableListOf<Row>()
        runCatching {
            context.contentResolver.query(base, cols, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    rows += Row(
                        video = DeviceVideo(
                            uri = ContentUris.withAppendedId(base, c.getLong(0)).toString(),
                            title = Files.titleFrom(c.getString(1) ?: ""),
                            durationMs = c.getLong(2), sizeBytes = c.getLong(3), addedSec = c.getLong(4),
                        ),
                        bucketId = c.getLong(5), bucketName = c.getString(6),
                    )
                }
            }
        }
        group(rows)
    }

    internal class Row(val video: DeviceVideo, val bucketId: Long, val bucketName: String?)

    /** Newest folder first (fresh downloads on top); episodes inside a folder in natural order, so E2 comes before E10. */
    internal fun group(rows: List<Row>): List<DeviceFolder> =
        rows.filter { it.video.durationMs > 0 || it.video.sizeBytes > 0 }
            .groupBy { it.bucketId }
            .map { (id, list) ->
                DeviceFolder(id, list.first().bucketName ?: "", list.map { it.video }.sortedBy { naturalKey(it.title) })
            }
            .sortedByDescending { f -> f.videos.maxOf { it.addedSec } }

    internal fun naturalKey(title: String) =
        title.lowercase().replace(Regex("[0-9]+")) { it.value.padStart(10, '0') }
}

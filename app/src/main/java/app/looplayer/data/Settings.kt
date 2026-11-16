package app.looplayer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Bundled open-licence (OFL) faces with full Turkish coverage, plus the device serif. */
enum class SubtitleFont { Inter, Roboto, Atkinson, OpenSans, PtNarrow, Inconsolata, Serif }

enum class SubtitleColor(val argb: Long) {
    White(0xFFFFFFFF), Yellow(0xFFFFE45C), Cyan(0xFF8FE3FF), Green(0xFFA6F0A0)
}

enum class SubtitleBackdrop { Outline, Shadow, Box }

/** Whether the screen follows the video's shape or simply follows the sensor. */
enum class OrientationMode { Video, Sensor }

/** The two interface languages. Turkish is the source language; English is its translation. */
enum class AppLanguage(val tag: String) { Turkish("tr"), English("en") }

/** Everything the user can change in Settings. */
data class AppSettings(
    val languageIndicator: Boolean = true,
    val swipeAdjust: Boolean = true,
    val holdBoost: Boolean = true,
    /** Slide and flip on a language switch; off makes the change instant. */
    val switchAnimation: Boolean = true,
    val holdSpeed: Float = 2f,
    val skipSeconds: Int = 10,
    val controlsTimeoutSec: Int = 3,
    val orientation: OrientationMode = OrientationMode.Video,
    val font: SubtitleFont = SubtitleFont.Inter,
    val sizePercent: Int = 100,
    val bold: Boolean = false,
    val color: SubtitleColor = SubtitleColor.White,
    val backdrop: SubtitleBackdrop = SubtitleBackdrop.Outline,
    /** Extra lift above the bottom edge, as a percent of the screen height. */
    val position: Int = 0,
    val textOpacity: Int = 100,
    /** Only the Box backdrop uses it. */
    val backdropOpacity: Int = 70,
    /** null follows the system. */
    val dark: Boolean? = null,
    /** null follows the system (Turkish unless the device speaks English). */
    val language: AppLanguage? = null,
)

/** Settings persisted in SharedPreferences; [value] is observable state. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var value by mutableStateOf(load())
        private set

    fun update(change: (AppSettings) -> AppSettings) {
        value = change(value)
        prefs.edit()
            .putBoolean("indicator", value.languageIndicator)
            .putBoolean("swipe", value.swipeAdjust)
            .putBoolean("hold", value.holdBoost)
            .putBoolean("switchAnim", value.switchAnimation)
            .putFloat("holdSpeed", value.holdSpeed)
            .putInt("skip", value.skipSeconds)
            .putInt("controlsTimeout", value.controlsTimeoutSec)
            .putString("orientation", value.orientation.name)
            .putInt("position", value.position)
            .putInt("textOpacity", value.textOpacity)
            .putInt("backdropOpacity", value.backdropOpacity)
            .putString("font", value.font.name)
            .putInt("size", value.sizePercent)
            .putBoolean("bold", value.bold)
            .putString("color", value.color.name)
            .putString("backdrop", value.backdrop.name)
            .apply { value.dark?.let { putBoolean("dark", it) } ?: remove("dark") }
            .apply { value.language?.let { putString("language", it.name) } ?: remove("language") }
            .apply()
    }

    private fun load(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            languageIndicator = prefs.getBoolean("indicator", d.languageIndicator),
            swipeAdjust = prefs.getBoolean("swipe", d.swipeAdjust),
            holdBoost = prefs.getBoolean("hold", d.holdBoost),
            switchAnimation = prefs.getBoolean("switchAnim", d.switchAnimation),
            holdSpeed = prefs.getFloat("holdSpeed", d.holdSpeed),
            skipSeconds = prefs.getInt("skip", d.skipSeconds),
            controlsTimeoutSec = prefs.getInt("controlsTimeout", d.controlsTimeoutSec),
            orientation = enumPref("orientation", d.orientation),
            position = prefs.getInt("position", d.position).coerceIn(0, MAX_POSITION),
            textOpacity = prefs.getInt("textOpacity", d.textOpacity).coerceIn(MIN_OPACITY, 100),
            backdropOpacity = prefs.getInt("backdropOpacity", d.backdropOpacity).coerceIn(0, 100),
            font = enumPref("font", d.font),
            sizePercent = prefs.getInt("size", d.sizePercent).coerceIn(MIN_SIZE, MAX_SIZE),
            bold = prefs.getBoolean("bold", d.bold),
            color = enumPref("color", d.color),
            backdrop = enumPref("backdrop", d.backdrop),
            dark = if (prefs.contains("dark")) prefs.getBoolean("dark", true) else null,
            language = prefs.getString("language", null)?.let { n -> AppLanguage.entries.firstOrNull { it.name == n } },
        )
    }

    private inline fun <reified E : Enum<E>> enumPref(key: String, default: E): E =
        prefs.getString(key, null)?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    companion object {
        /** Read before any UI exists, to pick the resources the activity starts with. */
        fun savedLanguage(context: Context): AppLanguage? =
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("language", null)
                ?.let { n -> AppLanguage.entries.firstOrNull { it.name == n } }

        const val MIN_SIZE = 70
        const val MAX_SIZE = 200
        const val MAX_POSITION = 50
        const val MIN_OPACITY = 30

        @Volatile private var instance: SettingsStore? = null

        /** One shared instance, so the library and the player always show the same state. */
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) { instance ?: SettingsStore(context.applicationContext).also { instance = it } }
    }
}

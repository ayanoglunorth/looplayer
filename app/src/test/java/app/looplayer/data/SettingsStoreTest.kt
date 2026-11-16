package app.looplayer.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class SettingsStoreTest {
    private val app get() = RuntimeEnvironment.getApplication()

    @Test fun `defaults keep the language indicator on`() {
        assertTrue(SettingsStore(app).value.languageIndicator)
    }

    @Test fun `changes persist to a new store`() {
        SettingsStore(app).update {
            it.copy(languageIndicator = false, font = SubtitleFont.Serif, sizePercent = 140, color = SubtitleColor.Yellow, backdrop = SubtitleBackdrop.Box)
        }
        val reloaded = SettingsStore(app).value
        assertEquals(false, reloaded.languageIndicator)
        assertEquals(SubtitleFont.Serif, reloaded.font)
        assertEquals(140, reloaded.sizePercent)
        assertEquals(SubtitleColor.Yellow, reloaded.color)
        assertEquals(SubtitleBackdrop.Box, reloaded.backdrop)
    }

    @Test fun `theme and language default to the system and persist when chosen`() {
        assertEquals(null, SettingsStore(app).value.dark)
        assertEquals(null, SettingsStore.savedLanguage(app))
        SettingsStore(app).update { it.copy(dark = false, language = AppLanguage.English) }
        assertEquals(false, SettingsStore(app).value.dark)
        assertEquals(AppLanguage.English, SettingsStore.savedLanguage(app))
        SettingsStore(app).update { it.copy(dark = null, language = null) }
        assertEquals(null, SettingsStore(app).value.dark)
    }
}

package com.charles.skypulse.app

import com.charles.skypulse.app.ui.util.SHORT_SCREEN_HEIGHT_DP
import com.charles.skypulse.app.ui.util.isShortScreen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenSizeTest {
    @Test
    fun fiveInchPhoneIsShort() {
        // Sony Xperia X class: 720x1280 at xhdpi = 360x640 dp.
        assertTrue(isShortScreen(640))
        assertTrue(isShortScreen(592))
    }

    @Test
    fun typicalPhonesAreNotShort() {
        assertFalse(isShortScreen(SHORT_SCREEN_HEIGHT_DP))
        assertFalse(isShortScreen(780))
        assertFalse(isShortScreen(891))
    }

    @Test
    fun undefinedHeightIsNotShort() {
        // Configuration.SCREEN_HEIGHT_DP_UNDEFINED == 0: keep the default layout.
        assertFalse(isShortScreen(0))
    }
}

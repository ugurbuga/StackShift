package com.ugurbuga.blockgames.ui.game

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals

class GameDialogsCommonTest {

    @Test
    fun resolveActionButtonContentColor_prefersExplicitContentColor() {
        val explicit = Color(0xFF12AB34)
        val fallback = Color(0xFF556677)

        assertEquals(explicit, resolveActionButtonContentColor(contentColor = explicit, fallbackColor = fallback))
    }

    @Test
    fun resolveActionButtonContentColor_fallsBackWhenExplicitColorIsMissing() {
        val fallback = Color(0xFF556677)

        assertEquals(fallback, resolveActionButtonContentColor(contentColor = null, fallbackColor = fallback))
    }
}


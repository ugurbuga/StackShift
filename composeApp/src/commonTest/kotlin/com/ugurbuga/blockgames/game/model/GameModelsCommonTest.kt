package com.ugurbuga.blockgames.game.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameModelsCommonTest {

    @Test
    fun square3x3_pieceKind_hasLevelFiveNineCellTemplate() {
        val pieceKind = PieceKind.Square3x3

        assertEquals(5, pieceKind.unlockLevel)
        assertEquals(9, pieceKind.template.size)
        assertEquals(0, pieceKind.template.minOf(GridPoint::column))
        assertEquals(2, pieceKind.template.maxOf(GridPoint::column))
        assertEquals(0, pieceKind.template.minOf(GridPoint::row))
        assertEquals(2, pieceKind.template.maxOf(GridPoint::row))
        assertTrue(pieceKind.template.contains(GridPoint(1, 1)))
        assertTrue(pieceKind.template.contains(GridPoint(2, 2)))
    }
}


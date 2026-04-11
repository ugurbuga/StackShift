package com.ugurbuga.stackshift.presentation.game

import com.ugurbuga.stackshift.game.logic.GameEvent
import com.ugurbuga.stackshift.platform.feedback.GameHaptic
import com.ugurbuga.stackshift.platform.feedback.GameSound

class GameFeedbackMapper {
    fun map(events: Set<GameEvent>): InteractionFeedback {
        if (events.isEmpty()) return InteractionFeedback.None

        val sounds = linkedSetOf<GameSound>()
        val haptics = linkedSetOf<GameHaptic>()

        if (GameEvent.InvalidDrop in events) {
            sounds += GameSound.DropInvalid
            haptics += GameHaptic.Warning
        }
        if (GameEvent.PlacementAccepted in events) {
            sounds += GameSound.DropSuccess
            haptics += GameHaptic.Light
        }
        if (GameEvent.SoftLockStarted in events || GameEvent.SoftLockAdjusted in events) {
            sounds += GameSound.Grab
            haptics += GameHaptic.Light
        }
        if (GameEvent.LineClear in events) {
            sounds += GameSound.LineClear
            haptics += GameHaptic.Success
        }
        if (GameEvent.ChainReaction in events || GameEvent.Combo in events || GameEvent.PerfectDrop in events) {
            sounds += GameSound.Combo
            haptics += GameHaptic.Success
        }
        if (GameEvent.Paused in events) {
            sounds += GameSound.Pause
            haptics += GameHaptic.Light
        }
        if (GameEvent.Resumed in events) {
            sounds += GameSound.Resume
            haptics += GameHaptic.Light
        }
        if (GameEvent.HoldUsed in events) {
            sounds += GameSound.Grab
            haptics += GameHaptic.Light
        }
        if (GameEvent.LaunchBoostCharged in events || GameEvent.SpecialTriggered in events) {
            sounds += GameSound.Combo
            haptics += GameHaptic.Success
        }
        if (GameEvent.PressureCritical in events) {
            haptics += GameHaptic.Warning
        }
        if (GameEvent.GameOver in events) {
            sounds += GameSound.GameOver
            haptics += GameHaptic.Warning
        }
        if (GameEvent.Revived in events) {
            sounds += GameSound.Combo
            haptics += GameHaptic.Success
        }
        if (GameEvent.Restarted in events) {
            sounds += GameSound.Restart
            haptics += GameHaptic.Success
        }

        return InteractionFeedback(
            sounds = sounds,
            haptics = haptics,
        )
    }
}


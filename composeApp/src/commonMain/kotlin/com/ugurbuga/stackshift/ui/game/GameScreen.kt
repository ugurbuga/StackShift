package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ugurbuga.stackshift.StackShiftTheme
import com.ugurbuga.stackshift.ads.GameAdController
import com.ugurbuga.stackshift.ads.NoOpGameAdController
import com.ugurbuga.stackshift.game.model.BlockVisualStyle
import com.ugurbuga.stackshift.game.model.BoardMatrix
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.ComboState
import com.ugurbuga.stackshift.game.model.FeedbackEmphasis
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GameStatus
import com.ugurbuga.stackshift.game.model.GameText
import com.ugurbuga.stackshift.game.model.GameTextKey
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.PieceKind
import com.ugurbuga.stackshift.game.model.PlacementPreview
import com.ugurbuga.stackshift.game.model.PressureLevel
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.game.model.gameText
import com.ugurbuga.stackshift.localization.LocalAppSettings
import com.ugurbuga.stackshift.platform.feedback.GameHaptic
import com.ugurbuga.stackshift.platform.feedback.GameHaptics
import com.ugurbuga.stackshift.platform.feedback.GameSound
import com.ugurbuga.stackshift.platform.feedback.NoOpGameHaptics
import com.ugurbuga.stackshift.platform.feedback.NoOpSoundEffectPlayer
import com.ugurbuga.stackshift.platform.feedback.SoundEffectPlayer
import com.ugurbuga.stackshift.presentation.game.GameViewModel
import com.ugurbuga.stackshift.presentation.game.InteractionFeedback
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.HighScoreStorage
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.telemetry.LogScreen
import com.ugurbuga.stackshift.telemetry.NoOpAppTelemetry
import com.ugurbuga.stackshift.telemetry.TelemetryActionNames
import com.ugurbuga.stackshift.telemetry.TelemetryScreenNames
import com.ugurbuga.stackshift.ui.theme.StackShiftThemeTokens
import com.ugurbuga.stackshift.ui.theme.appBackgroundBrush
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.app_title
import stackshift.composeapp.generated.resources.block_properties_column_clearer_desc
import stackshift.composeapp.generated.resources.block_properties_ghost_desc
import stackshift.composeapp.generated.resources.block_properties_heavy_desc
import stackshift.composeapp.generated.resources.block_properties_row_clearer_desc
import stackshift.composeapp.generated.resources.block_properties_title
import stackshift.composeapp.generated.resources.boost
import stackshift.composeapp.generated.resources.continue_label
import stackshift.composeapp.generated.resources.danger
import stackshift.composeapp.generated.resources.danger_none
import stackshift.composeapp.generated.resources.feedback_chain
import stackshift.composeapp.generated.resources.feedback_clear
import stackshift.composeapp.generated.resources.feedback_extra_life
import stackshift.composeapp.generated.resources.feedback_hold_armed
import stackshift.composeapp.generated.resources.feedback_micro_adjust
import stackshift.composeapp.generated.resources.feedback_overflow
import stackshift.composeapp.generated.resources.feedback_perfect
import stackshift.composeapp.generated.resources.feedback_perfect_lane
import stackshift.composeapp.generated.resources.feedback_score_only
import stackshift.composeapp.generated.resources.feedback_soft_lock
import stackshift.composeapp.generated.resources.feedback_special
import stackshift.composeapp.generated.resources.feedback_special_chain
import stackshift.composeapp.generated.resources.feedback_swap
import stackshift.composeapp.generated.resources.game_message_chain_lines
import stackshift.composeapp.generated.resources.game_message_extra_life_used
import stackshift.composeapp.generated.resources.game_message_good_shot
import stackshift.composeapp.generated.resources.game_message_hold_updated
import stackshift.composeapp.generated.resources.game_message_lines_cleared
import stackshift.composeapp.generated.resources.game_message_no_opening
import stackshift.composeapp.generated.resources.game_message_overflow
import stackshift.composeapp.generated.resources.game_message_paused
import stackshift.composeapp.generated.resources.game_message_perfect_drop
import stackshift.composeapp.generated.resources.game_message_pressure_game_over
import stackshift.composeapp.generated.resources.game_message_resumed
import stackshift.composeapp.generated.resources.game_message_select_column
import stackshift.composeapp.generated.resources.game_message_soft_lock
import stackshift.composeapp.generated.resources.game_message_special_chain_board
import stackshift.composeapp.generated.resources.game_message_special_lines
import stackshift.composeapp.generated.resources.game_message_special_triggered
import stackshift.composeapp.generated.resources.game_message_tempo_critical
import stackshift.composeapp.generated.resources.game_message_tempo_up
import stackshift.composeapp.generated.resources.game_over_extra_life
import stackshift.composeapp.generated.resources.game_over_extra_life_loading
import stackshift.composeapp.generated.resources.game_over_new_high_score
import stackshift.composeapp.generated.resources.game_over_title
import stackshift.composeapp.generated.resources.high_score
import stackshift.composeapp.generated.resources.high_score_new_record
import stackshift.composeapp.generated.resources.hold
import stackshift.composeapp.generated.resources.launch_bar
import stackshift.composeapp.generated.resources.launch_boost_active
import stackshift.composeapp.generated.resources.launch_chain_message
import stackshift.composeapp.generated.resources.launch_drag_hint
import stackshift.composeapp.generated.resources.launch_game_over
import stackshift.composeapp.generated.resources.launch_paused
import stackshift.composeapp.generated.resources.launch_soft_lock_message
import stackshift.composeapp.generated.resources.launch_special_chance
import stackshift.composeapp.generated.resources.lines
import stackshift.composeapp.generated.resources.pause
import stackshift.composeapp.generated.resources.pause_title
import stackshift.composeapp.generated.resources.piece_size_format
import stackshift.composeapp.generated.resources.play_again
import stackshift.composeapp.generated.resources.queue_empty
import stackshift.composeapp.generated.resources.queue_hold
import stackshift.composeapp.generated.resources.queue_next_short
import stackshift.composeapp.generated.resources.restart
import stackshift.composeapp.generated.resources.restart_cancel
import stackshift.composeapp.generated.resources.restart_confirm
import stackshift.composeapp.generated.resources.restart_confirm_body
import stackshift.composeapp.generated.resources.restart_confirm_title
import stackshift.composeapp.generated.resources.resume
import stackshift.composeapp.generated.resources.score
import stackshift.composeapp.generated.resources.settings_title
import stackshift.composeapp.generated.resources.special_column_clearer
import stackshift.composeapp.generated.resources.special_ghost
import stackshift.composeapp.generated.resources.special_heavy
import stackshift.composeapp.generated.resources.special_row_clearer
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin
import stackshift.composeapp.generated.resources.launch as launchString

private const val LaunchAnimationMillis = 140L
private const val EntryAnimationMillis = 70L
private const val NextPieceScale = 0.5f
private const val LaunchPreviewAlpha = 1f
private const val QueuePreviewAlpha = 1f

private val BottomDockHeight = 148.dp
private val TopBarVerticalPadding = 6.dp
private val TopBarRowSpacing = 4.dp
private val TopBarIconSpacing = 4.dp
private val TopBarMetricLaunchSpacing = 8.dp
private val TopBarActionIconSize = 16.dp
private val MetricChipHorizontalPadding = 16.dp
private val MetricChipVerticalPadding = 7.dp
private val RecordIndicatorIconSize = 12.dp
private val RecordIndicatorSideGap = 6.dp
private val TopBarStatusCardCornerRadius = 16.dp
private val TopBarActionBayPadding = 6.dp
private val TopBarActionBayCornerRadius = 18.dp
private val TopBarActionBaySurfaceAlpha = 0.82f
private const val TopBarMessagePanelAlpha = 0.72f
private const val MetricHighlightThreshold = 0.08f
private const val MetricHighlightPulseScale = 1.08f
private const val MetricHighlightPulseUpDurationMillis = 180
private const val MetricHighlightPulseDownDurationMillis = 520
private const val GameOverDialogRevealDurationMillis = 260

private val GameOverDialogWidth = 420.dp
private val GameOverDialogRevealOffsetDp = 12.dp
private val GameOverDialogCardPadding = 24.dp
private val GameOverDialogIconSize = 74.dp
private val GameOverDialogButtonSpacing = 12.dp
private const val TopBarPanelAlpha = 0.88f
private const val TopBarPanelStrokeAlpha = 0.72f
private const val TopBarPanelGlowAlpha = 0.14f
private const val DockPanelAlpha = 0.90f
private const val DockPanelStrokeAlpha = 0.74f
private const val DockPanelGlowAlpha = 0.12f
private const val MetricCardGlowAlpha = 0.12f
private const val GameOverPanelAlpha = 0.93f
private const val GameOverPanelStrokeAlpha = 0.80f
private val GameOverBoardOverlayTopAlpha = 0.14f
private val GameOverBoardOverlayBottomAlpha = 0.22f
private val GameOverBoardGlowAlpha = 0.24f
private const val GameOverBoardRowCoverAlpha = 0.92f
private const val GameOverBoardRowClearDurationMillis = 92
private const val GameOverBoardRowShakeAmplitudePx = 8f
private const val GameOverBoardRowBurstAlpha = 0.26f
private const val GameOverBoardRowBurstStrokeWidthPx = 2.4f
private const val GameOverBoardRowGlowHeightPx = 12f
private val TopBarActionBlockSize = 28.dp
private val TopBarActionBlockCornerRadius = 10.dp
private val TopBarActionBlockTones = listOf(
    CellTone.Cyan,
    CellTone.Gold,
    CellTone.Violet,
    CellTone.Lime,
    CellTone.Amber,
)

@Composable
private fun resolveActivePieceProperties(
    piece: Piece?,
): String {
    if (piece == null) return resolveGameText(gameText(GameTextKey.QueueEmpty))

    val size = stringResource(Res.string.piece_size_format, piece.width, piece.height)
    return buildList {
        add(piece.kind.name)
        add(size)
        if (piece.special != SpecialBlockType.None) {
            add(resolveGameText(piece.special.shortLabel()))
        }
    }.joinToString(separator = " • ")
}

@Composable
private fun resolveBlockDetail(
    piece: Piece?,
): String {
    val special = piece?.special ?: return resolveGameText(gameText(GameTextKey.QueueEmpty))
    return when (special) {
        SpecialBlockType.None -> ""
        SpecialBlockType.ColumnClearer -> stringResource(Res.string.block_properties_column_clearer_desc)
        SpecialBlockType.RowClearer -> stringResource(Res.string.block_properties_row_clearer_desc)
        SpecialBlockType.Ghost -> stringResource(Res.string.block_properties_ghost_desc)
        SpecialBlockType.Heavy -> stringResource(Res.string.block_properties_heavy_desc)
    }
}

@Composable
private fun GameOverBoardClearOverlay(
    progress: Float,
    rows: Int,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val colorScheme = MaterialTheme.colorScheme
    val clampedProgress = progress.coerceIn(0f, 1f)
    if (clampedProgress <= 0f) return

    val safeRows = rows.coerceAtLeast(1)

    Canvas(modifier = modifier) {
        val rowHeight = size.height / safeRows
        val rowWindow = 1f / safeRows

        for (clearIndex in 0 until safeRows) {
            val rowIndex = safeRows - 1 - clearIndex
            val rowTop = rowIndex * rowHeight
            val rowStartProgress = clearIndex * rowWindow
            val rowProgress = ((clampedProgress - rowStartProgress) / rowWindow).coerceIn(0f, 1f)
            if (rowProgress <= 0f) continue

            val rowWave = sin((rowProgress * PI).toDouble()).toFloat().coerceIn(-1f, 1f)
            val rowShakeDirection = if (clearIndex % 2 == 0) 1f else -1f
            val rowShakeX = rowShakeDirection * rowWave * GameOverBoardRowShakeAmplitudePx
            val rowRevealAlpha = rowProgress.coerceIn(0f, 1f)
            val rowBurstAlpha = (1f - rowProgress).coerceIn(0f, 1f) * GameOverBoardRowBurstAlpha

            drawRect(
                color = colorScheme.background.copy(alpha = (GameOverBoardRowCoverAlpha * rowRevealAlpha).coerceAtMost(1f)),
                topLeft = Offset(rowShakeX, rowTop),
                size = Size(size.width, rowHeight + 1f),
            )

            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.background.copy(alpha = (GameOverBoardOverlayTopAlpha + (rowRevealAlpha * 0.12f)).coerceAtMost(0.24f)),
                        uiColors.panel.copy(alpha = (0.22f + rowRevealAlpha * 0.30f).coerceAtMost(0.52f)),
                        colorScheme.background.copy(alpha = (GameOverBoardOverlayBottomAlpha * rowRevealAlpha).coerceAtMost(1f)),
                    ),
                ),
                topLeft = Offset(rowShakeX, rowTop),
                size = Size(size.width, rowHeight + 1f),
            )

            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = rowBurstAlpha),
                        uiColors.panelStroke.copy(alpha = (rowBurstAlpha * 0.80f).coerceAtMost(0.22f)),
                        Color.Transparent,
                    ),
                ),
                topLeft = Offset(rowShakeX, (rowTop - (rowHeight * 0.18f)).coerceAtLeast(0f)),
                size = Size(size.width, rowHeight + (rowHeight * 0.36f)),
            )
        }
    }
}

@Composable
private fun GameOverDialog(
    gameState: GameState,
    highestScore: Int,
    showNewHighScoreMessage: Boolean,
    revealProgress: Float,
    canUseExtraLife: Boolean,
    isExtraLifeLoading: Boolean,
    onPlayAgain: () -> Unit,
    onUseExtraLife: () -> Unit,
) {
    val density = LocalDensity.current
    val revealOffsetPx = with(density) { GameOverDialogRevealOffsetDp.toPx() }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        GameOverDialogContent(
            gameState = gameState,
            highestScore = highestScore,
            showNewHighScoreMessage = showNewHighScoreMessage,
            revealProgress = revealProgress,
            canUseExtraLife = canUseExtraLife,
            isExtraLifeLoading = isExtraLifeLoading,
            onPlayAgain = onPlayAgain,
            onUseExtraLife = onUseExtraLife,
            modifier = Modifier
                .widthIn(max = GameOverDialogWidth)
                .graphicsLayer(
                    alpha = revealProgress,
                    scaleX = 0.90f + (0.10f * revealProgress),
                    scaleY = 0.90f + (0.10f * revealProgress),
                    translationY = (1f - revealProgress) * revealOffsetPx,
                ),
        )
    }
}

@Composable
private fun GameOverDialogContent(
    gameState: GameState,
    highestScore: Int,
    showNewHighScoreMessage: Boolean,
    revealProgress: Float,
    canUseExtraLife: Boolean,
    isExtraLifeLoading: Boolean,
    onPlayAgain: () -> Unit,
    onUseExtraLife: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val isNewRecord = showNewHighScoreMessage
    val recordPulsePhase = remember { Animatable(0f) }

    LaunchedEffect(isNewRecord) {
        if (!isNewRecord) {
            recordPulsePhase.snapTo(0f)
            return@LaunchedEffect
        }

        recordPulsePhase.snapTo(0f)
        while (isActive && isNewRecord) {
            recordPulsePhase.animateTo(
                1f,
                animationSpec = tween(durationMillis = MetricHighlightPulseUpDurationMillis, easing = FastOutSlowInEasing),
            )
            recordPulsePhase.animateTo(
                0f,
                animationSpec = tween(durationMillis = MetricHighlightPulseDownDurationMillis, easing = FastOutSlowInEasing),
            )
        }
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.panel),
        border = BorderStroke(1.dp, uiColors.panelStroke),
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.dialogStart,
                            uiColors.dialogEnd,
                        ),
                    ),
                )
                .padding(GameOverDialogCardPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(GameOverDialogIconSize)
                    .clip(RoundedCornerShape(18.dp))
                    .graphicsLayer(
                        scaleX = if (isNewRecord) 1f + ((MetricHighlightPulseScale - 1f) * 0.28f * recordPulsePhase.value) else 1f,
                        scaleY = if (isNewRecord) 1f + ((MetricHighlightPulseScale - 1f) * 0.28f * recordPulsePhase.value) else 1f,
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isNewRecord) uiColors.success.copy(alpha = 0.94f) else uiColors.warning.copy(alpha = 0.92f),
                                if (isNewRecord) uiColors.success.copy(alpha = 0.70f) else uiColors.danger.copy(alpha = 0.85f),
                                uiColors.panel.copy(alpha = 0.12f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = resolveGameText(gameText(GameTextKey.GameOverTitle)),
                    tint = Color.White,
                    modifier = Modifier.size(36.dp),
                )
            }

            Column(
                modifier = Modifier.graphicsLayer(
                    scaleX = if (isNewRecord) 1f + ((MetricHighlightPulseScale - 1f) * 0.10f * recordPulsePhase.value) else 1f,
                    scaleY = if (isNewRecord) 1f + ((MetricHighlightPulseScale - 1f) * 0.10f * recordPulsePhase.value) else 1f,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (isNewRecord) stringResource(Res.string.high_score_new_record) else resolveGameText(gameText(GameTextKey.GameOverTitle)),
                    style = MaterialTheme.typography.headlineSmall,
                    color = if (isNewRecord) uiColors.success else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (isNewRecord) resolveGameText(gameText(GameTextKey.GameOverNewHighScore)) else resolveGameText(gameState.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = uiColors.subtitle,
                    textAlign = TextAlign.Center,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GameOverDialogButtonSpacing),
            ) {
                GameOverStatChip(
                    title = if (isNewRecord) stringResource(Res.string.high_score_new_record) else resolveGameText(gameText(GameTextKey.HighScore)),
                    value = highestScore.toString(),
                    accentColor = if (isNewRecord) uiColors.success else null,
                    modifier = Modifier
                        .weight(1f)
                        .graphicsLayer(
                            scaleX = 1f + (MetricHighlightPulseScale - 1f) * recordPulsePhase.value,
                            scaleY = 1f + (MetricHighlightPulseScale - 1f) * recordPulsePhase.value,
                        ),
                )
                GameOverStatChip(
                    title = resolveGameText(gameText(GameTextKey.Score)),
                    value = gameState.score.toString(),
                    modifier = Modifier.weight(1f),
                )
                GameOverStatChip(
                    title = resolveGameText(gameText(GameTextKey.Lines)),
                    value = gameState.linesCleared.toString(),
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GameOverDialogButtonSpacing),
            ) {
                if (canUseExtraLife || isExtraLifeLoading) {
                    Button(
                        onClick = onUseExtraLife,
                        enabled = !isExtraLifeLoading,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = uiColors.success,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            if (isExtraLifeLoading) {
                                resolveGameText(gameText(GameTextKey.GameOverExtraLifeLoading))
                            } else {
                                resolveGameText(gameText(GameTextKey.GameOverExtraLife))
                            }
                        )
                    }
                }
                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = uiColors.actionButton,
                        contentColor = uiColors.actionIcon,
                    ),
                ) {
                    Text(resolveGameText(gameText(GameTextKey.PlayAgain)))
                }
            }
        }
    }
}

@Composable
private fun GameOverStatChip(
    title: String,
    value: String,
    accentColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val borderColor = accentColor?.copy(alpha = 0.92f) ?: uiColors.panelStroke.copy(alpha = 0.72f)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor ?: uiColors.subtitle,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = accentColor ?: MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
fun StackShiftGameApp(
    modifier: Modifier = Modifier,
    soundPlayer: SoundEffectPlayer = NoOpSoundEffectPlayer,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    adController: GameAdController = NoOpGameAdController,
    viewModel: GameViewModel = remember { GameViewModel() },
    onOpenSettings: () -> Unit = {},
) {
    val haptics = rememberGameHaptics()
    val uiState by viewModel.uiState.collectAsState()
    val uiColors = StackShiftThemeTokens.uiColors

    var highestScore by remember { mutableIntStateOf(HighScoreStorage.load()) }
    var newHighScoreReached by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.gameState.score) {
        if (uiState.gameState.score > highestScore) {
            telemetry.logHighScoreReached(
                newScore = uiState.gameState.score,
                previousHighScore = highestScore,
            )
            highestScore = uiState.gameState.score
            HighScoreStorage.save(highestScore)
            newHighScoreReached = true
        }
    }
    LaunchedEffect(
        uiState.gameState.status,
        uiState.gameState.score,
        uiState.gameState.linesCleared
    ) {
        if (uiState.gameState.status == GameStatus.Running && uiState.gameState.score == 0 && uiState.gameState.linesCleared == 0) {
            newHighScoreReached = false
        }
    }

    var showBlockProperties by remember { mutableStateOf(false) }

    if (showBlockProperties) {
        BlockPropertiesScreen(
            modifier = modifier,
            telemetry = telemetry,
            onBack = { showBlockProperties = false },
        )
    } else {
        LogScreen(telemetry, TelemetryScreenNames.Game)
        GameScreen(
            modifier = modifier,
            gameState = uiState.gameState,
            onRequestPreview = viewModel::previewPlacement,
            onResolvePreviewImpact = viewModel::previewImpactPoints,
            onPlacePiece = viewModel::placePiece,
            onHoldPiece = {
                telemetry.logUserAction(TelemetryActionNames.HoldPiece)
                viewModel.holdPiece()
            },
            onPauseToggle = {
                telemetry.logUserAction(TelemetryActionNames.TogglePause)
                viewModel.togglePause()
            },
            onRestart = {
                telemetry.logUserAction(TelemetryActionNames.RestartGame)
                viewModel.restart(uiState.gameState.config)
            },
            onOpenSettings = onOpenSettings,
            onBlockProperties = {
                telemetry.logUserAction(TelemetryActionNames.OpenBlockProperties)
                showBlockProperties = true
            },
            onRewardedRevive = {
                telemetry.logUserAction("rewarded_revive")
                viewModel.reviveFromReward()
            },
            telemetry = telemetry,
            adController = adController,
            soundPlayer = soundPlayer,
            haptics = haptics,
            highestScore = highestScore,
            showNewHighScoreMessage = newHighScoreReached,
        )
    }
}

@Composable
fun GameScreen(
    gameState: GameState,
    onRequestPreview: (Int) -> PlacementPreview?,
    onResolvePreviewImpact: (PlacementPreview?) -> Set<GridPoint>,
    onPlacePiece: (Int) -> InteractionFeedback,
    onHoldPiece: () -> InteractionFeedback,
    onPauseToggle: () -> InteractionFeedback,
    onRestart: () -> InteractionFeedback,
    onRewardedRevive: () -> InteractionFeedback,
    onOpenSettings: () -> Unit,
    onBlockProperties: () -> Unit,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    adController: GameAdController = NoOpGameAdController,
    soundPlayer: SoundEffectPlayer,
    haptics: GameHaptics,
    highestScore: Int,
    showNewHighScoreMessage: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    val colorScheme = MaterialTheme.colorScheme
    val uiColors = StackShiftThemeTokens.uiColors
    val updatedPreviewProvider by rememberUpdatedState(onRequestPreview)
    val updatedPreviewImpactProvider by rememberUpdatedState(onResolvePreviewImpact)
    val updatedPlacePiece by rememberUpdatedState(onPlacePiece)
    val updatedHoldPiece by rememberUpdatedState(onHoldPiece)
    val updatedPauseToggle by rememberUpdatedState(onPauseToggle)
    val updatedRestart by rememberUpdatedState(onRestart)
    val updatedRewardedRevive by rememberUpdatedState(onRewardedRevive)
    var showRestartDialog by remember { mutableStateOf(false) }
    val screenShakeX = remember { Animatable(0f) }
    val screenShakeY = remember { Animatable(0f) }
    val impactFlashAlpha = remember { Animatable(0f) }
    val comboDriftY = remember { Animatable(18f) }
    val comboAlpha = remember { Animatable(0f) }
    val metricPulsePhase = remember { Animatable(0f) }
    val gameOverBoardClearProgress = remember { Animatable(0f) }
    val gameOverDialogRevealProgress = remember { Animatable(0f) }
    var highScoreHighlightActive by remember { mutableStateOf(false) }
    var celebratedHighScore by remember { mutableIntStateOf(highestScore) }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var rewardedReviveLoading by remember { mutableStateOf(false) }

    var overlayHostRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var boardRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var trayRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var overlayTopLeft by remember { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var isLaunching by remember { mutableStateOf(false) }

    val activePiece = gameState.activePiece
    val boardRect by remember(boardRectInRoot, overlayHostRectInRoot) {
        derivedStateOf { boardRectInRoot.toLocalRect(overlayHostRectInRoot) }
    }
    val trayRect by remember(trayRectInRoot, overlayHostRectInRoot) {
        derivedStateOf { trayRectInRoot.toLocalRect(overlayHostRectInRoot) }
    }
    val cellSizePx by remember(boardRect, gameState.config) {
        derivedStateOf {
            if (boardRect.width == 0f) 0f else boardRect.width / gameState.config.columns
        }
    }
    val spawnColumn by remember(
        activePiece?.id,
        gameState.lastPlacementColumn,
        gameState.config.columns
    ) {
        derivedStateOf {
            resolveSpawnColumn(
                piece = activePiece,
                boardColumns = gameState.config.columns,
                lastPlacementColumn = gameState.lastPlacementColumn,
            )
        }
    }
    val spawnTopLeft by remember(activePiece?.id, trayRect, boardRect, cellSizePx, spawnColumn) {
        derivedStateOf {
            gameState.softLock?.preview?.landingAnchor?.toTopLeft(
                boardRect = boardRect,
                cellSizePx = cellSizePx
            )
                ?: pieceSpawnTopLeft(
                    piece = activePiece,
                    trayRect = trayRect,
                    boardRect = boardRect,
                    cellSizePx = cellSizePx,
                    column = spawnColumn,
                )
        }
    }

    LaunchedEffect(activePiece?.id, spawnTopLeft) {
        if (spawnTopLeft != null) {
            overlayTopLeft = spawnTopLeft
            isDragging = false
            isLaunching = false
        }
    }

    val selectedColumn by remember(activePiece?.id, overlayTopLeft, boardRect, cellSizePx) {
        derivedStateOf {
            resolveSelectedColumn(
                piece = activePiece,
                overlayTopLeft = overlayTopLeft,
                boardRect = boardRect,
                cellSizePx = cellSizePx,
                boardColumns = gameState.config.columns,
            )
        }
    }

    val placementPreview by remember(
        selectedColumn,
        activePiece?.id,
        gameState.status,
        gameState.board
    ) {
        derivedStateOf {
            if (gameState.status != GameStatus.Running) return@derivedStateOf null
            if (gameState.softLock != null && !isDragging) {
                gameState.softLock.preview
            } else {
                selectedColumn?.let(updatedPreviewProvider)
            }
        }
    }

    val scoreHighlightActive = highScoreHighlightActive
    val anyMetricHighlightActive = highScoreHighlightActive

    LaunchedEffect(anyMetricHighlightActive, gameState.status) {
        if (!anyMetricHighlightActive || gameState.status == GameStatus.GameOver) {
            metricPulsePhase.snapTo(0f)
            return@LaunchedEffect
        }

        metricPulsePhase.snapTo(0f)
        while (isActive && anyMetricHighlightActive && gameState.status != GameStatus.GameOver) {
            metricPulsePhase.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = MetricHighlightPulseUpDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
            metricPulsePhase.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = MetricHighlightPulseDownDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            )
        }
    }

    val metricPulsePhaseValue = metricPulsePhase.value
    val highScorePulseScale = if (highScoreHighlightActive) {
        1f + (MetricHighlightPulseScale - 1f) * metricPulsePhaseValue
    } else {
        1f
    }
    val scorePulseScale = if (scoreHighlightActive) {
        1f + (MetricHighlightPulseScale - 1f) * metricPulsePhaseValue
    } else {
        1f
    }

    val previewImpactPoints by remember(
        placementPreview,
        activePiece?.id,
        gameState.board,
        gameState.status
    ) {
        derivedStateOf {
            if (gameState.status != GameStatus.Running || placementPreview == null) {
                emptySet()
            } else {
                updatedPreviewImpactProvider(placementPreview)
            }
        }
    }

    val displayOverlayTopLeft by remember(
        activePiece?.id,
        overlayTopLeft,
        selectedColumn,
        boardRect,
        cellSizePx,
        isLaunching,
    ) {
        derivedStateOf {
            val current = overlayTopLeft ?: return@derivedStateOf null
            val snappedColumn = selectedColumn
            if (snappedColumn == null || boardRect == Rect.Zero || cellSizePx <= 0f || isLaunching) {
                return@derivedStateOf current
            }
            current.copy(x = columnToLeft(snappedColumn, boardRect, cellSizePx))
        }
    }

    val overlayX by animateFloatAsState(
        targetValue = displayOverlayTopLeft?.x ?: 0f,
        animationSpec = if (isDragging) snap() else tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "overlayX",
    )
    val overlayY by animateFloatAsState(
        targetValue = displayOverlayTopLeft?.y ?: 0f,
        animationSpec = if (isDragging) snap() else tween(
            durationMillis = 180,
            easing = FastOutSlowInEasing
        ),
        label = "overlayY",
    )
    val overlayScale by animateFloatAsState(
        targetValue = when {
            isLaunching -> 1.06f
            gameState.isSoftLockActive -> 0.98f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "overlayScale",
    )

    LaunchedEffect(gameState.screenShakeToken) {
        if (gameState.screenShakeToken == 0L) return@LaunchedEffect
        screenShakeX.snapTo(0f)
        screenShakeY.snapTo(0f)
        listOf(14f, -10f, 8f, -6f, 3f, 0f).forEachIndexed { index, value ->
            screenShakeX.animateTo(value, animationSpec = tween(durationMillis = 34))
            screenShakeY.animateTo(
                if (index % 2 == 0) 3f else -2f,
                animationSpec = tween(durationMillis = 34)
            )
        }
        screenShakeY.animateTo(0f, animationSpec = tween(durationMillis = 40))
    }

    LaunchedEffect(gameState.impactFlashToken) {
        if (gameState.impactFlashToken == 0L) return@LaunchedEffect
        impactFlashAlpha.snapTo(0.22f)
        impactFlashAlpha.animateTo(
            0f,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(gameState.comboPopupToken, gameState.floatingFeedback?.token) {
        if (gameState.floatingFeedback == null || gameState.comboPopupToken == 0L) return@LaunchedEffect
        comboDriftY.snapTo(26f)
        comboAlpha.snapTo(0f)
        comboAlpha.animateTo(1f, animationSpec = tween(durationMillis = 120))
        launch {
            comboDriftY.animateTo(
                -22f,
                animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing)
            )
        }
        comboAlpha.animateTo(
            0f,
            animationSpec = tween(durationMillis = 760, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(gameState.status, gameState.clearAnimationToken, gameState.recentlyClearedRows) {
        if (gameState.status != GameStatus.GameOver) {
            gameOverBoardClearProgress.snapTo(0f)
            gameOverDialogRevealProgress.snapTo(0f)
            showGameOverDialog = false
            rewardedReviveLoading = false
            return@LaunchedEffect
        }

        showGameOverDialog = false
        gameOverDialogRevealProgress.snapTo(0f)
        gameOverBoardClearProgress.snapTo(0f)
        val gameOverBoardClearDurationMillis = (GameOverBoardRowClearDurationMillis * gameState.config.rows).coerceAtLeast(GameOverBoardRowClearDurationMillis)
        val dialogRevealDelayMillis = ((gameOverBoardClearDurationMillis * 0.42f).roundToInt()).coerceAtLeast(0)
        showGameOverDialog = true
        launch {
            gameOverBoardClearProgress.animateTo(
                1f,
                animationSpec = tween(durationMillis = gameOverBoardClearDurationMillis, easing = FastOutSlowInEasing),
            )
        }
        delay(dialogRevealDelayMillis.toLong())
        gameOverDialogRevealProgress.animateTo(1f, animationSpec = tween(durationMillis = GameOverDialogRevealDurationMillis, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(highestScore, gameState.score, gameState.status, gameState.linesCleared) {
        when {
            gameState.status == GameStatus.GameOver ||
                (gameState.status == GameStatus.Running && gameState.score == 0 && gameState.linesCleared == 0) -> {
                highScoreHighlightActive = false
                celebratedHighScore = highestScore
                metricPulsePhase.animateTo(0f, animationSpec = snap())
            }

            highestScore > celebratedHighScore -> {
                celebratedHighScore = highestScore
                highScoreHighlightActive = true
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush(uiColors))
                .graphicsLayer(
                    translationX = screenShakeX.value,
                    translationY = screenShakeY.value,
                )
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coordinates ->
                        overlayHostRectInRoot = coordinates.boundsInRoot()
                    },
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MinimalTopBar(
                        gameState = gameState,
                        highestScore = highestScore,
                        highScoreHighlightStrength = if (highScoreHighlightActive) 1f else 0f,
                        highScoreHighlightScale = highScorePulseScale,
                        scoreHighlightStrength = if (scoreHighlightActive) 1f else 0f,
                        scoreHighlightScale = scorePulseScale,
                        onHoldPiece = {
                            dispatchFeedback(
                                updatedHoldPiece(),
                                soundPlayer,
                                haptics
                            )
                        },
                        onPauseToggle = {
                            dispatchFeedback(
                                updatedPauseToggle(),
                                soundPlayer,
                                haptics
                            )
                        },
                        onRestart = { showRestartDialog = true },
                        onOpenSettings = onOpenSettings,
                        onBlockProperties = onBlockProperties,
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        val boardRatio = gameState.config.columns / gameState.config.rows.toFloat()
                        val boardWidth = when {
                            maxWidth == 0.dp || maxHeight == 0.dp -> 0.dp
                            maxWidth / maxHeight > boardRatio -> maxHeight * boardRatio
                            else -> maxWidth
                        }
                        val boardHeight = if (boardRatio > 0f) boardWidth / boardRatio else 0.dp

                        if (boardWidth > 0.dp && boardHeight > 0.dp) {
                            BoardGrid(
                                modifier = Modifier
                                    .size(width = boardWidth, height = boardHeight)
                                    .onGloballyPositioned { coordinates ->
                                        boardRectInRoot = coordinates.boundsInRoot()
                                    },
                                gameState = gameState,
                                preview = placementPreview,
                                impactedPreviewCells = previewImpactPoints,
                                activeColumn = selectedColumn,
                                activePiece = activePiece,
                                isColumnValid = placementPreview != null,
                                isDragging = isDragging,
                                gameOverClearProgress = gameOverBoardClearProgress.value,
                            )

                            if (gameState.status == GameStatus.GameOver || gameOverBoardClearProgress.value > 0f) {
                                GameOverBoardClearOverlay(
                                    progress = gameOverBoardClearProgress.value,
                                    rows = gameState.config.rows,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }

                    MinimalBottomDock(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(BottomDockHeight)
                            .onGloballyPositioned { coordinates ->
                                trayRectInRoot = coordinates.boundsInRoot()
                            },
                        gameState = gameState,
                        cellSizePx = cellSizePx,
                    )
                }

                gameState.floatingFeedback?.let { floatingFeedback ->
                    FloatingFeedbackBubble(
                        text = resolveGameText(floatingFeedback.text),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 96.dp),
                        isBonus = floatingFeedback.emphasis != FeedbackEmphasis.Info,
                        alpha = comboAlpha.value,
                        driftY = comboDriftY.value,
                    )
                }

                if (impactFlashAlpha.value > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = impactFlashAlpha.value)),
                    )
                }

                if (activePiece != null && overlayTopLeft != null && cellSizePx > 0f) {
                    val pieceCellDp = with(density) { cellSizePx.toDp() }
                    val resolvedPreviewStyle = resolveBoardBlockStyle(
                        selectedStyle = LocalAppSettings.current.blockVisualStyle,
                        mode = LocalAppSettings.current.boardBlockStyleMode,
                    )
                    val launchCellCornerRadius = boardCellCornerRadiusDp(
                        cellSize = pieceCellDp,
                        style = resolvedPreviewStyle,
                    )
                    PieceBlocks(
                        piece = activePiece,
                        cellSize = pieceCellDp,
                        cellCornerRadius = launchCellCornerRadius,
                        modifier = Modifier
                            .graphicsLayer(
                                translationX = overlayX,
                                translationY = overlayY,
                                scaleX = overlayScale,
                                scaleY = overlayScale,
                                transformOrigin = TransformOrigin(0f, 0f),
                            )
                            .pointerInput(
                                activePiece.id,
                                gameState.status,
                                isLaunching,
                                boardRect,
                                cellSizePx,
                            ) {
                                detectDragGestures(
                                    onDragStart = {
                                        if (gameState.status != GameStatus.Running || isLaunching) return@detectDragGestures
                                        isDragging = true
                                        dispatchFeedback(
                                            InteractionFeedback(
                                                sounds = setOf(GameSound.Grab),
                                                haptics = setOf(GameHaptic.Light),
                                            ),
                                            soundPlayer = soundPlayer,
                                            haptics = haptics,
                                        )
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (gameState.status != GameStatus.Running || isLaunching) return@detectDragGestures
                                        change.consume()
                                        val current = overlayTopLeft ?: return@detectDragGestures
                                        overlayTopLeft = current.copy(x = current.x + dragAmount.x)
                                    },
                                    onDragEnd = {
                                        if (gameState.status != GameStatus.Running || isLaunching) return@detectDragGestures
                                        isDragging = false

                                        val preview = placementPreview
                                        val column = selectedColumn
                                        val currentSpawn = spawnTopLeft
                                        if (preview == null || column == null || currentSpawn == null) {
                                            overlayTopLeft = currentSpawn
                                            dispatchFeedback(
                                                InteractionFeedback(
                                                    sounds = setOf(GameSound.DropInvalid),
                                                    haptics = setOf(GameHaptic.Warning),
                                                ),
                                                soundPlayer = soundPlayer,
                                                haptics = haptics,
                                            )
                                            return@detectDragGestures
                                        }

                                        if (gameState.softLock != null) {
                                            overlayTopLeft = preview.landingAnchor.toTopLeft(
                                                boardRect = boardRect,
                                                cellSizePx = cellSizePx
                                            )
                                            val feedback = updatedPlacePiece(column)
                                            dispatchFeedback(feedback, soundPlayer, haptics)
                                        } else {
                                            isLaunching = true
                                            overlayTopLeft = preview.entryAnchor.toTopLeft(
                                                boardRect = boardRect,
                                                cellSizePx = cellSizePx
                                            )
                                            coroutineScope.launch {
                                                delay(EntryAnimationMillis)
                                                overlayTopLeft = preview.landingAnchor.toTopLeft(
                                                    boardRect = boardRect,
                                                    cellSizePx = cellSizePx
                                                )
                                                delay(LaunchAnimationMillis)
                                                val feedback = updatedPlacePiece(column)
                                                dispatchFeedback(feedback, soundPlayer, haptics)
                                                isLaunching = false
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        isDragging = false
                                        spawnTopLeft?.let { overlayTopLeft = it }
                                    },
                                )
                            },
                        alpha = when {
                            gameState.status == GameStatus.Paused -> 0.42f
                            isLaunching -> LaunchPreviewAlpha
                            else -> 1f
                        },
                    )
                }
            }

            if (showGameOverDialog) {
                GameOverDialog(
                    gameState = gameState,
                    highestScore = highestScore,
                    showNewHighScoreMessage = showNewHighScoreMessage,
                    revealProgress = gameOverDialogRevealProgress.value,
                    canUseExtraLife = !gameState.rewardedReviveUsed,
                    isExtraLifeLoading = rewardedReviveLoading,
                    onPlayAgain = {
                        telemetry.logUserAction(TelemetryActionNames.PlayAgain)
                        adController.showRestartInterstitial {
                            dispatchFeedback(updatedRestart(), soundPlayer, haptics)
                        }
                    },
                    onUseExtraLife = {
                        if (rewardedReviveLoading || gameState.rewardedReviveUsed) return@GameOverDialog
                        rewardedReviveLoading = true
                        adController.showRewardedRevive { rewarded ->
                            rewardedReviveLoading = false
                            if (rewarded) {
                                dispatchFeedback(updatedRewardedRevive(), soundPlayer, haptics)
                            }
                        }
                    },
                )
            } else if (gameState.status == GameStatus.Paused) {
                PauseOverlay(
                    gameState = gameState,
                    showNewHighScoreMessage = showNewHighScoreMessage,
                    onPrimaryAction = {
                        telemetry.logUserAction(TelemetryActionNames.TogglePause)
                        dispatchFeedback(updatedPauseToggle(), soundPlayer, haptics)
                    },
                )
            }

            if (showRestartDialog) {
                AlertDialog(
                    onDismissRequest = { showRestartDialog = false },
                    title = {
                        Text(resolveGameText(gameText(GameTextKey.RestartConfirmTitle)))
                    },
                    text = {
                        Text(resolveGameText(gameText(GameTextKey.RestartConfirmBody)))
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showRestartDialog = false
                                telemetry.logUserAction(TelemetryActionNames.RestartGame)
                                adController.showRestartInterstitial {
                                    dispatchFeedback(updatedRestart(), soundPlayer, haptics)
                                }
                            },
                        ) {
                            Text(resolveGameText(gameText(GameTextKey.RestartConfirm)))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRestartDialog = false }) {
                            Text(resolveGameText(gameText(GameTextKey.RestartCancel)))
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun MinimalTopBar(
    gameState: GameState,
    highestScore: Int,
    highScoreHighlightStrength: Float,
    highScoreHighlightScale: Float,
    scoreHighlightStrength: Float,
    scoreHighlightScale: Float,
    onHoldPiece: () -> Unit,
    onPauseToggle: () -> Unit,
    onRestart: () -> Unit,
    onOpenSettings: () -> Unit,
    onBlockProperties: () -> Unit,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val isNewRecordHighlight = highScoreHighlightStrength > 0.08f
    val hudStatusLabel = resolveGameText(gameText(GameTextKey.AppTitle))
    val hudStatusTone = when (gameState.status) {
        GameStatus.Running -> CellTone.Cyan
        GameStatus.Paused -> CellTone.Gold
        GameStatus.GameOver -> CellTone.Coral
    }
    val holdLabel = resolveGameText(gameText(GameTextKey.Hold))
    val pauseLabel =
        resolveGameText(gameText(if (gameState.status == GameStatus.Paused) GameTextKey.Resume else GameTextKey.Pause))
    val restartLabel = resolveGameText(gameText(GameTextKey.Restart))
    val dockMessage = resolveActivePieceProperties(piece = gameState.activePiece)
    val dockDetail = resolveBlockDetail(piece = gameState.activePiece)
    val dockMessageColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
    val dockDetailColor = uiColors.subtitle
    val panelStrokeColor = uiColors.panelStroke.copy(alpha = TopBarPanelStrokeAlpha)
    val panelGlow = Brush.verticalGradient(
        colors = listOf(
            uiColors.panelHighlight.copy(alpha = TopBarPanelGlowAlpha),
            uiColors.launchGlow.copy(alpha = 0.10f),
            Color.Transparent,
        ),
    )
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = TopBarPanelAlpha)),
        border = BorderStroke(1.dp, panelStrokeColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(panelGlow)
                .padding(horizontal = 12.dp, vertical = TopBarVerticalPadding + 1.dp),
            verticalArrangement = Arrangement.spacedBy(TopBarRowSpacing),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(TopBarMetricLaunchSpacing),
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    HudStatusBadge(
                        label = hudStatusLabel,
                        tone = hudStatusTone,
                    )
                }
                TopBarActionBay {
                    TopBarActionBlockButton(
                        tone = TopBarActionBlockTones[0],
                        icon = Icons.Filled.Settings,
                        contentDescription = stringResource(Res.string.settings_title),
                        onClick = onOpenSettings,
                    )
                    TopBarActionBlockButton(
                        tone = TopBarActionBlockTones[1],
                        icon = Icons.Filled.ViewModule,
                        contentDescription = stringResource(Res.string.block_properties_title),
                        onClick = onBlockProperties,
                    )
                    TopBarActionBlockButton(
                        tone = TopBarActionBlockTones[2],
                        icon = Icons.Filled.SwapHoriz,
                        contentDescription = holdLabel,
                        onClick = onHoldPiece,
                        enabled = gameState.canHold && !gameState.isSoftLockActive,
                    )
                    TopBarActionBlockButton(
                        tone = TopBarActionBlockTones[3],
                        icon = if (gameState.status == GameStatus.Paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        contentDescription = pauseLabel,
                        onClick = onPauseToggle,
                    )
                    TopBarActionBlockButton(
                        tone = TopBarActionBlockTones[4],
                        icon = Icons.Filled.Refresh,
                        contentDescription = restartLabel,
                        onClick = onRestart,
                    )
                }
            }

            MetricLaunchRow(
                highScoreTitle = if (isNewRecordHighlight) {
                    stringResource(Res.string.high_score_new_record)
                } else {
                    resolveGameText(gameText(GameTextKey.HighScore))
                },
                highScoreValue = highestScore.toString(),
                scoreTitle = resolveGameText(gameText(GameTextKey.Score)),
                scoreValue = gameState.score.toString(),
                highScoreHighlightStrength = highScoreHighlightStrength,
                highScoreHighlightScale = highScoreHighlightScale,
                scoreHighlightStrength = scoreHighlightStrength,
                scoreHighlightScale = scoreHighlightScale,
                launchContent = {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.66f)),
                        border = BorderStroke(
                            1.dp,
                            uiColors.boardEmptyCellBorder.copy(alpha = 0.82f)
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            uiColors.launchGlow.copy(alpha = 0.14f),
                                            Color.Transparent,
                                        ),
                                    ),
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            LaunchBarView(gameState = gameState)

                            Text(
                                text = dockMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = dockMessageColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = dockDetail.ifBlank { " " },
                                style = MaterialTheme.typography.labelSmall,
                                color = dockDetailColor,
                                minLines = 2,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun HudStatusBadge(
    label: String,
    tone: CellTone,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val settings = LocalAppSettings.current
    val resolvedBlockStyle = resolveBoardBlockStyle(
        selectedStyle = settings.blockVisualStyle,
        mode = settings.boardBlockStyleMode,
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(TopBarStatusCardCornerRadius),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.80f)),
        border = BorderStroke(1.dp, uiColors.boardEmptyCellBorder.copy(alpha = 0.90f)),
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            uiColors.boardSignaturePrimary.copy(alpha = 0.18f),
                            Color.Transparent,
                            uiColors.launchGlow.copy(alpha = 0.12f),
                        ),
                    ),
                )
                .padding(start = 5.dp, end = 10.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StackShiftAppIconBadge(
                tone = tone,
                settings = settings,
                blockStyle = resolvedBlockStyle,
            )

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StackShiftAppIconBadge(
    tone: CellTone,
    settings: AppSettings,
    blockStyle: BlockVisualStyle,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val accentColor = when (tone) {
        CellTone.Cyan, CellTone.Blue -> uiColors.boardSignaturePrimary
        CellTone.Gold, CellTone.Amber -> uiColors.boardSignatureSecondary
        CellTone.Coral -> uiColors.launchGlow
        else -> uiColors.panelHighlight
    }
    val badgeShape = RoundedCornerShape(10.dp)
    val boardShape = RoundedCornerShape(8.dp)
    val badgeSize = TopBarActionBlockSize + 4.dp
    val boardPadding = 3.dp
    val cellGap = 1.dp
    val cellSize = 4.4.dp

    Surface(
        modifier = modifier.size(badgeSize),
        shape = badgeShape,
        color = uiColors.gameSurface.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.48f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.boardSignaturePrimary.copy(alpha = 0.16f),
                            uiColors.launchGlow.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(boardPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .clip(boardShape)
                    .background(uiColors.gameSurface.copy(alpha = 0.96f))
                    .border(1.dp, uiColors.boardEmptyCellBorder.copy(alpha = 0.78f), boardShape)
                    .padding(2.dp),
                verticalArrangement = Arrangement.spacedBy(cellGap),
            ) {
                repeat(5) { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                        repeat(5) { column ->
                            val placedTone = stackShiftAppIconCellTone(row = row, column = column)
                            if (placedTone == null) {
                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .clip(RoundedCornerShape(1.8.dp))
                                        .background(uiColors.boardEmptyCell.copy(alpha = 0.88f))
                                        .border(
                                            width = 0.6.dp,
                                            color = uiColors.boardEmptyCellBorder.copy(alpha = 0.44f),
                                            shape = RoundedCornerShape(1.8.dp),
                                        ),
                                )
                            } else {
                                BlockCellPreview(
                                    tone = placedTone,
                                    palette = settings.blockColorPalette,
                                    style = blockStyle,
                                    size = cellSize,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun stackShiftAppIconCellTone(row: Int, column: Int): CellTone? = when (row to column) {
    0 to 0, 0 to 1, 1 to 1 -> CellTone.Cyan
    0 to 3, 0 to 4, 1 to 3 -> CellTone.Gold
    1 to 2, 2 to 2 -> CellTone.Violet
    2 to 0, 3 to 0 -> CellTone.Emerald
    2 to 1, 3 to 2, 3 to 3 -> CellTone.Lime
    2 to 4, 3 to 4, 4 to 3 -> CellTone.Amber
    4 to 0, 4 to 1 -> CellTone.Coral
    else -> null
}

@Composable
private fun TopBarActionBay(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(TopBarActionBayCornerRadius),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = TopBarActionBaySurfaceAlpha)),
        border = BorderStroke(1.dp, uiColors.boardEmptyCellBorder.copy(alpha = 0.9f)),
    ) {
        Row(
            modifier = Modifier
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            uiColors.launchGlow.copy(alpha = 0.10f),
                            Color.Transparent,
                            uiColors.boardGradientBottom.copy(alpha = 0.10f),
                        ),
                    ),
                )
                .padding(TopBarActionBayPadding),
            horizontalArrangement = Arrangement.spacedBy(TopBarIconSpacing),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}

@Composable
private fun MetricLaunchRow(
    highScoreTitle: String,
    highScoreValue: String,
    scoreTitle: String,
    scoreValue: String,
    highScoreHighlightStrength: Float,
    highScoreHighlightScale: Float,
    scoreHighlightStrength: Float,
    scoreHighlightScale: Float,
    launchContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = TopBarMetricLaunchSpacing
    SubcomposeLayout(modifier = modifier.fillMaxWidth()) { constraints ->
        val spacingPx = spacing.roundToPx()
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)

        val initialMetric = subcompose("metric-initial") {
            EqualWidthMetricColumn(
                highScoreTitle = highScoreTitle,
                highScoreValue = highScoreValue,
                scoreTitle = scoreTitle,
                scoreValue = scoreValue,
                highScoreHighlightStrength = highScoreHighlightStrength,
                highScoreHighlightScale = highScoreHighlightScale,
                scoreHighlightStrength = scoreHighlightStrength,
                scoreHighlightScale = scoreHighlightScale,
            )
        }.first().measure(looseConstraints)

        val metricWidth = initialMetric.width
            .coerceAtMost((constraints.maxWidth - spacingPx).coerceAtLeast(0))
        val launchWidth = (constraints.maxWidth - metricWidth - spacingPx).coerceAtLeast(0)

        val launchPlaceable = subcompose("launch") {
            Box(modifier = Modifier.fillMaxWidth()) {
                launchContent()
            }
        }.first().measure(
            constraints.copy(
                minWidth = launchWidth,
                maxWidth = launchWidth,
                minHeight = 0,
            )
        )

        val metricPlaceable = subcompose("metric") {
            EqualWidthMetricColumn(
                highScoreTitle = highScoreTitle,
                highScoreValue = highScoreValue,
                scoreTitle = scoreTitle,
                scoreValue = scoreValue,
                highScoreHighlightStrength = highScoreHighlightStrength,
                highScoreHighlightScale = highScoreHighlightScale,
                scoreHighlightStrength = scoreHighlightStrength,
                scoreHighlightScale = scoreHighlightScale,
            )
        }.first().measure(
            constraints.copy(
                minWidth = metricWidth,
                maxWidth = metricWidth,
                minHeight = launchPlaceable.height,
                maxHeight = launchPlaceable.height,
            )
        )

        val layoutHeight = maxOf(metricPlaceable.height, launchPlaceable.height)
        layout(constraints.maxWidth, layoutHeight) {
            metricPlaceable.placeRelative(0, 0)
            launchPlaceable.placeRelative(metricWidth + spacingPx, 0)
        }
    }
}

@Composable
private fun EqualWidthMetricColumn(
    highScoreTitle: String,
    highScoreValue: String,
    scoreTitle: String,
    scoreValue: String,
    highScoreHighlightStrength: Float,
    highScoreHighlightScale: Float,
    scoreHighlightStrength: Float,
    scoreHighlightScale: Float,
    modifier: Modifier = Modifier,
) {
    val spacing = 6.dp
    SubcomposeLayout(modifier = modifier) { constraints ->
        val looseConstraints = constraints.copy(minWidth = 0, minHeight = 0)
        val spacingPx = spacing.roundToPx()
        val horizontalPaddingPx = MetricChipHorizontalPadding.roundToPx() * 2
        val recordIndicatorsExtraWidth = ((RecordIndicatorIconSize * 2) + (RecordIndicatorSideGap * 2)).roundToPx()

        fun measureMetricTextWidth(
            slotId: String,
            title: String,
            value: String,
            includeRecordIndicators: Boolean,
        ): Int {
            val titlePlaceable = subcompose("$slotId-title") {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }.first().measure(looseConstraints)
            val valuePlaceable = subcompose("$slotId-value") {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }.first().measure(looseConstraints)
            return maxOf(titlePlaceable.width, valuePlaceable.width) + horizontalPaddingPx + if (includeRecordIndicators) recordIndicatorsExtraWidth else 0
        }

        val targetWidth = maxOf(
            measureMetricTextWidth(
                slotId = "initialHighScore",
                title = highScoreTitle,
                value = highScoreValue,
                includeRecordIndicators = highScoreHighlightStrength > MetricHighlightThreshold,
            ),
            measureMetricTextWidth(
                slotId = "initialScore",
                title = scoreTitle,
                value = scoreValue,
                includeRecordIndicators = false,
            ),
        ).coerceIn(constraints.minWidth, constraints.maxWidth)

        val availableHeight = constraints.maxHeight.takeIf { it != Int.MAX_VALUE }
        val targetHeightPerChip = availableHeight
            ?.let { ((it - spacingPx).coerceAtLeast(0)) / 2 }
            ?.coerceAtLeast(0)

        val chipConstraints = if (targetHeightPerChip != null) {
            constraints.copy(
                minWidth = targetWidth,
                maxWidth = targetWidth,
                minHeight = targetHeightPerChip,
                maxHeight = targetHeightPerChip,
            )
        } else {
            constraints.copy(
                minWidth = targetWidth,
                maxWidth = targetWidth,
                minHeight = 0,
            )
        }

        val highScorePlaceable = subcompose("highScore") {
            CompactMetricChip(
                title = highScoreTitle,
                value = highScoreValue,
                highlightStrength = highScoreHighlightStrength,
                scale = highScoreHighlightScale,
            )
        }.first().measure(chipConstraints)
        val scorePlaceable = subcompose("score") {
            CompactMetricChip(
                title = scoreTitle,
                value = scoreValue,
                highlightStrength = scoreHighlightStrength,
                scale = scoreHighlightScale,
            )
        }.first().measure(chipConstraints)

        val layoutHeight = availableHeight ?: (highScorePlaceable.height + spacingPx + scorePlaceable.height)

        layout(targetWidth, layoutHeight) {
            highScorePlaceable.placeRelative(0, 0)
            scorePlaceable.placeRelative(0, highScorePlaceable.height + spacingPx)
        }
    }
}

@Composable
private fun MinimalBottomDock(
    gameState: GameState,
    cellSizePx: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val queuePieces = gameState.nextQueue.take(1)
    val dockGlow = Brush.verticalGradient(
        colors = listOf(
            uiColors.launchGlow.copy(alpha = DockPanelGlowAlpha),
            Color.Transparent,
        ),
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = DockPanelAlpha)),
        border = BorderStroke(1.dp, uiColors.boardEmptyCellBorder.copy(alpha = DockPanelStrokeAlpha)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(dockGlow)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.boardGradientTop.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            HoldAndQueueStrip(
                queue = queuePieces,
                cellSizePx = cellSizePx,
            )
        }
    }
}

@Composable
private fun CompactMetricChip(
    title: String,
    value: String,
    highlightStrength: Float = 0f,
    scale: Float = 1f,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val highlightBorderColor = uiColors.success.copy(alpha = (0.34f + highlightStrength * 0.5f).coerceIn(0f, 0.88f))
    val cardGlow = if (highlightStrength > 0.02f) {
        Brush.verticalGradient(
            colors = listOf(
                uiColors.launchGlow.copy(alpha = MetricCardGlowAlpha * highlightStrength),
                Color.Transparent,
            ),
        )
    } else {
        null
    }
    Card(
        modifier = modifier.graphicsLayer(
            scaleX = scale,
            scaleY = scale,
        ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.92f)),
        border = BorderStroke(
            1.dp,
            if (highlightStrength > 0.02f) highlightBorderColor else uiColors.panelStroke.copy(alpha = 0.72f)
        ),
    ) {
        SubcomposeLayout(
            modifier = Modifier
                .fillMaxHeight()
                .then(
                    if (cardGlow != null) {
                        Modifier.background(cardGlow)
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (highlightStrength > 0.02f) {
                        Modifier.background(
                            Brush.linearGradient(
                                colors = listOf(
                                    uiColors.success.copy(alpha = 0.18f * highlightStrength),
                                    uiColors.launchGlow.copy(alpha = 0.14f * highlightStrength),
                                    Color.Transparent,
                                ),
                            ),
                        )
                    } else {
                        Modifier
                    },
                )
                .padding(horizontal = MetricChipHorizontalPadding, vertical = MetricChipVerticalPadding),
        ) { constraints ->
            val spacingPx = 1.dp.roundToPx()
            val leadingRecordIcon = if (highlightStrength > MetricHighlightThreshold) {
                subcompose("leadingRecordIcon") {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = uiColors.success,
                        modifier = Modifier
                            .size(RecordIndicatorIconSize)
                            .graphicsLayer(alpha = highlightStrength.coerceIn(0f, 1f)),
                    )
                }.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
            } else {
                null
            }
            val trailingRecordIcon = if (highlightStrength > MetricHighlightThreshold) {
                subcompose("trailingRecordIcon") {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = null,
                        tint = uiColors.success,
                        modifier = Modifier
                            .size(RecordIndicatorIconSize)
                            .graphicsLayer(alpha = highlightStrength.coerceIn(0f, 1f)),
                    )
                }.first().measure(constraints.copy(minWidth = 0, minHeight = 0))
            } else {
                null
            }

            val sideGapPx = if (leadingRecordIcon != null || trailingRecordIcon != null) {
                RecordIndicatorSideGap.roundToPx()
            } else {
                0
            }
            val leadingSlotWidth = (leadingRecordIcon?.width ?: 0) + if (leadingRecordIcon != null) sideGapPx else 0
            val trailingSlotWidth = (trailingRecordIcon?.width ?: 0) + if (trailingRecordIcon != null) sideGapPx else 0
            val textAreaWidth = (constraints.maxWidth - leadingSlotWidth - trailingSlotWidth).coerceAtLeast(0)
            val textConstraints = constraints.copy(
                minWidth = textAreaWidth,
                maxWidth = textAreaWidth,
                minHeight = 0,
            )
            val titlePlaceable = subcompose("title") {
                Text(
                    text = title,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelSmall,
                    color = uiColors.subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }.first().measure(textConstraints)

            val valuePlaceable = subcompose("value") {
                Text(
                    text = value,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                )
            }.first().measure(textConstraints)

            val width = if (constraints.minWidth == constraints.maxWidth) {
                constraints.maxWidth
            } else {
                (maxOf(titlePlaceable.width, valuePlaceable.width) + leadingSlotWidth + trailingSlotWidth)
                    .coerceIn(constraints.minWidth, constraints.maxWidth)
            }
            val contentHeight = titlePlaceable.height + spacingPx + valuePlaceable.height
            val height = if (constraints.minHeight == constraints.maxHeight) {
                constraints.maxHeight
            } else {
                contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
            }

            layout(width, height) {
                val contentTop = ((height - contentHeight) / 2).coerceAtLeast(0)
                titlePlaceable.placeRelative(
                    x = leadingSlotWidth,
                    y = contentTop,
                )
                valuePlaceable.placeRelative(
                    x = leadingSlotWidth,
                    y = contentTop + titlePlaceable.height + spacingPx,
                )
                leadingRecordIcon?.placeRelative(
                    x = 0,
                    y = ((height - leadingRecordIcon.height) / 2).coerceAtLeast(0),
                )
                trailingRecordIcon?.placeRelative(
                    x = (width - trailingRecordIcon.width).coerceAtLeast(0),
                    y = ((height - trailingRecordIcon.height) / 2).coerceAtLeast(0),
                )
            }
        }
    }
}

@Preview
@Composable
private fun GameScreenRunningRecordIconsPreview() {
    StackShiftTheme(settings = AppSettings()) {
        val gameState = previewGameState()
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MinimalTopBar(
                    gameState = gameState,
                    highestScore = 142000,
                    highScoreHighlightStrength = 1f,
                    highScoreHighlightScale = 1.03f,
                    scoreHighlightStrength = 1f,
                    scoreHighlightScale = 1.03f,
                    onHoldPiece = {},
                    onPauseToggle = {},
                    onRestart = {},
                    onOpenSettings = {},
                    onBlockProperties = {},
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Transparent),
                )
                MinimalBottomDock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(BottomDockHeight),
                    gameState = gameState,
                    cellSizePx = 24f,
                )
            }
        }
    }
}

@Composable
private fun PauseOverlay(
    gameState: GameState,
    showNewHighScoreMessage: Boolean,
    onPrimaryAction: () -> Unit,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val gameOverGlow = Brush.verticalGradient(
        colors = listOf(
            uiColors.dialogStart.copy(alpha = GameOverPanelAlpha),
            uiColors.dialogEnd.copy(alpha = GameOverPanelAlpha),
        ),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(uiColors.overlay),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = uiColors.panel.copy(alpha = GameOverPanelAlpha)),
            border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = GameOverPanelStrokeAlpha)),
        ) {
            Column(
                modifier = Modifier
                    .background(gameOverGlow)
                    .padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = resolveGameText(gameText(if (gameState.status == GameStatus.GameOver) GameTextKey.GameOverTitle else GameTextKey.PauseTitle)),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = resolveGameText(gameState.message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = uiColors.subtitle,
                    textAlign = TextAlign.Center,
                    minLines = 2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (gameState.status == GameStatus.GameOver && showNewHighScoreMessage) {
                    Text(
                        text = resolveGameText(gameText(GameTextKey.GameOverNewHighScore)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Button(
                    onClick = onPrimaryAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = uiColors.actionButton,
                        contentColor = uiColors.actionIcon,
                    ),
                ) {
                    Text(resolveGameText(gameText(if (gameState.status == GameStatus.GameOver) GameTextKey.PlayAgain else GameTextKey.Continue)))
                }
            }
        }
    }
}

@Composable
private fun MetricChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.92f)),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            uiColors.panelStroke.copy(alpha = 0.72f)
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = uiColors.subtitle,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun TopBarActionBlockButton(
    tone: CellTone,
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    val settings = LocalAppSettings.current
    val resolvedBlockStyle = resolveBoardBlockStyle(
        selectedStyle = settings.blockVisualStyle,
        mode = settings.boardBlockStyleMode,
    )
    val isDarkTheme = isStackShiftDarkTheme(settings)
    val iconColor = specialBlockIconTint(
        style = resolvedBlockStyle,
        isDarkTheme = isDarkTheme,
    ).copy(alpha = if (enabled) 1f else 0.58f)
    val buttonShape = RoundedCornerShape(TopBarActionBlockCornerRadius)

    Box(
        modifier = Modifier
            .size(TopBarActionBlockSize)
            .graphicsLayer(alpha = if (enabled) 1f else 0.72f)
            .clip(buttonShape)
            .clickable(enabled = enabled) { onClick.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        BlockCellPreview(
            tone = tone,
            palette = settings.blockColorPalette,
            style = resolvedBlockStyle,
            size = TopBarActionBlockSize,
            modifier = Modifier.size(TopBarActionBlockSize),
            alpha = if (enabled) 1f else 0.55f,
        )
        if (!enabled) {
            Box(
                modifier = Modifier
                    .size(TopBarActionBlockSize)
                    .clip(buttonShape)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.16f)),
            )
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconColor,
            modifier = Modifier.size(TopBarActionIconSize),
        )
    }
}

@Composable
private fun LaunchBarView(gameState: GameState) {
    val uiColors = StackShiftThemeTokens.uiColors
    val progressPercent = (gameState.launchBar.progress * 100).roundToInt()
    val isBoostActive = gameState.launchBar.boostTurnsRemaining > 0
    val animatedProgress by animateFloatAsState(
        targetValue = gameState.launchBar.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "launchBarProgress",
    )
    val boostGlow by animateFloatAsState(
        targetValue = if (gameState.launchBar.boostTurnsRemaining > 0) 1f else 0.35f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "launchBarGlow",
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(uiColors.launchTrack),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress.coerceAtLeast(if (animatedProgress > 0f) 0.08f else 0f))
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                uiColors.launchGlow.copy(alpha = boostGlow),
                                uiColors.success.copy(alpha = 0.92f),
                                uiColors.launchAccent,
                            ),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (isBoostActive) resolveGameText(gameText(GameTextKey.Boost)) else resolveGameText(
                        gameText(GameTextKey.Launch)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (isBoostActive) "x${gameState.launchBar.boostTurnsRemaining}" else "%$progressPercent",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
        }
        Text(
            text = if (isBoostActive) {
                resolveGameText(
                    gameText(
                        GameTextKey.LaunchBoostActive,
                        gameState.launchBar.boostTurnsRemaining
                    )
                )
            } else {
                resolveGameText(gameText(GameTextKey.LaunchSpecialChance, progressPercent))
            },
            style = MaterialTheme.typography.labelSmall,
            color = uiColors.subtitle,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HoldAndQueueStrip(
    queue: List<Piece>,
    cellSizePx: Float,
) {
    val density = LocalDensity.current
    val nextPiece = queue.firstOrNull()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QueueSlot(
            title = resolveGameText(gameText(GameTextKey.QueueNextShort)),
            piece = nextPiece,
            cellSizePx = cellSizePx * NextPieceScale,
            density = density,
        )
    }
}

@Composable
private fun QueueSlot(
    title: String,
    piece: Piece?,
    cellSizePx: Float,
    density: androidx.compose.ui.unit.Density,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val surfaceColor = uiColors.panel.copy(alpha = DockPanelAlpha)
    Card(
        modifier = Modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        border = BorderStroke(1.dp, uiColors.boardEmptyCellBorder.copy(alpha = DockPanelStrokeAlpha)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = uiColors.subtitle,
                )
            }
            if (piece != null) {
                val resolvedPreviewStyle = resolveBoardBlockStyle(
                    selectedStyle = LocalAppSettings.current.blockVisualStyle,
                    mode = LocalAppSettings.current.boardBlockStyleMode,
                )
                val queueCellCornerRadius = boardCellCornerRadiusDp(
                    cellSize = with(density) { (cellSizePx.coerceAtLeast(1f)).toDp() },
                    style = resolvedPreviewStyle,
                )
                PieceBlocks(
                    piece = piece,
                    cellSize = with(density) { (cellSizePx.coerceAtLeast(1f)).toDp() },
                    cellCornerRadius = queueCellCornerRadius,
                    alpha = QueuePreviewAlpha,
                )
            } else {
                Text(
                    resolveGameText(gameText(GameTextKey.QueueEmpty)),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            if (piece?.special != null && piece.special != SpecialBlockType.None) {
                Text(
                    text = resolveGameText(piece.special.shortLabel()),
                    style = MaterialTheme.typography.labelSmall,
                    color = uiColors.warning,
                )
            }
        }
    }
}

@Composable
private fun FloatingFeedbackBubble(
    text: String,
    modifier: Modifier = Modifier,
    isBonus: Boolean,
    alpha: Float,
    driftY: Float,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val pulse by animateFloatAsState(
        targetValue = if (isBonus) 1f else 0.92f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "feedbackPulse",
    )
    Card(
        modifier = modifier.graphicsLayer(
            scaleX = pulse,
            scaleY = pulse,
            alpha = alpha.coerceIn(0f, 1f),
            translationY = driftY,
        ),
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBonus) uiColors.panelHighlight.copy(alpha = 0.92f) else uiColors.panel.copy(alpha = 0.90f),
        ),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = if (isBonus) uiColors.success else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun resolveGameText(text: GameText): String {
    val resource = text.key.stringResourceId()
    return if (text.args.isEmpty()) {
        stringResource(resource)
    } else {
        stringResource(resource, *text.args.toTypedArray())
    }
}

private fun SpecialBlockType.shortLabel(): GameText = when (this) {
    SpecialBlockType.None -> gameText(GameTextKey.QueueEmpty)
    SpecialBlockType.ColumnClearer -> gameText(GameTextKey.SpecialColumnClearer)
    SpecialBlockType.RowClearer -> gameText(GameTextKey.SpecialRowClearer)
    SpecialBlockType.Ghost -> gameText(GameTextKey.SpecialGhost)
    SpecialBlockType.Heavy -> gameText(GameTextKey.SpecialHeavy)
}


private fun GameTextKey.stringResourceId(): StringResource = when (this) {
    GameTextKey.AppTitle -> Res.string.app_title
    GameTextKey.Hold -> Res.string.hold
    GameTextKey.Pause -> Res.string.pause
    GameTextKey.Resume -> Res.string.resume
    GameTextKey.Restart -> Res.string.restart
    GameTextKey.RestartConfirmTitle -> Res.string.restart_confirm_title
    GameTextKey.RestartConfirmBody -> Res.string.restart_confirm_body
    GameTextKey.RestartConfirm -> Res.string.restart_confirm
    GameTextKey.RestartCancel -> Res.string.restart_cancel
    GameTextKey.Score -> Res.string.score
    GameTextKey.HighScore -> Res.string.high_score
    GameTextKey.GameOverNewHighScore -> Res.string.game_over_new_high_score
    GameTextKey.Lines -> Res.string.lines
    GameTextKey.Boost -> Res.string.boost
    GameTextKey.Danger -> Res.string.danger
    GameTextKey.DangerNone -> Res.string.danger_none
    GameTextKey.Launch -> Res.string.launchString
    GameTextKey.LaunchBar -> Res.string.launch_bar
    GameTextKey.LaunchBoostActive -> Res.string.launch_boost_active
    GameTextKey.LaunchSpecialChance -> Res.string.launch_special_chance
    GameTextKey.LaunchSoftLockMessage -> Res.string.launch_soft_lock_message
    GameTextKey.LaunchChainMessage -> Res.string.launch_chain_message
    GameTextKey.LaunchPaused -> Res.string.launch_paused
    GameTextKey.LaunchGameOver -> Res.string.launch_game_over
    GameTextKey.LaunchDragHint -> Res.string.launch_drag_hint
    GameTextKey.QueueHold -> Res.string.queue_hold
    GameTextKey.QueueNextShort -> Res.string.queue_next_short
    GameTextKey.QueueEmpty -> Res.string.queue_empty
    GameTextKey.PauseTitle -> Res.string.pause_title
    GameTextKey.GameOverTitle -> Res.string.game_over_title
    GameTextKey.Continue -> Res.string.continue_label
    GameTextKey.GameOverExtraLife -> Res.string.game_over_extra_life
    GameTextKey.GameOverExtraLifeLoading -> Res.string.game_over_extra_life_loading
    GameTextKey.PlayAgain -> Res.string.play_again
    GameTextKey.GameMessageSelectColumn -> Res.string.game_message_select_column
    GameTextKey.GameMessageNoOpening -> Res.string.game_message_no_opening
    GameTextKey.GameMessageSoftLock -> Res.string.game_message_soft_lock
    GameTextKey.GameMessageOverflow -> Res.string.game_message_overflow
    GameTextKey.GameMessageSpecialChainBoard -> Res.string.game_message_special_chain_board
    GameTextKey.GameMessagePressureGameOver -> Res.string.game_message_pressure_game_over
    GameTextKey.GameMessageSpecialLines -> Res.string.game_message_special_lines
    GameTextKey.GameMessageSpecialTriggered -> Res.string.game_message_special_triggered
    GameTextKey.GameMessagePerfectDrop -> Res.string.game_message_perfect_drop
    GameTextKey.GameMessageChainLines -> Res.string.game_message_chain_lines
    GameTextKey.GameMessageLinesCleared -> Res.string.game_message_lines_cleared
    GameTextKey.GameMessageGoodShot -> Res.string.game_message_good_shot
    GameTextKey.GameMessageHoldUpdated -> Res.string.game_message_hold_updated
    GameTextKey.GameMessageTempoCritical -> Res.string.game_message_tempo_critical
    GameTextKey.GameMessageTempoUp -> Res.string.game_message_tempo_up
    GameTextKey.GameMessagePaused -> Res.string.game_message_paused
    GameTextKey.GameMessageResumed -> Res.string.game_message_resumed
    GameTextKey.GameMessageExtraLifeUsed -> Res.string.game_message_extra_life_used
    GameTextKey.FeedbackOverflow -> Res.string.feedback_overflow
    GameTextKey.FeedbackPerfectLane -> Res.string.feedback_perfect_lane
    GameTextKey.FeedbackMicroAdjust -> Res.string.feedback_micro_adjust
    GameTextKey.FeedbackSoftLock -> Res.string.feedback_soft_lock
    GameTextKey.FeedbackHoldArmed -> Res.string.feedback_hold_armed
    GameTextKey.FeedbackSwap -> Res.string.feedback_swap
    GameTextKey.FeedbackSpecialChain -> Res.string.feedback_special_chain
    GameTextKey.FeedbackSpecial -> Res.string.feedback_special
    GameTextKey.FeedbackPerfect -> Res.string.feedback_perfect
    GameTextKey.FeedbackChain -> Res.string.feedback_chain
    GameTextKey.FeedbackClear -> Res.string.feedback_clear
    GameTextKey.FeedbackScoreOnly -> Res.string.feedback_score_only
    GameTextKey.FeedbackExtraLife -> Res.string.feedback_extra_life
    GameTextKey.SpecialColumnClearer -> Res.string.special_column_clearer
    GameTextKey.SpecialRowClearer -> Res.string.special_row_clearer
    GameTextKey.SpecialGhost -> Res.string.special_ghost
    GameTextKey.SpecialHeavy -> Res.string.special_heavy
}

@Composable
private fun rememberGameHaptics(): GameHaptics {
    val hapticFeedback = LocalHapticFeedback.current
    return remember(hapticFeedback) {
        object : GameHaptics {
            override fun perform(effect: GameHaptic) {
                when (effect) {
                    GameHaptic.Light -> hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    GameHaptic.Success -> hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    GameHaptic.Warning -> hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
        }
    }
}

private fun dispatchFeedback(
    feedback: InteractionFeedback,
    soundPlayer: SoundEffectPlayer,
    haptics: GameHaptics,
) {
    feedback.sounds.forEach(soundPlayer::play)
    feedback.haptics.forEach(haptics::perform)
}

private fun resolveSelectedColumn(
    piece: Piece?,
    overlayTopLeft: Offset?,
    boardRect: Rect,
    cellSizePx: Float,
    boardColumns: Int,
): Int? {
    if (piece == null || overlayTopLeft == null || boardRect == Rect.Zero || cellSizePx <= 0f) return null
    val maxColumn = boardColumns - piece.width
    if (maxColumn < 0) return null
    val approximateColumn = ((overlayTopLeft.x - boardRect.left) / cellSizePx).roundToInt()
    return approximateColumn.coerceIn(0, maxColumn)
}

private fun pieceSpawnTopLeft(
    piece: Piece?,
    trayRect: Rect,
    boardRect: Rect,
    cellSizePx: Float,
    column: Int?,
): Offset? {
    if (piece == null || trayRect == Rect.Zero || boardRect == Rect.Zero || cellSizePx <= 0f || column == null) return null
    return Offset(
        x = columnToLeft(column, boardRect, cellSizePx),
        y = trayRect.center.y - (piece.height * cellSizePx) / 2f,
    )
}

private fun resolveSpawnColumn(
    piece: Piece?,
    boardColumns: Int,
    lastPlacementColumn: Int?,
): Int? {
    if (piece == null) return null
    val maxColumn = boardColumns - piece.width
    if (maxColumn < 0) return null
    return lastPlacementColumn?.coerceIn(0, maxColumn)
        ?: ((boardColumns - piece.width) / 2f).roundToInt().coerceIn(0, maxColumn)
}

private fun columnToLeft(
    column: Int,
    boardRect: Rect,
    cellSizePx: Float,
): Float = boardRect.left + (column * cellSizePx)

private fun Rect.toLocalRect(hostRect: Rect): Rect {
    if (this == Rect.Zero || hostRect == Rect.Zero) return Rect.Zero
    return Rect(
        left = left - hostRect.left,
        top = top - hostRect.top,
        right = right - hostRect.left,
        bottom = bottom - hostRect.top,
    )
}

private fun GridPoint.toTopLeft(
    boardRect: Rect,
    cellSizePx: Float,
): Offset = Offset(
    x = boardRect.left + (column * cellSizePx),
    y = boardRect.top + (row * cellSizePx),
)

@Preview
@Composable
private fun GameScreenRunningPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameScreen(
            gameState = previewGameState(),
            onRequestPreview = { previewPlacementPreview() },
            onResolvePreviewImpact = { previewImpactPointsPreview() },
            onPlacePiece = { InteractionFeedback.None },
            onHoldPiece = { InteractionFeedback.None },
            onPauseToggle = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onRewardedRevive = { InteractionFeedback.None },
            onOpenSettings = {},
            onBlockProperties = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 142000,
        )
    }
}

@Preview
@Composable
private fun GameOverDialogContentPreview() {
    StackShiftTheme(settings = AppSettings()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            GameOverDialogContent(
                gameState = previewGameState(status = GameStatus.GameOver),
                highestScore = 142000,
                showNewHighScoreMessage = false,
                revealProgress = 1f,
                canUseExtraLife = true,
                isExtraLifeLoading = false,
                onPlayAgain = {},
                onUseExtraLife = {},
                modifier = Modifier.widthIn(max = GameOverDialogWidth),
            )
        }
    }
}

@Preview
@Composable
private fun GameOverDialogContentNewRecordPreview() {
    StackShiftTheme(settings = AppSettings()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            GameOverDialogContent(
                gameState = previewGameState(status = GameStatus.GameOver).copy(score = 1520),
                highestScore = 1520,
                showNewHighScoreMessage = true,
                revealProgress = 1f,
                canUseExtraLife = true,
                isExtraLifeLoading = false,
                onPlayAgain = {},
                onUseExtraLife = {},
                modifier = Modifier.widthIn(max = GameOverDialogWidth),
            )
        }
    }
}

@Preview
@Composable
private fun GameScreenPausedPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameScreen(
            gameState = previewGameState(status = GameStatus.Paused),
            onRequestPreview = { previewPlacementPreview() },
            onResolvePreviewImpact = { emptySet() },
            onPlacePiece = { InteractionFeedback.None },
            onHoldPiece = { InteractionFeedback.None },
            onPauseToggle = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onRewardedRevive = { InteractionFeedback.None },
            onOpenSettings = {},
            onBlockProperties = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 142000,
        )
    }
}

@Preview
@Composable
private fun GameScreenGameOverPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameScreen(
            gameState = previewGameState(status = GameStatus.GameOver),
            onRequestPreview = { previewPlacementPreview() },
            onResolvePreviewImpact = { emptySet() },
            onPlacePiece = { InteractionFeedback.None },
            onHoldPiece = { InteractionFeedback.None },
            onPauseToggle = { InteractionFeedback.None },
            onRestart = { InteractionFeedback.None },
            onRewardedRevive = { InteractionFeedback.None },
            onOpenSettings = {},
            onBlockProperties = {},
            soundPlayer = NoOpSoundEffectPlayer,
            haptics = NoOpGameHaptics,
            highestScore = 142000,
        )
    }
}

private fun previewGameState(status: GameStatus = GameStatus.Running): GameState {
    val config = GameConfig(columns = 10, rows = 16)
    val board = BoardMatrix.empty(columns = 10, rows = 16)
        .fill(
            points = listOf(
                GridPoint(0, 15),
                GridPoint(0, 14),
                GridPoint(1, 15),
                GridPoint(1, 14),
                GridPoint(2, 15),
            ),
            tone = CellTone.Blue,
        )
        .fill(
            points = listOf(
                GridPoint(2, 12),
                GridPoint(2, 11),
                GridPoint(3, 12),
                GridPoint(4, 12),
                GridPoint(5, 11),
                GridPoint(6, 10),
            ),
            tone = CellTone.Emerald,
        )
        .fill(
            points = listOf(
                GridPoint(2, 9),
                GridPoint(6, 9),
                GridPoint(7, 8),
            ),
            tone = CellTone.Gold,
        )
        .fill(
            points = listOf(
                GridPoint(8, 14),
                GridPoint(8, 13),
                GridPoint(9, 14),
            ),
            tone = CellTone.Coral,
        )
    return GameState(
        config = config,
        board = board,
        activePiece = Piece(
            id = 1,
            kind = PieceKind.T,
            tone = CellTone.Gold,
            cells = listOf(
                GridPoint(0, 0),
                GridPoint(1, 0),
                GridPoint(2, 0),
                GridPoint(1, 1),
            ),
            width = 3,
            height = 2,
            special = SpecialBlockType.RowClearer,
        ),
        nextQueue = listOf(
            Piece(
                id = 2,
                kind = PieceKind.Domino,
                tone = CellTone.Cyan,
                cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
                width = 2,
                height = 1,
                special = SpecialBlockType.Ghost,
            ),
            Piece(
                id = 3,
                kind = PieceKind.Square,
                tone = CellTone.Violet,
                cells = listOf(GridPoint(0, 0), GridPoint(1, 0), GridPoint(0, 1), GridPoint(1, 1)),
                width = 2,
                height = 2,
                special = SpecialBlockType.None,
            ),
            Piece(
                id = 4,
                kind = PieceKind.I,
                tone = CellTone.Amber,
                cells = listOf(GridPoint(0, 0), GridPoint(1, 0), GridPoint(2, 0), GridPoint(3, 0)),
                width = 4,
                height = 1,
                special = SpecialBlockType.Heavy,
            ),
        ),
        holdPiece = Piece(
            id = 5,
            kind = PieceKind.Domino,
            tone = CellTone.Blue,
            cells = listOf(GridPoint(0, 0), GridPoint(1, 0)),
            width = 2,
            height = 1,
            special = SpecialBlockType.ColumnClearer,
        ),
        canHold = true,
        score = 1420,
        lastMoveScore = 280,
        linesCleared = 12,
        level = 3,
        difficultyStage = 1,
        secondsUntilDifficultyIncrease = 9,
        combo = ComboState(chain = 2, best = 4),
        perfectDropStreak = 2,
        launchBar = com.ugurbuga.stackshift.game.model.LaunchBarState(
            progress = 0.76f,
            boostTurnsRemaining = 1,
            lastGain = 0.22f
        ),
        columnPressure = (0 until 10).map { column ->
            com.ugurbuga.stackshift.game.model.ColumnPressure(
                column = column,
                filledCells = when (column) {
                    4, 5 -> 13
                    7 -> 10
                    else -> 4
                },
                fillRatio = when (column) {
                    4, 5 -> 0.84f
                    7 -> 0.64f
                    else -> 0.25f
                },
                level = when (column) {
                    4, 5 -> PressureLevel.Critical
                    7 -> PressureLevel.Warning
                    else -> PressureLevel.Calm
                },
            )
        },
        softLock = null,
        status = status,
        recentlyClearedRows = setOf(14),
        lastResolvedLines = 2,
        lastChainDepth = 2,
        specialChainCount = 1,
        clearAnimationToken = 1,
        screenShakeToken = 1L,
        impactFlashToken = 1L,
        comboPopupToken = 1L,
        floatingFeedback = com.ugurbuga.stackshift.game.model.FloatingFeedback(
            text = gameText(GameTextKey.FeedbackPerfect, 2, 280),
            emphasis = FeedbackEmphasis.Bonus,
            token = 1L,
        ),
        feedbackToken = 1L,
        message = when (status) {
            GameStatus.Running -> gameText(GameTextKey.GameMessageSelectColumn)
            GameStatus.Paused -> gameText(GameTextKey.GameMessagePaused)
            GameStatus.GameOver -> gameText(GameTextKey.GameMessagePressureGameOver)
        },
    )
}

private fun previewImpactPointsPreview(): Set<GridPoint> = setOf(
    GridPoint(2, 9),
    GridPoint(6, 9),
    GridPoint(7, 8),
)

private fun previewPlacementPreview(): PlacementPreview = PlacementPreview(
    selectedColumn = 4,
    entryAnchor = GridPoint(4, 14),
    landingAnchor = GridPoint(4, 8),
    occupiedCells = listOf(
        GridPoint(4, 8),
        GridPoint(5, 8),
        GridPoint(6, 8),
        GridPoint(5, 9),
    ),
    coveredColumns = 4..6,
)

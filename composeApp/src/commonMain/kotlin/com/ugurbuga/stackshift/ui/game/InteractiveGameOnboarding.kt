package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.StackShiftTheme
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.FirstRunOnboardingStage
import com.ugurbuga.stackshift.settings.FirstRunOnboardingTarget
import com.ugurbuga.stackshift.settings.FirstRunOnboardingScene
import com.ugurbuga.stackshift.settings.FirstRunGameOnboardingStateFactory
import com.ugurbuga.stackshift.ui.theme.StackShiftThemeTokens
import com.ugurbuga.stackshift.ui.theme.appBackgroundBrush
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.interactive_onboarding_aim_hint
import stackshift.composeapp.generated.resources.interactive_onboarding_column_clearer_body
import stackshift.composeapp.generated.resources.interactive_onboarding_column_clearer_title
import stackshift.composeapp.generated.resources.interactive_onboarding_drag_body
import stackshift.composeapp.generated.resources.interactive_onboarding_drag_ready
import stackshift.composeapp.generated.resources.interactive_onboarding_drag_title
import stackshift.composeapp.generated.resources.interactive_onboarding_drag_to_board
import stackshift.composeapp.generated.resources.interactive_onboarding_ghost_body
import stackshift.composeapp.generated.resources.interactive_onboarding_ghost_title
import stackshift.composeapp.generated.resources.interactive_onboarding_heavy_body
import stackshift.composeapp.generated.resources.interactive_onboarding_heavy_title
import stackshift.composeapp.generated.resources.interactive_onboarding_line_clear_body
import stackshift.composeapp.generated.resources.interactive_onboarding_line_clear_title
import stackshift.composeapp.generated.resources.interactive_onboarding_row_clearer_body
import stackshift.composeapp.generated.resources.interactive_onboarding_row_clearer_title
import stackshift.composeapp.generated.resources.interactive_onboarding_step_counter
import stackshift.composeapp.generated.resources.interactive_onboarding_target_locked
import stackshift.composeapp.generated.resources.interactive_onboarding_waiting

data class GameInteractiveOnboardingUi(
    val scene: FirstRunOnboardingScene,
    val currentStep: Int,
    val totalSteps: Int,
    val hasDraggedAwayFromSpawn: Boolean,
    val isTargetAligned: Boolean,
    val isAwaitingPlacementCommit: Boolean,
)

@Composable
fun InteractiveGameOnboardingOverlay(
    ui: GameInteractiveOnboardingUi,
    boardRect: Rect,
    trayRect: Rect,
    cellSizePx: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val targetPulseTransition = rememberInfiniteTransition(label = "interactiveOnboardingTargetPulse")
    val targetPulsePhase = targetPulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 980,
                easing = FastOutSlowInEasing,
            ),
        ),
        label = "interactiveOnboardingTargetPulsePhase",
    )
    val targetRect = onboardingTargetRect(
        scene = ui.scene,
        boardRect = boardRect,
        trayRect = trayRect,
        cellSizePx = cellSizePx,
    )
    val guideColumns = onboardingGuideColumns(ui.scene)
    val title = when (ui.scene.stage) {
        FirstRunOnboardingStage.DragAndLaunch -> stringResource(Res.string.interactive_onboarding_drag_title)
        FirstRunOnboardingStage.LineClear -> stringResource(Res.string.interactive_onboarding_line_clear_title)
        FirstRunOnboardingStage.ColumnClearer -> stringResource(Res.string.interactive_onboarding_column_clearer_title)
        FirstRunOnboardingStage.RowClearer -> stringResource(Res.string.interactive_onboarding_row_clearer_title)
        FirstRunOnboardingStage.Ghost -> stringResource(Res.string.interactive_onboarding_ghost_title)
        FirstRunOnboardingStage.Heavy -> stringResource(Res.string.interactive_onboarding_heavy_title)
    }
    val body = when (ui.scene.stage) {
        FirstRunOnboardingStage.DragAndLaunch -> stringResource(Res.string.interactive_onboarding_drag_body)
        FirstRunOnboardingStage.LineClear -> stringResource(Res.string.interactive_onboarding_line_clear_body)
        FirstRunOnboardingStage.ColumnClearer -> stringResource(Res.string.interactive_onboarding_column_clearer_body)
        FirstRunOnboardingStage.RowClearer -> stringResource(Res.string.interactive_onboarding_row_clearer_body)
        FirstRunOnboardingStage.Ghost -> stringResource(Res.string.interactive_onboarding_ghost_body)
        FirstRunOnboardingStage.Heavy -> stringResource(Res.string.interactive_onboarding_heavy_body)
    }
    val isDragStepReady = ui.scene.stage == FirstRunOnboardingStage.DragAndLaunch && ui.hasDraggedAwayFromSpawn
    val isLaterStepReady = ui.scene.stage != FirstRunOnboardingStage.DragAndLaunch && ui.isTargetAligned
    val isSuccessState = isDragStepReady || isLaterStepReady
    val hint = when {
        ui.isAwaitingPlacementCommit -> stringResource(Res.string.interactive_onboarding_waiting)
        isLaterStepReady -> stringResource(Res.string.interactive_onboarding_target_locked)
        isDragStepReady -> stringResource(Res.string.interactive_onboarding_drag_ready)
        ui.scene.stage == FirstRunOnboardingStage.DragAndLaunch -> stringResource(Res.string.interactive_onboarding_drag_to_board)
        else -> stringResource(Res.string.interactive_onboarding_aim_hint)
    }
    val guideColor = when {
        ui.isAwaitingPlacementCommit -> uiColors.warning
        isSuccessState -> uiColors.success
        else -> uiColors.actionButton
    }
    val hintContainerAlpha = when {
        ui.isAwaitingPlacementCommit -> 0.26f
        isSuccessState -> 0.20f
        else -> 0.18f
    }
    val hintBorderAlpha = when {
        ui.isAwaitingPlacementCommit -> 0.72f
        isSuccessState -> 0.58f
        else -> 0.48f
    }
    val guideBadgeTextColor = if (guideColor.luminance() > 0.58f) {
        Color(0xFF101114)
    } else {
        Color.White
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (
                ui.scene.target == FirstRunOnboardingTarget.Board &&
                boardRect != Rect.Zero &&
                cellSizePx > 0f &&
                guideColumns.isNotEmpty()
            ) {
                val highlightedColumns = (ui.scene.gameState.activePiece?.width ?: 1).coerceAtLeast(1)
                guideColumns.forEach { column ->
                    val laneLeft = boardRect.left + (column * cellSizePx)
                    val laneWidth = (highlightedColumns * cellSizePx).coerceAtMost(boardRect.right - laneLeft)
                    if (laneWidth <= 0f) return@forEach
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                guideColor.copy(alpha = 0.14f + (targetPulsePhase.value * 0.06f)),
                                guideColor.copy(alpha = 0.20f + (targetPulsePhase.value * 0.08f)),
                                uiColors.launchGlow.copy(alpha = 0.16f + (targetPulsePhase.value * 0.08f)),
                            ),
                        ),
                        topLeft = Offset(laneLeft, boardRect.top),
                        size = androidx.compose.ui.geometry.Size(laneWidth, boardRect.height),
                        cornerRadius = CornerRadius(22f, 22f),
                    )
                }
            }
            if (targetRect != null) {
                val pulsePhase = targetPulsePhase.value
                val pulseScale = when {
                    ui.isAwaitingPlacementCommit -> 1.005f
                    isSuccessState -> 1.008f + (pulsePhase * 0.015f)
                    else -> 1.015f + (pulsePhase * 0.025f)
                }
                val pulseRect = targetRect.scaleAroundCenter(pulseScale)
                val cornerRadius = CornerRadius(24f, 24f)
                drawRoundRect(
                    color = guideColor.copy(
                        alpha = when {
                            ui.isAwaitingPlacementCommit -> 0.14f
                            isSuccessState -> 0.16f
                            else -> 0.12f
                        }
                    ),
                    topLeft = Offset(targetRect.left, targetRect.top),
                    size = targetRect.size,
                    cornerRadius = cornerRadius,
                )
                drawRoundRect(
                    color = guideColor.copy(alpha = if (isSuccessState) 0.94f else 0.86f),
                    topLeft = Offset(targetRect.left, targetRect.top),
                    size = targetRect.size,
                    cornerRadius = cornerRadius,
                    style = Stroke(width = if (isSuccessState) 4.5f else 4f),
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = if (isSuccessState) 0.18f else 0.12f),
                    topLeft = Offset(targetRect.left + 2f, targetRect.top + 2f),
                    size = targetRect.size.copy(
                        width = targetRect.size.width - 4f,
                        height = targetRect.size.height - 4f,
                    ),
                    cornerRadius = CornerRadius(20f, 20f),
                    style = Stroke(width = 1.2f),
                )
                drawRoundRect(
                    color = guideColor.copy(
                        alpha = when {
                            ui.isAwaitingPlacementCommit -> 0.18f
                            isSuccessState -> 0.20f + (pulsePhase * 0.05f)
                            else -> 0.14f + (pulsePhase * 0.10f)
                        }
                    ),
                    topLeft = Offset(pulseRect.left, pulseRect.top),
                    size = pulseRect.size,
                    cornerRadius = CornerRadius(30f, 30f),
                    style = Stroke(width = if (isSuccessState) 7f else 6f),
                )
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.95f)),
            border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.82f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                uiColors.panelHighlight.copy(alpha = 0.18f),
                                uiColors.launchGlow.copy(alpha = 0.10f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = guideColor.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    ) {
                        Text(
                            text = stringResource(
                                Res.string.interactive_onboarding_step_counter,
                                ui.currentStep,
                                ui.totalSteps,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = guideBadgeTextColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                )
                InteractiveOnboardingScenePreview(
                    scene = ui.scene,
                    guideColor = guideColor,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = guideColor.copy(alpha = hintContainerAlpha),
                    border = BorderStroke(1.dp, guideColor.copy(alpha = hintBorderAlpha)),
                ) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveOnboardingScenePreview(
    scene: FirstRunOnboardingScene,
    guideColor: Color,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val settings = com.ugurbuga.stackshift.localization.LocalAppSettings.current
    val blockStyle = resolveBoardBlockStyle(settings.blockVisualStyle, settings.boardBlockStyleMode)
    val guideColumns = onboardingGuideColumns(scene)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(18.dp),
            color = uiColors.panelMuted.copy(alpha = 0.78f),
            border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.70f)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                scene.gameState.activePiece?.let { piece ->
                    PieceBlocks(
                        piece = piece,
                        cellSize = 14.dp,
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        repeat(scene.gameState.config.columns.coerceAtMost(10)) { column ->
                            val isGuided = guideColumns.any { guided ->
                                val activeWidth = (scene.gameState.activePiece?.width ?: 1).coerceAtLeast(1)
                                column in guided until (guided + activeWidth)
                            }
                            Box(
                                modifier = Modifier
                                    .size(width = 14.dp, height = 10.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (isGuided) {
                                            guideColor.copy(alpha = 0.92f)
                                        } else {
                                            uiColors.boardEmptyCell.copy(alpha = 0.52f)
                                        }
                                    ),
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val activePiece = scene.gameState.activePiece
                        BlockCellPreview(
                            tone = activePiece?.tone ?: CellTone.Blue,
                            palette = settings.blockColorPalette,
                            style = blockStyle,
                            size = 18.dp,
                            special = activePiece?.special ?: SpecialBlockType.None,
                        )
                        if (scene.target == FirstRunOnboardingTarget.Board) {
                            guideColumns.forEach { column ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = guideColor.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, guideColor.copy(alpha = 0.64f)),
                                ) {
                                    Text(
                                        text = "${column + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        } else {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 16.dp, height = 8.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(guideColor.copy(alpha = 0.42f)),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun onboardingTargetRect(
    scene: FirstRunOnboardingScene,
    boardRect: Rect,
    trayRect: Rect,
    cellSizePx: Float,
): Rect? = when (scene.target) {
    FirstRunOnboardingTarget.Tray -> trayRect.takeIf { it != Rect.Zero }?.expand(padding = 14f)
    FirstRunOnboardingTarget.Board -> {
        if (boardRect == Rect.Zero || cellSizePx <= 0f) return null
        val guideColumn = scene.guideColumn ?: return boardRect.expand(padding = 6f)
        val highlightedColumns = (scene.gameState.activePiece?.width ?: 1).coerceAtLeast(1)
        val left = boardRect.left + (guideColumn * cellSizePx)
        val right = (left + (highlightedColumns * cellSizePx)).coerceAtMost(boardRect.right)
        Rect(
            left = (left - (cellSizePx * 0.08f)).coerceAtLeast(boardRect.left),
            top = boardRect.top,
            right = (right + (cellSizePx * 0.08f)).coerceAtMost(boardRect.right),
            bottom = boardRect.bottom,
        ).expand(padding = 8f)
    }
}

private fun onboardingGuideColumns(scene: FirstRunOnboardingScene): List<Int> = when {
    scene.acceptedColumns.isNotEmpty() -> scene.acceptedColumns.sorted()
    scene.guideColumn != null -> listOf(scene.guideColumn)
    else -> emptyList()
}

private fun Rect.expand(padding: Float): Rect = Rect(
    left = left - padding,
    top = top - padding,
    right = right + padding,
    bottom = bottom + padding,
)

private fun Rect.scaleAroundCenter(scale: Float): Rect {
    val safeScale = scale.coerceAtLeast(0.01f)
    val scaledWidth = width * safeScale
    val scaledHeight = height * safeScale
    return Rect(
        left = center.x - (scaledWidth / 2f),
        top = center.y - (scaledHeight / 2f),
        right = center.x + (scaledWidth / 2f),
        bottom = center.y + (scaledHeight / 2f),
    )
}

@Preview
@Composable
private fun InteractiveGameOnboardingOverlayTrayPreview() {
    InteractiveGameOnboardingOverlayPreviewHost(
        stage = FirstRunOnboardingStage.DragAndLaunch,
        settings = AppSettings(),
        hasDraggedAwayFromSpawn = false,
        isTargetAligned = false,
        isAwaitingPlacementCommit = false,
    )
}

@Preview
@Composable
private fun InteractiveGameOnboardingOverlayLineClearPreview() {
    InteractiveGameOnboardingOverlayPreviewHost(
        stage = FirstRunOnboardingStage.LineClear,
        settings = AppSettings(themeMode = AppThemeMode.Dark),
        hasDraggedAwayFromSpawn = true,
        isTargetAligned = true,
        isAwaitingPlacementCommit = false,
    )
}

@Preview
@Composable
private fun InteractiveGameOnboardingOverlayRowClearerPreview() {
    InteractiveGameOnboardingOverlayPreviewHost(
        stage = FirstRunOnboardingStage.RowClearer,
        settings = AppSettings(themeMode = AppThemeMode.Dark),
        hasDraggedAwayFromSpawn = true,
        isTargetAligned = true,
        isAwaitingPlacementCommit = false,
    )
}

@Preview
@Composable
private fun InteractiveGameOnboardingOverlayColumnClearerPreview() {
    InteractiveGameOnboardingOverlayPreviewHost(
        stage = FirstRunOnboardingStage.ColumnClearer,
        settings = AppSettings(themeMode = AppThemeMode.Dark),
        hasDraggedAwayFromSpawn = true,
        isTargetAligned = true,
        isAwaitingPlacementCommit = true,
    )
}

@Preview
@Composable
private fun InteractiveGameOnboardingOverlayGhostPreview() {
    InteractiveGameOnboardingOverlayPreviewHost(
        stage = FirstRunOnboardingStage.Ghost,
        settings = AppSettings(themeMode = AppThemeMode.Dark),
        hasDraggedAwayFromSpawn = true,
        isTargetAligned = true,
        isAwaitingPlacementCommit = false,
    )
}

@Preview
@Composable
private fun InteractiveGameOnboardingOverlayHeavyPreview() {
    InteractiveGameOnboardingOverlayPreviewHost(
        stage = FirstRunOnboardingStage.Heavy,
        settings = AppSettings(themeMode = AppThemeMode.Dark),
        hasDraggedAwayFromSpawn = true,
        isTargetAligned = false,
        isAwaitingPlacementCommit = false,
    )
}

@Composable
private fun InteractiveGameOnboardingOverlayPreviewHost(
    stage: FirstRunOnboardingStage,
    settings: AppSettings,
    hasDraggedAwayFromSpawn: Boolean,
    isTargetAligned: Boolean,
    isAwaitingPlacementCommit: Boolean,
) {
    val onboardingStages = FirstRunGameOnboardingStateFactory.stages
    val scene = FirstRunGameOnboardingStateFactory.scene(stage)
    val boardRect = Rect(left = 76f, top = 174f, right = 716f, bottom = 948f)
    val trayRect = Rect(left = 68f, top = 1006f, right = 724f, bottom = 1134f)
    StackShiftTheme(settings = settings) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush(StackShiftThemeTokens.uiColors))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(106.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = StackShiftThemeTokens.uiColors.gameSurface.copy(alpha = 0.62f),
                    border = BorderStroke(1.dp, StackShiftThemeTokens.uiColors.panelStroke.copy(alpha = 0.48f)),
                ) {}
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(28.dp),
                    color = StackShiftThemeTokens.uiColors.panel.copy(alpha = 0.34f),
                    border = BorderStroke(1.dp, StackShiftThemeTokens.uiColors.panelStroke.copy(alpha = 0.38f)),
                ) {}
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = StackShiftThemeTokens.uiColors.gameSurface.copy(alpha = 0.52f),
                    border = BorderStroke(1.dp, StackShiftThemeTokens.uiColors.panelStroke.copy(alpha = 0.42f)),
                ) {}
            }

            InteractiveGameOnboardingOverlay(
                ui = GameInteractiveOnboardingUi(
                    scene = scene,
                    currentStep = onboardingStages.indexOf(stage) + 1,
                    totalSteps = onboardingStages.size,
                    hasDraggedAwayFromSpawn = hasDraggedAwayFromSpawn,
                    isTargetAligned = isTargetAligned,
                    isAwaitingPlacementCommit = isAwaitingPlacementCommit,
                ),
                boardRect = boardRect,
                trayRect = trayRect,
                cellSizePx = boardRect.width / scene.gameState.config.columns,
            )
        }
    }
}

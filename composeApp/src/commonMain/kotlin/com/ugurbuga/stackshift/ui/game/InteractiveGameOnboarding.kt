package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.StackShiftTheme
import com.ugurbuga.stackshift.game.logic.GameLogic
import com.ugurbuga.stackshift.game.model.AppThemeMode
import com.ugurbuga.stackshift.game.model.PlacementPreview
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.settings.FirstRunGameOnboardingStateFactory
import com.ugurbuga.stackshift.settings.FirstRunOnboardingScene
import com.ugurbuga.stackshift.settings.FirstRunOnboardingStage
import com.ugurbuga.stackshift.settings.FirstRunOnboardingTarget
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

@Immutable
data class GameInteractiveOnboardingUi(
    val scene: FirstRunOnboardingScene,
    val currentStep: Int,
    val totalSteps: Int,
    val hasDraggedAwayFromSpawn: Boolean,
    val isTargetAligned: Boolean,
    val isAwaitingPlacementCommit: Boolean,
)

private val onboardingGuideLogic = GameLogic()
private const val InteractiveOnboardingTargetPulseDurationMillis = 1880

@Immutable
private data class InteractiveOnboardingVisualState(
    val title: String,
    val body: String,
    val hint: String,
    val guideColor: Color,
    val guideBadgeTextColor: Color,
    val hintContainerAlpha: Float,
    val hintBorderAlpha: Float,
    val isSuccessState: Boolean,
)

@Composable
fun InteractiveGameOnboardingOverlay(
    ui: GameInteractiveOnboardingUi,
    boardRect: Rect,
    trayRect: Rect,
    spawnRect: Rect = Rect.Zero,
    cellSizePx: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val targetRect = remember(ui.scene, boardRect, trayRect, spawnRect, cellSizePx) {
        onboardingTargetRect(
            scene = ui.scene,
            boardRect = boardRect,
            trayRect = trayRect,
            spawnRect = spawnRect,
            cellSizePx = cellSizePx,
        )
    }
    val dragTitle = stringResource(Res.string.interactive_onboarding_drag_title)
    val lineClearTitle = stringResource(Res.string.interactive_onboarding_line_clear_title)
    val columnClearTitle = stringResource(Res.string.interactive_onboarding_column_clearer_title)
    val rowClearTitle = stringResource(Res.string.interactive_onboarding_row_clearer_title)
    val ghostTitle = stringResource(Res.string.interactive_onboarding_ghost_title)
    val heavyTitle = stringResource(Res.string.interactive_onboarding_heavy_title)
    val dragBody = stringResource(Res.string.interactive_onboarding_drag_body)
    val lineClearBody = stringResource(Res.string.interactive_onboarding_line_clear_body)
    val columnClearBody = stringResource(Res.string.interactive_onboarding_column_clearer_body)
    val rowClearBody = stringResource(Res.string.interactive_onboarding_row_clearer_body)
    val ghostBody = stringResource(Res.string.interactive_onboarding_ghost_body)
    val heavyBody = stringResource(Res.string.interactive_onboarding_heavy_body)
    val waitingHint = stringResource(Res.string.interactive_onboarding_waiting)
    val targetLockedHint = stringResource(Res.string.interactive_onboarding_target_locked)
    val dragReadyHint = stringResource(Res.string.interactive_onboarding_drag_ready)
    val dragToBoardHint = stringResource(Res.string.interactive_onboarding_drag_to_board)
    val aimHint = stringResource(Res.string.interactive_onboarding_aim_hint)
    val visualState = remember(
        ui,
        uiColors.warning,
        uiColors.success,
        uiColors.actionButton,
        dragTitle,
        lineClearTitle,
        columnClearTitle,
        rowClearTitle,
        ghostTitle,
        heavyTitle,
        dragBody,
        lineClearBody,
        columnClearBody,
        rowClearBody,
        ghostBody,
        heavyBody,
        waitingHint,
        targetLockedHint,
        dragReadyHint,
        dragToBoardHint,
        aimHint,
    ) {
        val isDragStepReady = (ui.scene.stage == FirstRunOnboardingStage.DragAndLaunch) && ui.hasDraggedAwayFromSpawn
        val isLaterStepReady = (ui.scene.stage != FirstRunOnboardingStage.DragAndLaunch) && ui.isTargetAligned
        val isSuccessState = isDragStepReady || isLaterStepReady
        val title = when (ui.scene.stage) {
            FirstRunOnboardingStage.DragAndLaunch -> dragTitle
            FirstRunOnboardingStage.LineClear -> lineClearTitle
            FirstRunOnboardingStage.ColumnClearer -> columnClearTitle
            FirstRunOnboardingStage.RowClearer -> rowClearTitle
            FirstRunOnboardingStage.Ghost -> ghostTitle
            FirstRunOnboardingStage.Heavy -> heavyTitle
        }
        val body = when (ui.scene.stage) {
            FirstRunOnboardingStage.DragAndLaunch -> dragBody
            FirstRunOnboardingStage.LineClear -> lineClearBody
            FirstRunOnboardingStage.ColumnClearer -> columnClearBody
            FirstRunOnboardingStage.RowClearer -> rowClearBody
            FirstRunOnboardingStage.Ghost -> ghostBody
            FirstRunOnboardingStage.Heavy -> heavyBody
        }
        val hint = when {
            ui.isAwaitingPlacementCommit -> waitingHint
            isLaterStepReady -> targetLockedHint
            isDragStepReady -> dragReadyHint
            ui.scene.stage == FirstRunOnboardingStage.DragAndLaunch -> dragToBoardHint
            else -> aimHint
        }
        val guideColor = when {
            ui.isAwaitingPlacementCommit -> uiColors.warning
            isSuccessState -> uiColors.success
            else -> uiColors.actionButton
        }
        InteractiveOnboardingVisualState(
            title = title,
            body = body,
            hint = hint,
            guideColor = guideColor,
            guideBadgeTextColor = if (guideColor.luminance() > 0.58f) Color(0xFF101114) else Color.White,
            hintContainerAlpha = when {
                ui.isAwaitingPlacementCommit -> 0.26f
                isSuccessState -> 0.20f
                else -> 0.18f
            },
            hintBorderAlpha = when {
                ui.isAwaitingPlacementCommit -> 0.72f
                isSuccessState -> 0.58f
                else -> 0.48f
            },
            isSuccessState = isSuccessState,
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        InteractiveOnboardingTargetHighlight(
            targetRect = targetRect,
            guideColor = visualState.guideColor,
            isSuccessState = visualState.isSuccessState,
            isAwaitingPlacementCommit = ui.isAwaitingPlacementCommit,
        )

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
                        androidx.compose.ui.graphics.Brush.verticalGradient(
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
                        color = visualState.guideColor.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                    ) {
                        Text(
                            text = stringResource(
                                Res.string.interactive_onboarding_step_counter,
                                ui.currentStep,
                                ui.totalSteps,
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = visualState.guideBadgeTextColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
                Text(
                    text = visualState.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = visualState.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = visualState.guideColor.copy(alpha = visualState.hintContainerAlpha),
                    border = BorderStroke(1.dp, visualState.guideColor.copy(alpha = visualState.hintBorderAlpha)),
                ) {
                    Text(
                        text = visualState.hint,
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
private fun InteractiveOnboardingTargetHighlight(
    targetRect: Rect?,
    guideColor: Color,
    isSuccessState: Boolean,
    isAwaitingPlacementCommit: Boolean,
    modifier: Modifier = Modifier,
) {
    val rect = targetRect ?: return
    val density = LocalDensity.current
    val shouldAnimatePulse = !isAwaitingPlacementCommit && !isSuccessState
    val pulsePhase = if (!shouldAnimatePulse) {
        0f
    } else {
        val targetPulseTransition = rememberInfiniteTransition(label = "interactiveOnboardingTargetPulse")
        targetPulseTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = InteractiveOnboardingTargetPulseDurationMillis,
                    easing = FastOutSlowInEasing,
                ),
            ),
            label = "interactiveOnboardingTargetPulsePhase",
        ).value
    }
    val widthDp = with(density) { rect.width.toDp() }
    val heightDp = with(density) { rect.height.toDp() }

    Canvas(
        modifier = modifier
            .graphicsLayer(
                translationX = rect.left,
                translationY = rect.top,
            )
            .size(width = widthDp, height = heightDp),
    ) {
        val localTargetRect = Rect(0f, 0f, size.width, size.height)
        val pulseScale = when {
            isAwaitingPlacementCommit -> 1f
            isSuccessState -> 1f
            else -> 1.004f + (pulsePhase * 0.010f)
        }
        val pulseRect = localTargetRect.scaleAroundCenter(pulseScale)
        val cornerRadius = CornerRadius(24f, 24f)

        drawRoundRect(
            color = guideColor.copy(
                alpha = when {
                    isAwaitingPlacementCommit -> 0.10f
                    isSuccessState -> 0.12f
                    else -> 0.09f
                },
            ),
            topLeft = Offset.Zero,
            size = localTargetRect.size,
            cornerRadius = cornerRadius,
        )
        drawRoundRect(
            color = guideColor.copy(alpha = if (isSuccessState) 0.88f else 0.82f),
            topLeft = Offset.Zero,
            size = localTargetRect.size,
            cornerRadius = cornerRadius,
            style = Stroke(width = if (isSuccessState) 4f else 3.6f),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = if (isSuccessState) 0.12f else 0.08f),
            topLeft = Offset(2f, 2f),
            size = Size(
                width = (size.width - 4f).coerceAtLeast(0f),
                height = (size.height - 4f).coerceAtLeast(0f),
            ),
            cornerRadius = CornerRadius(20f, 20f),
            style = Stroke(width = 1f),
        )
        if (shouldAnimatePulse) {
            drawRoundRect(
                color = guideColor.copy(
                    alpha = 0.08f + (pulsePhase * 0.04f)
                ),
                topLeft = Offset(pulseRect.left, pulseRect.top),
                size = pulseRect.size,
                cornerRadius = CornerRadius(28f, 28f),
                style = Stroke(width = 4f),
            )
        }
    }
}

private fun onboardingTargetRect(
    scene: FirstRunOnboardingScene,
    boardRect: Rect,
    trayRect: Rect,
    spawnRect: Rect,
    cellSizePx: Float,
): Rect? = when (scene.target) {
    FirstRunOnboardingTarget.Tray -> when {
        scene.stage == FirstRunOnboardingStage.DragAndLaunch && trayRect != Rect.Zero -> {
            trayRect.expand(horizontalPadding = -14f, verticalPadding = -10f)
        }
        spawnRect != Rect.Zero -> spawnRect.expand(horizontalPadding = 10f, verticalPadding = 8f)
        trayRect != Rect.Zero -> trayRect.expand(horizontalPadding = 14f, verticalPadding = 8f)
        else -> null
    }
    FirstRunOnboardingTarget.Board -> {
        if ((boardRect == Rect.Zero) || (cellSizePx <= 0f)) return null
        onboardingTargetPreview(scene)?.toBoardRect(boardRect, cellSizePx)?.expand(padding = 8f)
            ?: boardRect.expand(padding = 6f)
    }
}

private fun onboardingTargetPreview(scene: FirstRunOnboardingScene): PlacementPreview? {
    val candidateColumns = buildList {
        scene.guideColumn?.let(::add)
        addAll(scene.acceptedColumns.sorted())
    }.distinct()
    return candidateColumns.firstNotNullOfOrNull { column ->
        onboardingGuideLogic.previewPlacement(scene.gameState, column)
    }
}

private fun PlacementPreview.toBoardRect(boardRect: Rect, cellSizePx: Float): Rect? {
    if (occupiedCells.isEmpty() || (boardRect == Rect.Zero) || (cellSizePx <= 0f)) return null
    val minColumn = occupiedCells.minOf { it.column }
    val maxColumn = occupiedCells.maxOf { it.column }
    val minRow = occupiedCells.minOf { it.row }
    val maxRow = occupiedCells.maxOf { it.row }
    return Rect(
        left = boardRect.left + (minColumn * cellSizePx),
        top = boardRect.top + (minRow * cellSizePx),
        right = boardRect.left + ((maxColumn + 1) * cellSizePx),
        bottom = boardRect.top + ((maxRow + 1) * cellSizePx),
    )
}

private fun Rect.expand(padding: Float): Rect = Rect(
    left = left - padding,
    top = top - padding,
    right = right + padding,
    bottom = bottom + padding,
)

private fun Rect.expand(horizontalPadding: Float, verticalPadding: Float): Rect = Rect(
    left = left - horizontalPadding,
    top = top - verticalPadding,
    right = right + horizontalPadding,
    bottom = bottom + verticalPadding,
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
    val boardRect = Rect(left = 76f, top = 372f, right = 716f, bottom = 756f)
    val trayRect = Rect(left = 68f, top = 818f, right = 724f, bottom = 946f)
    val previewCellSizePx = boardRect.width / scene.gameState.config.columns
    val previewSpawnColumn = resolveSpawnColumn(
        piece = scene.gameState.activePiece,
        boardColumns = scene.gameState.config.columns,
        lastPlacementColumn = scene.gameState.lastPlacementColumn,
    )
    val spawnTopLeft = pieceSpawnTopLeft(
        piece = scene.gameState.activePiece,
        trayRect = trayRect,
        boardRect = boardRect,
        cellSizePx = previewCellSizePx,
        column = previewSpawnColumn,
    )
    val spawnRect = if (scene.gameState.activePiece != null && spawnTopLeft != null) {
        Rect(
            left = spawnTopLeft.x,
            top = spawnTopLeft.y,
            right = spawnTopLeft.x + (scene.gameState.activePiece.width * previewCellSizePx),
            bottom = spawnTopLeft.y + (scene.gameState.activePiece.height * previewCellSizePx),
        )
    } else {
        Rect.Zero
    }
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
                spawnRect = spawnRect,
                cellSizePx = previewCellSizePx,
            )
        }
    }
}

package com.ugurbuga.stackshift.ui.game

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.StackShiftTheme
import com.ugurbuga.stackshift.ads.GameAdController
import com.ugurbuga.stackshift.ads.NoOpGameAdController
import com.ugurbuga.stackshift.game.logic.GameLogic
import com.ugurbuga.stackshift.game.model.BoardMatrix
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.GameConfig
import com.ugurbuga.stackshift.game.model.GameState
import com.ugurbuga.stackshift.game.model.GameStatus
import com.ugurbuga.stackshift.game.model.GameTextKey
import com.ugurbuga.stackshift.game.model.GridPoint
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.PieceKind
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import com.ugurbuga.stackshift.game.model.gameText
import com.ugurbuga.stackshift.game.model.resolveBoardBlockStyle
import com.ugurbuga.stackshift.localization.LocalAppSettings
import com.ugurbuga.stackshift.settings.AppSettings
import com.ugurbuga.stackshift.telemetry.AppTelemetry
import com.ugurbuga.stackshift.telemetry.LogScreen
import com.ugurbuga.stackshift.telemetry.NoOpAppTelemetry
import com.ugurbuga.stackshift.telemetry.TelemetryScreenNames
import com.ugurbuga.stackshift.ui.theme.GameUiShapeTokens
import com.ugurbuga.stackshift.ui.theme.StackShiftThemeTokens
import com.ugurbuga.stackshift.ui.theme.appBackgroundBrush
import com.ugurbuga.stackshift.ui.theme.isStackShiftDarkTheme
import com.ugurbuga.stackshift.ui.theme.stackShiftSurfaceShadow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.block_properties_column_clearer_desc
import stackshift.composeapp.generated.resources.block_properties_column_clearer_title
import stackshift.composeapp.generated.resources.block_properties_ghost_desc
import stackshift.composeapp.generated.resources.block_properties_ghost_title
import stackshift.composeapp.generated.resources.block_properties_heavy_desc
import stackshift.composeapp.generated.resources.block_properties_heavy_title
import stackshift.composeapp.generated.resources.block_properties_row_clearer_desc
import stackshift.composeapp.generated.resources.block_properties_row_clearer_title
import stackshift.composeapp.generated.resources.launch_drag_hint
import stackshift.composeapp.generated.resources.piece_properties_active
import stackshift.composeapp.generated.resources.queue_next_short
import stackshift.composeapp.generated.resources.restart
import stackshift.composeapp.generated.resources.settings_challenges
import stackshift.composeapp.generated.resources.settings_language
import stackshift.composeapp.generated.resources.settings_theme
import stackshift.composeapp.generated.resources.settings_tutorial
import stackshift.composeapp.generated.resources.tutorial_back
import stackshift.composeapp.generated.resources.tutorial_finish
import stackshift.composeapp.generated.resources.tutorial_intro_body
import stackshift.composeapp.generated.resources.tutorial_intro_title
import stackshift.composeapp.generated.resources.tutorial_next
import stackshift.composeapp.generated.resources.tutorial_ready_body
import stackshift.composeapp.generated.resources.tutorial_ready_settings_hint
import stackshift.composeapp.generated.resources.tutorial_ready_title
import stackshift.composeapp.generated.resources.tutorial_ready_tutorial_hint
import stackshift.composeapp.generated.resources.tutorial_specials_body
import stackshift.composeapp.generated.resources.tutorial_specials_title
import stackshift.composeapp.generated.resources.tutorial_step_counter
import stackshift.composeapp.generated.resources.tutorial_systems_body
import stackshift.composeapp.generated.resources.tutorial_systems_title

private val TutorialMiniDockHeight = 88.dp
private const val TutorialIntroColumns = 10
private const val TutorialIntroRows = 10
private const val TutorialCompactColumns = 5
private const val TutorialCompactRows = 5
private const val TutorialDemoAutoplayStartDelayMillis = 720L
private const val TutorialDemoAutoplayStepDelayMillis = 1650L
private const val TutorialDemoTravelDurationMillis = 1120
private const val TutorialDemoSoftLockDelayMillis = 220L
private const val TutorialDemoResolutionHoldMillis = 980L
private const val TutorialDemoResetDelayMillis = 480L
private const val TutorialDemoClearAnimationDurationMillis = 620
private const val TutorialDemoBoardShiftDurationMillis = 360
private const val TutorialDemoTrayPulseDurationMillis = 900
private const val TutorialDemoTrayPulseScaleBoost = 0.05f
private val TutorialSamplePiece = Piece(
    id = -1,
    kind = PieceKind.T,
    tone = CellTone.Cyan,
    cells = listOf(
        GridPoint(column = 1, row = 0),
        GridPoint(column = 0, row = 1),
        GridPoint(column = 1, row = 1),
        GridPoint(column = 2, row = 1),
    ),
    width = 3,
    height = 2,
)
private val TutorialNextPiece = Piece(
    id = -2,
    kind = PieceKind.L,
    tone = CellTone.Gold,
    cells = listOf(
        GridPoint(column = 0, row = 0),
        GridPoint(column = 0, row = 1),
        GridPoint(column = 0, row = 2),
        GridPoint(column = 1, row = 2),
    ),
    width = 2,
    height = 3,
    special = SpecialBlockType.Ghost,
)

private data class TutorialDemoScene(
    val gameState: GameState,
    val spawnColumn: Int,
    val autoColumns: List<Int>,
)

private fun tutorialPreviewGameState(
    board: BoardMatrix,
    activePiece: Piece,
    nextQueue: List<Piece> = listOf(TutorialNextPiece),
    spawnColumn: Int,
): GameState = GameState(
    config = GameConfig(columns = board.columns, rows = board.rows),
    board = board,
    activePiece = activePiece,
    nextQueue = nextQueue,
    holdPiece = null,
    canHold = true,
    lastPlacementColumn = spawnColumn,
    score = 0,
    linesCleared = 0,
    level = 1,
    difficultyStage = 0,
    secondsUntilDifficultyIncrease = 18,
    status = GameStatus.Running,
    message = gameText(GameTextKey.GameMessageSelectColumn),
)

private fun tutorialIntroScene(): TutorialDemoScene {
    val board = BoardMatrix.empty(columns = TutorialIntroColumns, rows = TutorialIntroRows)
        .fill(
            points = listOf(
                GridPoint(1, 5),
                GridPoint(2, 5),
                GridPoint(3, 5),
                GridPoint(6, 4),
                GridPoint(7, 4),
                GridPoint(8, 4),
                GridPoint(4, 3),
                GridPoint(5, 3),
            ),
            tone = CellTone.Blue,
        )
        .fill(
            points = listOf(
                GridPoint(2, 4),
                GridPoint(7, 3),
            ),
            tone = CellTone.Gold,
        )
    return TutorialDemoScene(
        gameState = tutorialPreviewGameState(
            board = board,
            activePiece = TutorialSamplePiece.copy(id = -101, tone = CellTone.Cyan),
            spawnColumn = 0,
        ),
        spawnColumn = 0,
        autoColumns = listOf(0, 2, 4, 6, 7, 5, 3, 1),
    )
}

private fun tutorialSpecialScene(
    tone: CellTone,
    special: SpecialBlockType,
): TutorialDemoScene {
    val board = when (special) {
        SpecialBlockType.ColumnClearer -> BoardMatrix.empty(
            columns = TutorialCompactColumns,
            rows = TutorialCompactRows
        )
            .fill(
                points = listOf(
                    GridPoint(0, 3),
                    GridPoint(1, 3),
                    GridPoint(1, 2),
                    GridPoint(2, 2),
                    GridPoint(3, 2),
                    GridPoint(4, 2),
                ),
                tone = CellTone.Blue,
            )
            .fill(
                points = listOf(
                    GridPoint(4, 1),
                ),
                tone = CellTone.Gold,
            )

        SpecialBlockType.RowClearer -> BoardMatrix.empty(
            columns = TutorialCompactColumns,
            rows = TutorialCompactRows
        )
            .fill(
                points = listOf(
                    GridPoint(0, 2),
                    GridPoint(1, 2),
                    GridPoint(3, 2),
                    GridPoint(4, 2),
                    GridPoint(1, 1),
                    GridPoint(2, 1),
                    GridPoint(4, 1),
                ),
                tone = CellTone.Emerald,
            )

        SpecialBlockType.Ghost -> BoardMatrix.empty(
            columns = TutorialCompactColumns,
            rows = TutorialCompactRows
        )
            .fill(
                points = listOf(
                    GridPoint(0, 3),
                    GridPoint(1, 2),
                    GridPoint(2, 2),
                    GridPoint(4, 2),
                    GridPoint(0, 1),
                    GridPoint(3, 1),
                ),
                tone = CellTone.Violet,
            )
            .fill(
                points = listOf(GridPoint(2, 1)),
                tone = CellTone.Gold,
            )

        SpecialBlockType.Heavy -> BoardMatrix.empty(
            columns = TutorialCompactColumns,
            rows = TutorialCompactRows
        )
            .fill(
                points = listOf(
                    GridPoint(0, 3),
                    GridPoint(1, 3),
                    GridPoint(3, 2),
                    GridPoint(4, 2),
                    GridPoint(0, 1),
                    GridPoint(2, 1),
                    GridPoint(3, 1),
                ),
                tone = CellTone.Coral,
            )

        SpecialBlockType.None -> BoardMatrix.empty(
            columns = TutorialCompactColumns,
            rows = TutorialCompactRows
        )
    }
    val activePiece = when (special) {
        SpecialBlockType.Ghost -> TutorialNextPiece.copy(id = -202L, tone = tone, special = special)
        else -> TutorialSamplePiece.copy(
            id = -201L - special.ordinal,
            tone = tone,
            special = special
        )
    }
    val spawnColumn = if (special == SpecialBlockType.Ghost) 1 else 1
    val autoColumns = when (special) {
        SpecialBlockType.Ghost -> listOf(0, 1, 2, 3, 2, 1)
        else -> listOf(0, 1, 2, 1)
    }
    return TutorialDemoScene(
        gameState = tutorialPreviewGameState(
            board = board,
            activePiece = activePiece,
            spawnColumn = spawnColumn,
        ),
        spawnColumn = spawnColumn,
        autoColumns = autoColumns,
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GameTutorialScreen(
    modifier: Modifier = Modifier,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    adController: GameAdController = NoOpGameAdController,
    initialPage: Int = 0,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    LogScreen(telemetry, TelemetryScreenNames.Tutorial)
    val totalSteps = 4
    val uiColors = StackShiftThemeTokens.uiColors
    val pagerState = rememberPagerState(initialPage = initialPage) { totalSteps }
    val coroutineScope = rememberCoroutineScope()
    val currentStep = pagerState.currentPage
    val isLastStep = (currentStep == (totalSteps - 1))

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appBackgroundBrush(uiColors))
                .statusBarsPadding(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                            TopBarActionBlockButton(
                                tone = CellTone.Cyan,
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(Res.string.tutorial_back),
                                onClick = onBack,
                                size = 44.dp,
                            )

                    Text(
                        text = stringResource(Res.string.settings_tutorial),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )

                    TutorialStepChip(
                        currentStep = currentStep + 1,
                        totalSteps = totalSteps,
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    uiColors.panelHighlight.copy(alpha = 0.14f),
                                    uiColors.launchGlow.copy(alpha = 0.10f),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f),
                        userScrollEnabled = true,
                        verticalAlignment = Alignment.Top,
                    ) { page ->
                        TutorialScrollablePage {
                            when (page) {
                                0 -> TutorialIntroStep()
                                1 -> TutorialSystemsStep()
                                2 -> TutorialSpecialsStep()
                                3 -> TutorialReadyStep()
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (currentStep > 0) {
                        TopBarActionBlockButton(
                            tone = CellTone.Cyan,
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.tutorial_back),
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(currentStep - 1)
                                }
                            },
                            size = 40.dp,
                        )
                    } else {
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    BlockStyleActionButton(
                        text = if (isLastStep) stringResource(Res.string.tutorial_finish) else stringResource(
                            Res.string.tutorial_next
                        ),
                        icon = if (isLastStep) Icons.Filled.PlayArrow else Icons.AutoMirrored.Filled.ArrowForward,
                        onClick = {
                            if (isLastStep) {
                                onFinish()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(currentStep + 1)
                                }
                            }
                        },
                        modifier = Modifier
                            .widthIn(max = 180.dp)
                            .height(48.dp),
                        emphasized = true,
                        tone = if (isLastStep) CellTone.Emerald else CellTone.Cyan,
                        iconOnRight = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialStepChip(currentStep: Int, totalSteps: Int) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = Modifier.stackShiftSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.chipCorner),
            elevation = 5.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.chipCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.panelMuted.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.68f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Text(
            text = stringResource(Res.string.tutorial_step_counter, currentStep, totalSteps),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TutorialScrollablePage(
    content: @Composable ColumnScope.() -> Unit,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val scrollState = rememberScrollState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(end = 2.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )

        if (scrollState.canScrollBackward) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(22.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                uiColors.panel.copy(alpha = 0.82f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }

        if (scrollState.canScrollForward) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                uiColors.panel.copy(alpha = 0.88f),
                            ),
                        ),
                    )
                    .padding(top = 18.dp, bottom = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "⌄",
                    style = MaterialTheme.typography.titleMedium,
                    color = uiColors.subtitle,
                )
                Text(
                    text = "⌄",
                    style = MaterialTheme.typography.labelSmall,
                    color = uiColors.subtitle.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun TutorialMiniBoardShell(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    val settings = LocalAppSettings.current
    val boardStyle = resolveBoardBlockStyle(settings.blockVisualStyle, settings.boardBlockStyleMode)
    Card(
        modifier = modifier.stackShiftSurfaceShadow(
            shape = RoundedCornerShape(boardFrameCornerRadiusDp(boardStyle)),
            elevation = 10.dp,
        ),
        shape = RoundedCornerShape(boardFrameCornerRadiusDp(boardStyle)),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.88f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.boardOutline.copy(alpha = 0.84f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.boardGradientTop.copy(alpha = 0.24f),
                            uiColors.boardGradientBottom.copy(alpha = 0.28f),
                        ),
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 10.dp)
                .padding(bottom = 12.dp),
            content = content,
        )
    }
}

@Composable
private fun TutorialIntroStep(launchPreviewColumn: Int? = null) {
    TutorialSection(
        title = stringResource(Res.string.tutorial_intro_title),
        body = stringResource(Res.string.tutorial_intro_body),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TutorialPieceCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.piece_properties_active),
                piece = TutorialSamplePiece,
                alpha = 1f,
            )
            TutorialPieceCard(
                modifier = Modifier.weight(1f),
                title = stringResource(Res.string.queue_next_short),
                piece = TutorialNextPiece,
                alpha = 0.58f,
            )
        }
        TutorialLaunchBoardDemo(lockedColumn = launchPreviewColumn)
        TutorialHintCard(text = stringResource(Res.string.launch_drag_hint))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TutorialSystemsStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_systems_title),
        body = stringResource(Res.string.tutorial_systems_body),
    ) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TutorialActionTile(
                tone = CellTone.Emerald,
                icon = Icons.Filled.EmojiEvents,
                title = stringResource(Res.string.settings_challenges),
            )
            TutorialActionTile(
                tone = CellTone.Gold,
                icon = Icons.AutoMirrored.Filled.MenuBook,
                title = stringResource(Res.string.settings_tutorial),
            )
            TutorialActionTile(
                tone = CellTone.Violet,
                icon = Icons.Filled.Palette,
                title = stringResource(Res.string.settings_theme),
            )
            TutorialActionTile(
                tone = CellTone.Coral,
                icon = Icons.Filled.Translate,
                title = stringResource(Res.string.settings_language),
            )
            TutorialActionTile(
                tone = CellTone.Cyan,
                icon = Icons.Filled.Refresh,
                title = stringResource(Res.string.restart),
            )
        }
    }
}

@Composable
private fun TutorialSpecialsStep(previewColumns: Map<SpecialBlockType, Int> = emptyMap()) {
    TutorialSection(
        title = stringResource(Res.string.tutorial_specials_title),
        body = stringResource(Res.string.tutorial_specials_body),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TutorialAnimatedSpecialsShowcase(previewColumns = previewColumns)
            TutorialSpecialCard(
                tone = CellTone.Emerald,
                special = SpecialBlockType.ColumnClearer,
                title = stringResource(Res.string.block_properties_column_clearer_title),
                body = stringResource(Res.string.block_properties_column_clearer_desc),
            )
            TutorialSpecialCard(
                tone = CellTone.Amber,
                special = SpecialBlockType.RowClearer,
                title = stringResource(Res.string.block_properties_row_clearer_title),
                body = stringResource(Res.string.block_properties_row_clearer_desc),
            )
            TutorialSpecialCard(
                tone = CellTone.Violet,
                special = SpecialBlockType.Ghost,
                title = stringResource(Res.string.block_properties_ghost_title),
                body = stringResource(Res.string.block_properties_ghost_desc),
            )
            TutorialSpecialCard(
                tone = CellTone.Coral,
                special = SpecialBlockType.Heavy,
                title = stringResource(Res.string.block_properties_heavy_title),
                body = stringResource(Res.string.block_properties_heavy_desc),
            )
        }
    }
}

@Composable
private fun TutorialReadyStep() {
    TutorialSection(
        title = stringResource(Res.string.tutorial_ready_title),
        body = stringResource(Res.string.tutorial_ready_body),
    ) {
        TutorialHintCard(text = stringResource(Res.string.tutorial_ready_tutorial_hint))
        TutorialHintCard(text = stringResource(Res.string.tutorial_ready_settings_hint))
    }
}

@Composable
private fun TutorialLaunchBoardDemo(
    lockedColumn: Int? = null,
) {
    TutorialMiniGameDemo(
        scene = remember { tutorialIntroScene() },
        compact = false,
        lockedColumn = lockedColumn,
    )
}

@Composable
private fun TutorialAnimatedSpecialsShowcase(
    previewColumns: Map<SpecialBlockType, Int> = emptyMap(),
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TutorialSpecialBoardDemo(
                modifier = Modifier.weight(1f),
                tone = CellTone.Emerald,
                special = SpecialBlockType.ColumnClearer,
                lockedColumn = previewColumns[SpecialBlockType.ColumnClearer],
            )
            TutorialSpecialBoardDemo(
                modifier = Modifier.weight(1f),
                tone = CellTone.Amber,
                special = SpecialBlockType.RowClearer,
                lockedColumn = previewColumns[SpecialBlockType.RowClearer],
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TutorialSpecialBoardDemo(
                modifier = Modifier.weight(1f),
                tone = CellTone.Violet,
                special = SpecialBlockType.Ghost,
                lockedColumn = previewColumns[SpecialBlockType.Ghost],
            )
            TutorialSpecialBoardDemo(
                modifier = Modifier.weight(1f),
                tone = CellTone.Coral,
                special = SpecialBlockType.Heavy,
                lockedColumn = previewColumns[SpecialBlockType.Heavy],
            )
        }
    }
}

@Composable
private fun TutorialSpecialBoardDemo(
    tone: CellTone,
    special: SpecialBlockType,
    modifier: Modifier = Modifier,
    lockedColumn: Int? = null,
) {
    TutorialMiniGameDemo(
        scene = remember(tone, special) { tutorialSpecialScene(tone = tone, special = special) },
        modifier = modifier,
        compact = true,
        lockedColumn = lockedColumn,
        badgeTone = tone,
        badgeSpecial = special,
    )
}

@Composable
private fun TutorialMiniGameDemo(
    scene: TutorialDemoScene,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    lockedColumn: Int? = null,
    badgeTone: CellTone? = null,
    badgeSpecial: SpecialBlockType = SpecialBlockType.None,
) {
    val settings = LocalAppSettings.current
    val density = LocalDensity.current
    val boardStyle = resolveBoardBlockStyle(settings.blockVisualStyle, settings.boardBlockStyleMode)
    val gameLogic = remember { GameLogic() }
    var gameState by remember(scene, lockedColumn) { mutableStateOf(scene.gameState) }
    val activePiece = gameState.activePiece
    var hostRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var boardRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var trayRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var overlayTopLeft by remember(
        scene.gameState.activePiece?.id,
        lockedColumn
    ) { mutableStateOf<Offset?>(null) }
    var isDragging by remember { mutableStateOf(value = false) }

    val boardRect = boardRectInRoot.toLocalRect(hostRectInRoot)
    val trayRect = trayRectInRoot.toLocalRect(hostRectInRoot)
    val cellSizePx = if (boardRect != Rect.Zero) {
        boardRect.width / gameState.config.columns
    } else {
        0f
    }
    val maxColumn = activePiece?.let { (gameState.config.columns - it.width).coerceAtLeast(0) } ?: 0
    val resolvedLockedColumn = lockedColumn?.coerceIn(0, maxColumn)
    val spawnColumn = scene.spawnColumn.coerceIn(0, maxColumn)
    val spawnTopLeft = pieceSpawnTopLeft(
        piece = activePiece,
        trayRect = trayRect,
        boardRect = boardRect,
        cellSizePx = cellSizePx,
        column = spawnColumn,
    )
    val autoColumns = remember(scene.autoColumns, maxColumn, spawnColumn) {
        scene.autoColumns
            .ifEmpty { listOf(spawnColumn) }
            .map { it.coerceIn(0, maxColumn) }
            .ifEmpty { listOf(spawnColumn) }
    }
    val pieceCellDp = with(density) { cellSizePx.coerceAtLeast(1f).toDp() }
    val launchCellCornerRadius = boardCellCornerRadiusDp(
        cellSize = pieceCellDp,
        style = boardStyle,
    )
    val trayPulseTransition = rememberInfiniteTransition(label = "tutorialTrayPulse")
    val trayPulsePhase by trayPulseTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = TutorialDemoTrayPulseDurationMillis),
        ),
        label = "tutorialTrayPulsePhase",
    )
    val animatedOverlayX by animateFloatAsState(
        targetValue = overlayTopLeft?.x ?: 0f,
        animationSpec = if (isDragging) {
            snap()
        } else {
            tween(durationMillis = TutorialDemoTravelDurationMillis)
        },
        label = "tutorialOverlayX",
    )
    val animatedOverlayY by animateFloatAsState(
        targetValue = overlayTopLeft?.y ?: 0f,
        animationSpec = if (isDragging) {
            snap()
        } else {
            tween(durationMillis = TutorialDemoTravelDurationMillis)
        },
        label = "tutorialOverlayY",
    )
    val overlayPreviewTopLeft = overlayTopLeft?.let {
        Offset(x = animatedOverlayX, y = animatedOverlayY)
    }
    val selectedColumn = resolvedLockedColumn ?: resolveSelectedColumn(
        piece = activePiece,
        overlayTopLeft = overlayPreviewTopLeft,
        boardRect = boardRect,
        cellSizePx = cellSizePx,
        boardColumns = gameState.config.columns,
    )
    val placementPreview = remember(gameState, selectedColumn) {
        selectedColumn?.let { gameLogic.previewPlacement(gameState, it) }
    }
    val previewImpactPoints = remember(gameState, placementPreview) {
        gameLogic.previewImpactPoints(gameState, placementPreview)
    }
    val trayPieceScale = when {
        (activePiece == null) || (overlayTopLeft == null) || (resolvedLockedColumn != null) -> 1f
        isDragging -> 1f
        else -> 1f + (TutorialDemoTrayPulseScaleBoost * trayPulsePhase)
    }

    LaunchedEffect(
        activePiece?.id,
        boardRect,
        trayRect,
        resolvedLockedColumn,
        spawnColumn,
        cellSizePx
    ) {
        if (boardRect == Rect.Zero || trayRect == Rect.Zero || cellSizePx <= 0f) return@LaunchedEffect
        if (resolvedLockedColumn != null && gameState.clearAnimationToken != scene.gameState.clearAnimationToken) {
            return@LaunchedEffect
        }
        overlayTopLeft = pieceSpawnTopLeft(
            piece = activePiece,
            trayRect = trayRect,
            boardRect = boardRect,
            cellSizePx = cellSizePx,
            column = spawnColumn,
        )
    }

    LaunchedEffect(
        activePiece?.id,
        boardRect,
        trayRect,
        resolvedLockedColumn,
        isDragging,
        autoColumns
    ) {
        if (boardRect == Rect.Zero || trayRect == Rect.Zero || cellSizePx <= 0f) return@LaunchedEffect
        if (resolvedLockedColumn != null || isDragging || activePiece == null) return@LaunchedEffect
        delay(TutorialDemoAutoplayStartDelayMillis)
        var autoIndex = 0
        while (isActive && !isDragging) {
            val column = autoColumns[autoIndex % autoColumns.size]
            overlayTopLeft = pieceSpawnTopLeft(
                piece = activePiece,
                trayRect = trayRect,
                boardRect = boardRect,
                cellSizePx = cellSizePx,
                column = column,
            )
            autoIndex += 1
            delay(TutorialDemoAutoplayStepDelayMillis)
        }
    }

    LaunchedEffect(scene, resolvedLockedColumn, boardRect, trayRect, cellSizePx, isDragging) {
        if (resolvedLockedColumn == null) return@LaunchedEffect
        if (boardRect == Rect.Zero || trayRect == Rect.Zero || cellSizePx <= 0f || isDragging) return@LaunchedEffect

        while (isActive && !isDragging) {
            gameState = scene.gameState
            overlayTopLeft = pieceSpawnTopLeft(
                piece = scene.gameState.activePiece,
                trayRect = trayRect,
                boardRect = boardRect,
                cellSizePx = cellSizePx,
                column = spawnColumn,
            )
            delay(TutorialDemoAutoplayStartDelayMillis)

            overlayTopLeft = pieceSpawnTopLeft(
                piece = scene.gameState.activePiece,
                trayRect = trayRect,
                boardRect = boardRect,
                cellSizePx = cellSizePx,
                column = resolvedLockedColumn,
            )
            delay(TutorialDemoTravelDurationMillis.toLong())

            val placed = gameLogic.placePiece(scene.gameState, resolvedLockedColumn)
            val lockedPreview = placed.state.softLock?.preview
            if (lockedPreview == null) {
                delay(TutorialDemoResetDelayMillis)
                continue
            }

            overlayTopLeft = lockedPreview.entryAnchor.toLocalTopLeft(
                boardRect = boardRect,
                cellSizePx = cellSizePx
            )
            delay((TutorialDemoTravelDurationMillis / 2).toLong())
            overlayTopLeft = lockedPreview.landingAnchor.toLocalTopLeft(
                boardRect = boardRect,
                cellSizePx = cellSizePx
            )
            delay((TutorialDemoTravelDurationMillis / 3).toLong())

            gameState = placed.state
            delay(TutorialDemoSoftLockDelayMillis)

            val committed = gameLogic.commitSoftLock(placed.state)
            gameState = committed.state
            overlayTopLeft = null
            delay(TutorialDemoResolutionHoldMillis)
            delay(TutorialDemoResetDelayMillis)
        }
    }

    TutorialMiniBoardShell(modifier = modifier) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    hostRectInRoot = coordinates.boundsInRoot()
                },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    BoardGrid(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(gameState.config.columns.toFloat() / gameState.config.rows.toFloat())
                            .onGloballyPositioned { coordinates ->
                                boardRectInRoot = coordinates.boundsInRoot()
                            },
                        gameState = gameState,
                        preview = placementPreview,
                        impactedPreviewCells = previewImpactPoints,
                        guidedColumns = resolvedLockedColumn?.let(::setOf).orEmpty(),
                        activeColumn = selectedColumn,
                        activePiece = activePiece,
                        isDragging = isDragging || (resolvedLockedColumn == null && autoColumns.size > 1),
                        clearFlashDurationMillis = if (resolvedLockedColumn != null) {
                            TutorialDemoClearAnimationDurationMillis
                        } else {
                            420
                        },
                        boardShiftDurationMillis = if (resolvedLockedColumn != null) {
                            TutorialDemoBoardShiftDurationMillis
                        } else {
                            220
                        },
                    )

                    LaunchGuideLineOverlay(
                        preview = placementPreview,
                        activePiece = activePiece,
                        pieceTopLeft = overlayPreviewTopLeft,
                        boardRect = boardRect,
                        cellSizePx = cellSizePx,
                    )

                    if (badgeTone != null && badgeSpecial != SpecialBlockType.None) {
                        BlockCellPreview(
                            tone = badgeTone,
                            palette = settings.blockColorPalette,
                            style = boardStyle,
                            size = 18.dp,
                            special = badgeSpecial,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp),
                        )
                    }
                }

                TutorialMiniBottomDock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TutorialMiniDockHeight),
                    onTrayPositioned = { trayRectInRoot = it },
                )
            }

            if (activePiece != null && overlayTopLeft != null && cellSizePx > 0f) {
                PieceBlocks(
                    piece = activePiece,
                    cellSize = pieceCellDp,
                    cellCornerRadius = launchCellCornerRadius,
                    modifier = Modifier
                        .graphicsLayer(
                            translationX = animatedOverlayX,
                            translationY = animatedOverlayY,
                            scaleX = trayPieceScale,
                            scaleY = trayPieceScale,
                            transformOrigin = TransformOrigin(0f, 0f),
                        )
                        .then(
                            if (resolvedLockedColumn == null) {
                                Modifier.pointerInput(
                                    activePiece.id,
                                    boardRect,
                                    cellSizePx,
                                ) {
                                    detectDragGestures(
                                        onDragStart = {
                                            isDragging = true
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            overlayTopLeft = if (selectedColumn != null) {
                                                overlayTopLeft?.let { current ->
                                                    Offset(
                                                        columnToLeft(
                                                            selectedColumn,
                                                            boardRect,
                                                            cellSizePx
                                                        ), current.y
                                                    )
                                                }
                                            } else {
                                                spawnTopLeft
                                            }
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            overlayTopLeft = spawnTopLeft
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val current = overlayTopLeft ?: spawnTopLeft
                                            ?: return@detectDragGestures
                                            val minLeft = boardRect.left
                                            val maxLeft =
                                                (boardRect.right - (activePiece.width * cellSizePx)).coerceAtLeast(
                                                    minLeft
                                                )
                                            overlayTopLeft = Offset(
                                                (current.x + dragAmount.x).coerceIn(
                                                    minLeft,
                                                    maxLeft
                                                ),
                                                current.y,
                                            )
                                        },
                                    )
                                }
                            } else {
                                Modifier
                            },
                        ),
                )

                val handX = with(density) { animatedOverlayX.toDp() }
                val handY = with(density) { animatedOverlayY.toDp() }

                TutorialDemoHand(
                    x = handX,
                    y = handY,
                    pieceWidth = activePiece.width,
                    pieceHeight = activePiece.height,
                    cellSize = pieceCellDp,
                    alpha = if (isDragging || resolvedLockedColumn != null) 1f else 0.75f,
                )
            }
        }
    }
}

@Composable
private fun TutorialDemoHand(
    x: androidx.compose.ui.unit.Dp,
    y: androidx.compose.ui.unit.Dp,
    pieceWidth: Int,
    pieceHeight: Int,
    cellSize: androidx.compose.ui.unit.Dp,
    alpha: Float,
) {
    val isDark = isStackShiftDarkTheme(LocalAppSettings.current)
    val handColor = if (isDark) Color.White else Color(0xFF101114)
    val handSize = cellSize * 1.5f

    Box(
        modifier = Modifier
            .offset(
                x = x + (cellSize * pieceWidth / 2f) - (handSize * 0.38f),
                y = y + (cellSize * pieceHeight) - (cellSize * 0.42f),
            )
            .graphicsLayer {
                this.alpha = alpha
                rotationZ = -12f
            },
    ) {
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = null,
            tint = Color.Black.copy(alpha = if (isDark) 0.4f else 0.2f),
            modifier = Modifier
                .offset(1.dp, 1.dp)
                .size(handSize),
        )
        Icon(
            imageVector = Icons.Default.TouchApp,
            contentDescription = null,
            tint = handColor,
            modifier = Modifier.size(handSize),
        )
    }
}

@Composable
private fun TutorialMiniBottomDock(
    onTrayPositioned: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = modifier.stackShiftSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
            elevation = 10.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.dockCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.90f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.boardEmptyCellBorder.copy(alpha = 0.68f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            uiColors.launchGlow.copy(alpha = 0.16f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 10.dp, vertical = 10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(GameUiShapeTokens.surfaceCorner))
                    .background(uiColors.panelMuted.copy(alpha = 0.48f))
                    .border(
                        width = 1.dp,
                        color = uiColors.boardEmptyCellBorder.copy(alpha = 0.78f),
                        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
                    )
                    .onGloballyPositioned { coordinates ->
                        onTrayPositioned(coordinates.boundsInRoot())
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(18.dp)
                        .clip(RoundedCornerShape(GameUiShapeTokens.chipCorner))
                        .background(uiColors.launchGlow.copy(alpha = 0.10f))
                        .border(
                            width = 1.dp,
                            color = uiColors.boardEmptyCellBorder.copy(alpha = 0.46f),
                            shape = RoundedCornerShape(GameUiShapeTokens.chipCorner),
                        ),
                )
            }
        }
    }
}

@Composable
private fun TutorialSection(
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = Modifier.stackShiftSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
            elevation = 5.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.gameSurface.copy(alpha = 0.84f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.72f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = uiColors.subtitle,
            )
            content()
        }
    }
}

@Composable
private fun TutorialPieceCard(
    title: String,
    piece: Piece,
    alpha: Float,
    modifier: Modifier = Modifier,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = modifier.stackShiftSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
            elevation = 5.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.panelMuted.copy(alpha = 0.76f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.68f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = uiColors.subtitle.copy(alpha = alpha.coerceIn(0f, 1f)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold,
            )
            PieceBlocks(
                piece = piece,
                cellSize = 22.dp,
                alpha = alpha,
            )
        }
    }
}

@Composable
private fun TutorialActionTile(
    tone: CellTone,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
) {
    val settings = LocalAppSettings.current
    val blockStyle = resolveBoardBlockStyle(settings.blockVisualStyle, settings.boardBlockStyleMode)
    val stylePulse = rememberBlockStylePulse(style = blockStyle)
    val iconTint = blockStyleIconTint(style = blockStyle)

    Column(
        modifier = modifier.widthIn(min = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            BlockCellPreview(
                tone = tone,
                palette = settings.blockColorPalette,
                style = blockStyle,
                size = 40.dp,
                pulse = stylePulse,
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = iconTint,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TutorialSpecialCard(
    tone: CellTone,
    special: SpecialBlockType,
    title: String,
    body: String,
) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = Modifier.stackShiftSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
            elevation = 5.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.panelMuted.copy(alpha = 0.78f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.70f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BlockCellPreview(
                tone = tone,
                palette = LocalAppSettings.current.blockColorPalette,
                style = resolveBoardBlockStyle(
                    LocalAppSettings.current.blockVisualStyle,
                    LocalAppSettings.current.boardBlockStyleMode
                ),
                size = 34.dp,
                special = special,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = uiColors.subtitle,
                )
            }
        }
    }
}

@Composable
private fun TutorialHintCard(text: String) {
    val uiColors = StackShiftThemeTokens.uiColors
    Card(
        modifier = Modifier.stackShiftSurfaceShadow(
            shape = RoundedCornerShape(GameUiShapeTokens.hintCorner),
            elevation = 5.dp,
        ),
        shape = RoundedCornerShape(GameUiShapeTokens.hintCorner),
        colors = CardDefaults.cardColors(containerColor = uiColors.metricCard.copy(alpha = 0.84f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.62f)),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun Rect.toLocalRect(hostRect: Rect): Rect {
    if (this == Rect.Zero || hostRect == Rect.Zero) return Rect.Zero
    return Rect(
        left - hostRect.left,
        top - hostRect.top,
        right - hostRect.left,
        bottom - hostRect.top,
    )
}

private fun GridPoint.toLocalTopLeft(
    boardRect: Rect,
    cellSizePx: Float,
): Offset = Offset(
    x = boardRect.left + (column * cellSizePx),
    y = boardRect.top + (row * cellSizePx),
)

@Preview(name = "Tutorial - Intro", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialIntroStepPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 0,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - Systems", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialSystemsStepPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 1,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - Specials", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialSpecialsStepPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 2,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - Ready", widthDp = 412, heightDp = 915)
@Composable
private fun TutorialReadyStepPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameTutorialScreen(
            initialPage = 3,
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - Full Screen", widthDp = 412, heightDp = 915)
@Composable
private fun GameTutorialScreenPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameTutorialScreen(
            onBack = {},
            onFinish = {},
        )
    }
}

@Preview(name = "Tutorial - Full Screen (Start)", widthDp = 412, heightDp = 915)
@Composable
private fun GameTutorialScreenStartPreview() {
    StackShiftTheme(settings = AppSettings()) {
        GameTutorialScreen(
            onBack = {},
            onFinish = {},
        )
    }
}



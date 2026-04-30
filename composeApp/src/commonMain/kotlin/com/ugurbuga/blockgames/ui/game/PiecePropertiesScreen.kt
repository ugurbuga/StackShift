package com.ugurbuga.blockgames.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ugurbuga.blockgames.game.model.CellTone
import com.ugurbuga.blockgames.game.model.Piece
import com.ugurbuga.blockgames.game.model.SpecialBlockType
import com.ugurbuga.blockgames.telemetry.AppTelemetry
import com.ugurbuga.blockgames.telemetry.LogScreen
import com.ugurbuga.blockgames.telemetry.NoOpAppTelemetry
import com.ugurbuga.blockgames.telemetry.TelemetryScreenNames
import com.ugurbuga.blockgames.ui.theme.GameUiShapeTokens
import com.ugurbuga.blockgames.ui.theme.BlockGamesThemeTokens
import com.ugurbuga.blockgames.ui.theme.blockGamesSurfaceShadow
import org.jetbrains.compose.resources.stringResource
import blockgames.composeapp.generated.resources.Res
import blockgames.composeapp.generated.resources.piece_properties_active
import blockgames.composeapp.generated.resources.piece_properties_cells
import blockgames.composeapp.generated.resources.piece_properties_kind
import blockgames.composeapp.generated.resources.piece_properties_none
import blockgames.composeapp.generated.resources.piece_properties_size
import blockgames.composeapp.generated.resources.piece_properties_special
import blockgames.composeapp.generated.resources.piece_properties_title
import blockgames.composeapp.generated.resources.piece_properties_unlock_level
import blockgames.composeapp.generated.resources.piece_size_format
import blockgames.composeapp.generated.resources.queue_empty
import blockgames.composeapp.generated.resources.special_column_clearer
import blockgames.composeapp.generated.resources.special_ghost
import blockgames.composeapp.generated.resources.special_heavy
import blockgames.composeapp.generated.resources.special_row_clearer

@Composable
fun PiecePropertiesScreen(
    modifier: Modifier = Modifier,
    telemetry: AppTelemetry = NoOpAppTelemetry,
    activePiece: Piece?,
    onBack: () -> Unit,
) {
    LogScreen(telemetry, TelemetryScreenNames.PieceProperties)
    val uiColors = BlockGamesThemeTokens.uiColors
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Card(
                modifier = Modifier.blockGamesSurfaceShadow(
                    shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                    elevation = 10.dp,
                ),
                shape = RoundedCornerShape(GameUiShapeTokens.panelCorner),
                colors = CardDefaults.cardColors(containerColor = uiColors.panel),
                border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.panelStroke),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    TopBarActionBlockButton(
                        tone = CellTone.Cyan,
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(Res.string.piece_properties_title),
                        onClick = onBack,
                        size = 40.dp,
                    )
                    Text(
                        text = stringResource(Res.string.piece_properties_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(Res.string.piece_properties_active),
                        style = MaterialTheme.typography.labelLarge,
                        color = uiColors.subtitle,
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .blockGamesSurfaceShadow(
                        shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
                        elevation = 5.dp,
                    ),
                shape = RoundedCornerShape(GameUiShapeTokens.surfaceCorner),
                colors = CardDefaults.cardColors(containerColor = uiColors.panelMuted),
                border = androidx.compose.foundation.BorderStroke(1.dp, uiColors.panelStroke.copy(alpha = 0.72f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    if (activePiece == null) {
                        Text(
                            text = stringResource(Res.string.queue_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        )
                        return@Column
                    }

                    PropertyRow(
                        label = stringResource(Res.string.piece_properties_kind),
                        value = activePiece.kind.name,
                    )
                    PropertyRow(
                        label = stringResource(Res.string.piece_properties_size),
                        value = stringResource(Res.string.piece_size_format, activePiece.width, activePiece.height),
                    )

                    val specialLabel = if (activePiece.special == SpecialBlockType.None) {
                        stringResource(Res.string.piece_properties_none)
                    } else {
                        resolveSpecialLabel(activePiece.special)
                    }
                    PropertyRow(
                        label = stringResource(Res.string.piece_properties_special),
                        value = specialLabel,
                    )

                    PropertyRow(
                        label = stringResource(Res.string.piece_properties_cells),
                        value = activePiece.cells.size.toString(),
                    )

                    PropertyRow(
                        label = stringResource(Res.string.piece_properties_unlock_level),
                        value = activePiece.kind.unlockLevel.toString(),
                    )
                }
            }
        }
    }
}

@Composable
private fun PropertyRow(
    label: String,
    value: String,
) {
    val uiColors = BlockGamesThemeTokens.uiColors
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = uiColors.subtitle,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        fontWeight = FontWeight.Medium,
    )
}

@Composable
private fun resolveSpecialLabel(type: SpecialBlockType): String {
    return when (type) {
        SpecialBlockType.None -> stringResource(Res.string.piece_properties_none)
        SpecialBlockType.ColumnClearer -> stringResource(Res.string.special_column_clearer)
        SpecialBlockType.RowClearer -> stringResource(Res.string.special_row_clearer)
        SpecialBlockType.Ghost -> stringResource(Res.string.special_ghost)
        SpecialBlockType.Heavy -> stringResource(Res.string.special_heavy)
    }
}

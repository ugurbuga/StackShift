package com.ugurbuga.stackshift.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.game.model.Piece
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.piece_properties_active
import stackshift.composeapp.generated.resources.piece_properties_cells
import stackshift.composeapp.generated.resources.piece_properties_kind
import stackshift.composeapp.generated.resources.piece_properties_none
import stackshift.composeapp.generated.resources.piece_properties_size
import stackshift.composeapp.generated.resources.piece_properties_special
import stackshift.composeapp.generated.resources.piece_properties_title
import stackshift.composeapp.generated.resources.piece_properties_unlock_level
import stackshift.composeapp.generated.resources.piece_size_format
import stackshift.composeapp.generated.resources.queue_empty
import stackshift.composeapp.generated.resources.special_column_clearer
import stackshift.composeapp.generated.resources.special_ghost
import stackshift.composeapp.generated.resources.special_heavy
import stackshift.composeapp.generated.resources.special_row_clearer

@Composable
fun PiecePropertiesScreen(
    modifier: Modifier = Modifier,
    activePiece: Piece?,
    onBack: () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF070B14),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10192A).copy(alpha = 0.94f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.piece_properties_title),
                            tint = Color.White,
                        )
                    }
                    Text(
                        text = stringResource(Res.string.piece_properties_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(Res.string.piece_properties_active),
                        style = MaterialTheme.typography.labelLarge,
                        color = Color(0xFFC7D0EC),
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    if (activePiece == null) {
                        Text(
                            text = stringResource(Res.string.queue_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
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
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = Color(0xFFC7D0EC),
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
        color = Color.White,
        maxLines = 1,
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

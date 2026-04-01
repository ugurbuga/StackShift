package com.ugurbuga.stackshift.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ugurbuga.stackshift.game.model.CellTone
import com.ugurbuga.stackshift.game.model.SpecialBlockType
import org.jetbrains.compose.resources.stringResource
import stackshift.composeapp.generated.resources.Res
import stackshift.composeapp.generated.resources.block_properties_column_clearer_desc
import stackshift.composeapp.generated.resources.block_properties_column_clearer_title
import stackshift.composeapp.generated.resources.block_properties_dot_desc
import stackshift.composeapp.generated.resources.block_properties_ghost_desc
import stackshift.composeapp.generated.resources.block_properties_ghost_title
import stackshift.composeapp.generated.resources.block_properties_heavy_desc
import stackshift.composeapp.generated.resources.block_properties_heavy_title
import stackshift.composeapp.generated.resources.block_properties_normal_desc
import stackshift.composeapp.generated.resources.block_properties_normal_title
import stackshift.composeapp.generated.resources.block_properties_row_clearer_desc
import stackshift.composeapp.generated.resources.block_properties_row_clearer_title
import stackshift.composeapp.generated.resources.block_properties_select_hint
import stackshift.composeapp.generated.resources.block_properties_title

private val BlockSampleSize = 78.dp
private val BlockCornerRadius = 18.dp
private val BlockBorderWidth = 2.dp
private val BlockDotSize = 10.dp

@Composable
fun BlockPropertiesScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit,
) {
    var selected by remember { mutableStateOf(SpecialBlockType.None) }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.block_properties_title),
                            tint = Color.White,
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(Res.string.block_properties_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(Res.string.block_properties_select_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC7D0EC),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Card(
                    modifier = Modifier.width(132.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BlockSample(
                            tone = sampleToneFor(selected),
                            special = selected,
                        )
                        BlockTitleAndDesc(special = selected)
                        DotLegend()
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SpecialBlockType.entries.forEach { type ->
                        BlockTypeRow(
                            special = type,
                            selected = type == selected,
                            onClick = { selected = type },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DotLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(99.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(BlockDotSize)
                    .background(color = Color(0xFFFFD166), shape = RoundedCornerShape(99.dp)),
            )
        }
        Text(
            text = stringResource(Res.string.block_properties_dot_desc),
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFC7D0EC),
        )
    }
}

@Composable
private fun BlockTypeRow(
    special: SpecialBlockType,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) {
        Color(0xFF1A2A46).copy(alpha = 0.85f)
    } else {
        Color.White.copy(alpha = 0.06f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            BlockMiniIcon(tone = sampleToneFor(special), special = special)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resolveBlockTitle(special),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = resolveBlockDesc(special),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFC7D0EC),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BlockSample(
    tone: CellTone,
    special: SpecialBlockType,
) {
    Box(
        modifier = Modifier
            .size(BlockSampleSize)
            .background(color = tone.color().copy(alpha = 0.95f), shape = RoundedCornerShape(BlockCornerRadius))
            .then(
                if (special == SpecialBlockType.None) {
                    Modifier
                } else {
                    Modifier.border(
                        width = BlockBorderWidth,
                        color = specialColor(special).copy(alpha = 0.65f),
                        shape = RoundedCornerShape(BlockCornerRadius),
                    )
                },
            ),
        contentAlignment = Alignment.TopEnd,
    ) {
        if (special != SpecialBlockType.None) {
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .size(BlockDotSize)
                    .background(color = specialColor(special).copy(alpha = 0.9f), shape = RoundedCornerShape(99.dp)),
            )
        }
    }
}

@Composable
private fun BlockMiniIcon(
    tone: CellTone,
    special: SpecialBlockType,
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(color = tone.color().copy(alpha = 0.92f), shape = RoundedCornerShape(10.dp))
            .then(
                if (special == SpecialBlockType.None) {
                    Modifier
                } else {
                    Modifier.border(
                        width = 1.5.dp,
                        color = specialColor(special).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp),
                    )
                },
            ),
        contentAlignment = Alignment.TopEnd,
    ) {
        if (special != SpecialBlockType.None) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(6.dp)
                    .background(color = specialColor(special).copy(alpha = 0.9f), shape = RoundedCornerShape(99.dp)),
            )
        }
    }
}

@Composable
private fun BlockTitleAndDesc(special: SpecialBlockType) {
    Text(
        text = resolveBlockTitle(special),
        style = MaterialTheme.typography.titleMedium,
        color = Color.White,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
    Text(
        text = resolveBlockDesc(special),
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFFC7D0EC),
    )
}

@Composable
private fun resolveBlockTitle(special: SpecialBlockType): String {
    return when (special) {
        SpecialBlockType.None -> stringResource(Res.string.block_properties_normal_title)
        SpecialBlockType.ColumnClearer -> stringResource(Res.string.block_properties_column_clearer_title)
        SpecialBlockType.RowClearer -> stringResource(Res.string.block_properties_row_clearer_title)
        SpecialBlockType.Ghost -> stringResource(Res.string.block_properties_ghost_title)
        SpecialBlockType.Heavy -> stringResource(Res.string.block_properties_heavy_title)
    }
}

@Composable
private fun resolveBlockDesc(special: SpecialBlockType): String {
    return when (special) {
        SpecialBlockType.None -> stringResource(Res.string.block_properties_normal_desc)
        SpecialBlockType.ColumnClearer -> stringResource(Res.string.block_properties_column_clearer_desc)
        SpecialBlockType.RowClearer -> stringResource(Res.string.block_properties_row_clearer_desc)
        SpecialBlockType.Ghost -> stringResource(Res.string.block_properties_ghost_desc)
        SpecialBlockType.Heavy -> stringResource(Res.string.block_properties_heavy_desc)
    }
}

private fun sampleToneFor(special: SpecialBlockType): CellTone {
    return when (special) {
        SpecialBlockType.None -> CellTone.Cyan
        SpecialBlockType.ColumnClearer -> CellTone.Emerald
        SpecialBlockType.RowClearer -> CellTone.Gold
        SpecialBlockType.Ghost -> CellTone.Violet
        SpecialBlockType.Heavy -> CellTone.Coral
    }
}

private fun specialColor(type: SpecialBlockType): Color {
    return when (type) {
        SpecialBlockType.ColumnClearer -> Color(0xFF57E389)
        SpecialBlockType.RowClearer -> Color(0xFFFFD166)
        SpecialBlockType.Ghost -> Color(0xFF9B8CFF)
        SpecialBlockType.Heavy -> Color(0xFFFF7A90)
        SpecialBlockType.None -> Color.Transparent
    }
}

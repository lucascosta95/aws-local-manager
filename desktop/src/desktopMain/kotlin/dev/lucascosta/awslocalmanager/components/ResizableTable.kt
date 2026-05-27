@file:OptIn(ExperimentalFoundationApi::class)

package dev.lucascosta.awslocalmanager.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import java.awt.Cursor

private val HANDLE_WIDTH = 4.dp
private val MIN_COL_WIDTH = 60.dp
private const val TOOLTIP_DELAY_MS = 400

data class TableColumn(
    val header: String,
    val initialWidthFraction: Float,
    val minWidthDp: Dp = MIN_COL_WIDTH,
)

@Composable
fun ResizableTable(
    columns: List<TableColumn>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val density = LocalDensity.current
    val colWidths =
        remember(columns) {
            mutableStateListOf(*Array(columns.size) { 0.dp })
        }
    val tableWidthState = remember { androidx.compose.runtime.mutableStateOf(0) }

    Box(
        modifier =
            modifier.onSizeChanged { size ->
                val totalPx = size.width
                if (totalPx > 0 && tableWidthState.value != totalPx) {
                    tableWidthState.value = totalPx
                    val totalDp = with(density) { totalPx.toDp() }
                    val handleSpace = HANDLE_WIDTH * (columns.size - 1)
                    val usable = totalDp - handleSpace
                    columns.forEachIndexed { i, col ->
                        colWidths[i] = maxOf(col.minWidthDp, usable * col.initialWidthFraction)
                    }
                }
            },
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            stickyHeader {
                TableHeaderRow(columns = columns, colWidths = colWidths)
            }
            itemsIndexed(rows) { rowIndex, row ->
                TableDataRow(
                    row = row,
                    columns = columns,
                    colWidths = colWidths,
                    isOdd = rowIndex % 2 != 0,
                )
            }
        }
    }
}

@Composable
private fun TableHeaderRow(
    columns: List<TableColumn>,
    colWidths: MutableList<Dp>,
) {
    val density = LocalDensity.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        columns.forEachIndexed { i, col ->
            val width = colWidths.getOrElse(i) { 0.dp }
            if (width > 0.dp) {
                Text(
                    col.header,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(width).padding(horizontal = 10.dp, vertical = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (i < columns.lastIndex) {
                ResizeHandle(
                    onDrag = { delta ->
                        val deltaDp = with(density) { delta.toDp() }
                        val newLeft = maxOf(col.minWidthDp, colWidths[i] + deltaDp)
                        val nextCol = columns[i + 1]
                        val spill = newLeft - colWidths[i]
                        val newRight = maxOf(nextCol.minWidthDp, colWidths[i + 1] - spill)
                        colWidths[i] = newLeft
                        colWidths[i + 1] = newRight
                    },
                )
            }
        }
    }
}

@Composable
private fun TableDataRow(
    row: List<String>,
    columns: List<TableColumn>,
    colWidths: MutableList<Dp>,
    isOdd: Boolean,
) {
    val hoverSource = remember { MutableInteractionSource() }
    val isHovered by hoverSource.collectIsHoveredAsState()

    val rowBackground =
        when {
            isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            isOdd -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            else -> MaterialTheme.colorScheme.surface
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(rowBackground)
                .hoverable(hoverSource),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        columns.forEachIndexed { i, col ->
            val width = colWidths.getOrElse(i) { 0.dp }
            if (width > 0.dp) {
                val cellValue = row.getOrElse(i) { "" }
                TooltipArea(
                    tooltip = {
                        if (cellValue.isNotBlank()) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                tonalElevation = 4.dp,
                                shadowElevation = 4.dp,
                            ) {
                                Text(
                                    cellValue,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                    },
                    delayMillis = TOOLTIP_DELAY_MS,
                    tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
                    modifier = Modifier.width(width),
                ) {
                    Text(
                        cellValue,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (i < columns.lastIndex) {
                Box(modifier = Modifier.width(HANDLE_WIDTH).fillMaxHeight())
            }
        }
    }
}

@Composable
private fun ResizeHandle(onDrag: (Float) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isDragging = remember { androidx.compose.runtime.mutableStateOf(false) }

    val handleColor =
        when {
            isDragging.value -> MaterialTheme.colorScheme.primary
            isHovered -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0f)
        }

    Box(
        modifier =
            Modifier
                .width(HANDLE_WIDTH)
                .fillMaxHeight()
                .background(handleColor)
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta -> onDrag(delta) },
                    onDragStarted = { isDragging.value = true },
                    onDragStopped = { isDragging.value = false },
                ),
    )
}

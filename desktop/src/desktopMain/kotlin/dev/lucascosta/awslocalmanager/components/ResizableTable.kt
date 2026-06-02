@file:OptIn(ExperimentalFoundationApi::class)

package dev.lucascosta.awslocalmanager.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import java.awt.Cursor

private val HANDLE_WIDTH = 6.dp
private val MIN_COL_WIDTH = 60.dp
private val ACTION_COLUMN_WIDTH = 32.dp
private val SCROLLBAR_RESERVED = 12.dp
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
    onRowClick: ((Int) -> Unit)? = null,
    selectedRowIndex: Int? = null,
    emptyMessage: String? = null,
    onRowCopy: ((rowIndex: Int) -> String)? = null,
) {
    val density = LocalDensity.current
    val columnWidths = remember(columns) { List(columns.size) { -1f }.toMutableStateList() }
    val scrollState = rememberScrollState()
    val hasRowCopy = onRowCopy != null

    BoxWithConstraints(modifier = modifier) {
        val totalWidthPx = with(density) { maxWidth.toPx() }

        if (columnWidths.all { it < 0f } && totalWidthPx > 0f) {
            val usableWidthPx =
                if (hasRowCopy) {
                    val handleTotalPx = (columns.size - 1) * with(density) { HANDLE_WIDTH.toPx() }
                    totalWidthPx -
                        with(density) { ACTION_COLUMN_WIDTH.toPx() } -
                        with(density) { SCROLLBAR_RESERVED.toPx() } -
                        handleTotalPx
                } else {
                    totalWidthPx
                }
            columns.forEachIndexed { i, col ->
                columnWidths[i] = usableWidthPx * col.initialWidthFraction
            }
        }

        if (columnWidths.all { it >= 0f }) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().padding(end = SCROLLBAR_RESERVED)) {
                    TableHeaderRow(
                        columns = columns,
                        columnWidths = columnWidths,
                        hasRowCopy = hasRowCopy,
                        onResize = { index, delta ->
                            resizeColumns(columnWidths, index, delta, columns, density)
                        },
                    )
                    HorizontalDivider()
                    Box(modifier = Modifier.weight(1f)) {
                        if (rows.isEmpty() && emptyMessage != null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = emptyMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                            ) {
                                rows.forEachIndexed { rowIndex, row ->
                                    TableDataRow(
                                        values = row,
                                        columnWidths = columnWidths,
                                        isOdd = rowIndex % 2 != 0,
                                        isSelected = selectedRowIndex == rowIndex,
                                        onClick = onRowClick?.let { cb -> { cb(rowIndex) } },
                                        rowCopyText = onRowCopy?.invoke(rowIndex),
                                    )
                                    HorizontalDivider(
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    )
                                }
                            }
                        }
                    }
                }

                if (rows.isNotEmpty() || emptyMessage == null) {
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(scrollState),
                        modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 2.dp),
                    )
                }
            }
        }
    }
}

private fun resizeColumns(
    columnWidths: MutableList<Float>,
    index: Int,
    delta: Float,
    columns: List<TableColumn>,
    density: Density,
) {
    val rightIndex = index + 1
    if (rightIndex >= columnWidths.size) return

    val minLeft = with(density) { columns[index].minWidthDp.toPx() }
    val minRight = with(density) { columns[rightIndex].minWidthDp.toPx() }

    val maxDelta = columnWidths[rightIndex] - minRight
    val minDelta = -(columnWidths[index] - minLeft)
    val clampedDelta = delta.coerceIn(minDelta, maxDelta)

    columnWidths[index] += clampedDelta
    columnWidths[rightIndex] -= clampedDelta
}

@Composable
private fun TableHeaderRow(
    columns: List<TableColumn>,
    columnWidths: List<Float>,
    hasRowCopy: Boolean,
    onResize: (index: Int, delta: Float) -> Unit,
) {
    val density = LocalDensity.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        columns.forEachIndexed { index, column ->
            val widthPx = columnWidths.getOrElse(index) { 100f }
            val widthDp = with(density) { widthPx.toDp() }.coerceAtLeast(1.dp)

            Box(
                modifier =
                    Modifier
                        .width(widthDp)
                        .fillMaxHeight()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = column.header,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (index < columns.size - 1) {
                ResizeHandle(onDrag = { delta -> onResize(index, delta) })
            }
        }

        if (hasRowCopy) {
            Spacer(modifier = Modifier.width(ACTION_COLUMN_WIDTH).fillMaxHeight())
        }
    }
}

@Composable
private fun TableDataRow(
    values: List<String>,
    columnWidths: List<Float>,
    isOdd: Boolean,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    rowCopyText: String? = null,
) {
    val density = LocalDensity.current
    var isHovered by remember { mutableStateOf(false) }

    val background by animateColorAsState(
        targetValue =
            when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                isHovered -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                isOdd -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            },
        animationSpec = tween(100),
    )

    val clickModifier =
        if (onClick != null) {
            Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
        } else {
            Modifier
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(background)
                .then(clickModifier)
                .onHover { isHovered = it }
                .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        values.forEachIndexed { index, value ->
            val widthPx = columnWidths.getOrElse(index) { 100f }
            val widthDp = with(density) { widthPx.toDp() }.coerceAtLeast(1.dp)

            TooltipArea(
                tooltip = {
                    if (value.isNotBlank()) {
                        Surface(
                            modifier = Modifier.widthIn(max = 480.dp),
                            shape = RoundedCornerShape(6.dp),
                            tonalElevation = 4.dp,
                            shadowElevation = 4.dp,
                        ) {
                            Text(
                                text = value,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            )
                        }
                    }
                },
                delayMillis = TOOLTIP_DELAY_MS,
                tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 16.dp)),
                modifier = Modifier.width(widthDp),
            ) {
                Text(
                    text = value,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (index < values.size - 1) {
                Spacer(modifier = Modifier.width(HANDLE_WIDTH).fillMaxHeight())
            }
        }

        if (rowCopyText != null) {
            Box(
                modifier = Modifier.width(ACTION_COLUMN_WIDTH).fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                if (isHovered) {
                    CopyButton(textToCopy = rowCopyText)
                }
            }
        }
    }
}

@Composable
private fun ResizeHandle(onDrag: (Float) -> Unit) {
    var isHovered by remember { mutableStateOf(false) }
    var isDragging by remember { mutableStateOf(false) }

    val color by animateColorAsState(
        targetValue =
            when {
                isDragging -> MaterialTheme.colorScheme.primary
                isHovered -> MaterialTheme.colorScheme.outlineVariant
                else -> Color.Transparent
            },
        animationSpec = tween(150),
    )

    Box(
        modifier =
            Modifier
                .width(HANDLE_WIDTH)
                .fillMaxHeight()
                .background(color)
                .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                .onHover { isHovered = it }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x)
                        },
                    )
                },
    )
}

private fun Modifier.onHover(onHover: (Boolean) -> Unit): Modifier =
    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                    PointerEventType.Enter -> onHover(true)
                    PointerEventType.Exit -> onHover(false)
                    else -> {}
                }
            }
        }
    }

package com.ivarna.deviceinsight.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

/**
 * A robust, dependency-free reorderable list implementation.
 * 
 * Features:
 * - Prevents jitter by keeping the underlying list static during drag and only manipulating offsets.
 * - Live reordering feedback (items yield to the dragged item).
 * - "Premium" feel animations.
 * - Supports custom content via [itemContent].
 * 
 * Requirements:
 * - Items should be of roughly uniform height for the best experience with this logic.
 *
 * @param items The list of items to display.
 * @param onReorder Callback when an item is dropped in a new position. Update your source of truth here.
 * @param modifier Modifier for the container.
 * @param itemContent Composable content for each item.
 */
@Composable
fun <T> ReorderableList(
    items: List<T>,
    onReorder: (fromIndex: Int, toIndex: Int) -> Unit,
    modifier: Modifier = Modifier,
    key: ((T) -> Any)? = null,
    itemContent: @Composable (item: T, isDragging: Boolean) -> Unit
) {
    // Current drag state
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    
    // We need to know the height of an item to calculate the swap target index.
    // Assuming uniform height as per "Requirements".
    var itemHeightPx by remember { mutableIntStateOf(0) }

    Column(
        modifier = modifier
    ) {
        if (items.isEmpty()) return@Column

        items.forEachIndexed { index, item ->
            // Use user provided key or index
            val itemKey = key?.invoke(item) ?: index
            
            key(itemKey) {
                // Determine if this item is currently being dragged
                val isDragging = draggingItemIndex == index

                // Calculate the visual offset for "live" reordering feedback
                // This is the core "No Jitter" logic: passing visual offsets instead of mutating the list
                val targetOffset = remember(draggingItemIndex, dragOffsetY, index, itemHeightPx) {
                    calculateOffset(
                        index = index,
                        draggingItemIndex = draggingItemIndex,
                        dragOffsetY = dragOffsetY,
                        itemHeight = itemHeightPx
                    )
                }

                // Animate the offset for smooth "shifting" of non-dragged items
                val animatedOffset by animateIntAsState(
                    targetValue = targetOffset,
                    animationSpec = tween(durationMillis = 150),
                    label = "item_offset"
                )

                // Dragged item needs to follow the finger exactly (no animation lag on the drag itself)
                 val actualOffset = if (isDragging) {
                    dragOffsetY.roundToInt()
                } else {
                    animatedOffset
                }

                // Apply Z-index to lift the dragged item
                val zIndex = if (isDragging) 1f else 0f
                val scale by animateFloatAsState(if (isDragging) 1.05f else 1f, label = "scale")
                val shadowElevation = if(isDragging) 8.dp else 0.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(zIndex)
                        .offset { IntOffset(0, actualOffset) }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .onGloballyPositioned {
                            if (itemHeightPx == 0 && !isDragging) {
                                itemHeightPx = it.size.height
                            }
                        }
                        .pointerInput(index, items) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { 
                                    draggingItemIndex = index
                                    dragOffsetY = 0f
                                },
                                onDragEnd = {
                                    if (draggingItemIndex != null) {
                                        val currentIndex = draggingItemIndex!!
                                        val currentDragY = dragOffsetY
                                        
                                        // Calculate final target index based on total drag distance
                                        if (itemHeightPx > 0) {
                                            val spacesMoved = (currentDragY / itemHeightPx).roundToInt()
                                            val targetIndex = (currentIndex + spacesMoved).coerceIn(0, items.lastIndex)
                                            
                                            // Only callback if changed
                                            if (currentIndex != targetIndex) {
                                                onReorder(currentIndex, targetIndex)
                                            }
                                        }
                                    }
                                    // Reset state
                                    draggingItemIndex = null
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggingItemIndex = null
                                    dragOffsetY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y
                                }
                            )
                        }
                ) {
                    itemContent(item, isDragging)
                }
            }
        }
    }
}

/**
 * Calculates the visual Y offset for an item based on the current drag state.
 */
private fun calculateOffset(
    index: Int,
    draggingItemIndex: Int?,
    dragOffsetY: Float,
    itemHeight: Int
): Int {
    if (draggingItemIndex == null || itemHeight == 0) return 0
    
    // The dragged item is handled separately via raw offset
    if (index == draggingItemIndex) return 0

    // How many "slots" has the dragged item moved?
    val slotsShifted = (dragOffsetY / itemHeight).roundToInt()
    val virtualTargetIndex = (draggingItemIndex + slotsShifted)

    // Check if this item (at 'index') is in the path of the shift
    return when {
        // Dragging DOWN: Item is below start, but above or at target -> move UP
        slotsShifted > 0 && index > draggingItemIndex && index <= virtualTargetIndex -> -itemHeight
        
        // Dragging UP: Item is above start, but below or at target -> move DOWN
        slotsShifted < 0 && index < draggingItemIndex && index >= virtualTargetIndex -> itemHeight
        
        else -> 0
    }
}

package app.orionmd.management.ui.lock

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * A classic 3x3 Android-style pattern lock. Dots are indexed 0-8, left-to-right/top-to-bottom.
 * [onPatternComplete] fires once the user lifts their finger, with the ordered list of visited
 * dot indices (minimum 4 dots, matching standard Android pattern-lock rules).
 */
@Composable
fun PatternLockView(
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    idleColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
    errorColor: Color = MaterialTheme.colorScheme.error,
    showError: Boolean = false,
    onPatternComplete: (List<Int>) -> Unit
) {
    var visitedDots by remember { mutableStateOf(listOf<Int>()) }
    var currentPos by remember { mutableStateOf<Offset?>(null) }
    var dotCenters by remember { mutableStateOf(listOf<Offset>()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(24.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            visitedDots = listOfNotNull(nearestDot(offset, dotCenters, size.width / 6.5f))
                            currentPos = offset
                        },
                        onDrag = { change, _ ->
                            currentPos = change.position
                            nearestDot(change.position, dotCenters, size.width / 6.5f)?.let { dot ->
                                if (dot !in visitedDots) visitedDots = visitedDots + dot
                            }
                        },
                        onDragEnd = {
                            if (visitedDots.size >= 4) onPatternComplete(visitedDots)
                            visitedDots = listOf()
                            currentPos = null
                        },
                        onDragCancel = {
                            visitedDots = listOf()
                            currentPos = null
                        }
                    )
                }
        ) {
            val cell = min(size.width, size.height) / 3f
            val radius = cell * 0.12f
            val centers = (0 until 9).map { index ->
                val row = index / 3
                val col = index % 3
                Offset(cell * col + cell / 2f, cell * row + cell / 2f)
            }
            dotCenters = centers

            val lineColor = if (showError) errorColor else activeColor

            // connecting lines
            for (i in 0 until visitedDots.size - 1) {
                drawLine(
                    color = lineColor,
                    start = centers[visitedDots[i]],
                    end = centers[visitedDots[i + 1]],
                    strokeWidth = 10f
                )
            }
            if (visitedDots.isNotEmpty() && currentPos != null) {
                drawLine(
                    color = lineColor,
                    start = centers[visitedDots.last()],
                    end = currentPos!!,
                    strokeWidth = 10f
                )
            }

            // dots
            centers.forEachIndexed { index, center ->
                val visited = index in visitedDots
                drawCircle(
                    color = if (visited) lineColor else idleColor,
                    radius = if (visited) radius * 1.15f else radius,
                    center = center
                )
                drawCircle(
                    color = if (visited) lineColor else idleColor,
                    radius = radius * 2.2f,
                    center = center,
                    style = Stroke(width = 3f)
                )
            }
        }
    }
}

private fun nearestDot(point: Offset, centers: List<Offset>, touchRadius: Float): Int? {
    if (centers.isEmpty()) return null
    var closestIndex = -1
    var closestDistSq = Float.MAX_VALUE
    centers.forEachIndexed { index, center ->
        val dx = point.x - center.x
        val dy = point.y - center.y
        val distSq = dx * dx + dy * dy
        if (distSq < closestDistSq) {
            closestDistSq = distSq
            closestIndex = index
        }
    }
    return if (closestDistSq <= touchRadius * touchRadius) closestIndex else null
}

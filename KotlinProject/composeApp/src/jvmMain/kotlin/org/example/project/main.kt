package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.lang.Float.min

// --- Constants ---
val SPIRAL_COLORS = listOf(
    Color.Red, Color.Blue, Color.Green, Color(0xFF800080), // Purple
    Color(0xFFFFA500), // Orange
    Color.Cyan, Color.Magenta, Color.Yellow
)

enum class SpiralDirection {
    START, // The very first square
    UP,    // Placed above previous
    LEFT,  // Placed left of previous
    DOWN,  // Placed below previous
    RIGHT  // Placed right of previous
}

// --- Data Models ---
data class FibonacciSquare(
    val index: Int,
    val value: Long,
    val rect: Rect,
    val color: Color,
    val direction: SpiralDirection
)

// --- Math Logic (The Business Layer) ---

fun generateFibonacci(n: Int): List<Long> {
    if (n <= 0) return emptyList()
    if (n == 1) return listOf(1L)
    val list = mutableListOf(1L, 1L)
    for (i in 2 until n) {
        list.add(list[i - 1] + list[i - 2])
    }
    return list
}

fun calculateGeometry(n: Int): List<FibonacciSquare> {
    val fibs = generateFibonacci(n)
    if (fibs.isEmpty()) return emptyList()

    val squares = mutableListOf<FibonacciSquare>()

    // 1. Square 0 (The Seed)
    val firstRect = Rect(0f, 0f, 1f, 1f)
    squares.add(
        FibonacciSquare(0, fibs[0], firstRect, SPIRAL_COLORS[0], SpiralDirection.START)
    )

    if (n > 1) {
        // 2. Square 1
        // In a standard spiral, the second '1' is placed ABOVE the first '1'.
        val prev = squares.last().rect
        val size = fibs[1].toFloat()

        // Align Bottom of New with Top of Old.
        val secondRect = Rect(
            left = prev.left,
            top = prev.top - size,
            right = prev.left + size,
            bottom = prev.top
        )

        squares.add(
            FibonacciSquare(1, fibs[1], secondRect, SPIRAL_COLORS[1], SpiralDirection.UP)
        )
    }

    // 3. Loop for the remaining squares (Index 2 to N)
    // Sequence from index 2 is: LEFT, DOWN, RIGHT, UP...
    for (i in 2 until n) {
        val size = fibs[i].toFloat()
        val prev = squares.last().rect

        // Determine the direction based on index
        val dirIndex = (i - 2) % 4
        val direction = when(dirIndex) {
            0 -> SpiralDirection.LEFT
            1 -> SpiralDirection.DOWN
            2 -> SpiralDirection.RIGHT
            else -> SpiralDirection.UP
        }

        var newRect: Rect

        when (direction) {
            SpiralDirection.LEFT -> {
                // Attach to the Left side of Prev
                // Align Top
                newRect = Rect(
                    left = prev.left - size,
                    top = prev.top,
                    right = prev.left,
                    bottom = prev.top + size
                )
            }
            SpiralDirection.DOWN -> {
                // Attach to Bottom of Prev
                // Align Left
                newRect = Rect(
                    left = prev.left,
                    top = prev.bottom,
                    right = prev.left + size,
                    bottom = prev.bottom + size
                )
            }
            SpiralDirection.RIGHT -> {
                // Attach to Right of Prev
                // Align Bottom
                newRect = Rect(
                    left = prev.right,
                    top = prev.bottom - size,
                    right = prev.right + size,
                    bottom = prev.bottom
                )
            }
            SpiralDirection.UP -> {
                // Attach to Top of Prev
                // Align Right
                newRect = Rect(
                    left = prev.right - size,
                    top = prev.top - size,
                    right = prev.right,
                    bottom = prev.top
                )
            }
            else -> throw IllegalStateException("Impossible direction")
        }

        squares.add(
            FibonacciSquare(
                i, fibs[i], newRect,
                SPIRAL_COLORS[i % SPIRAL_COLORS.size],
                direction
            )
        )
    }
    return squares
}

// --- UI Components ---
@Composable
fun App() {
    MaterialTheme {
        var squareCount by remember { mutableStateOf(5) }

        // Re-calculate geometry when squareCount changes
        val squares = remember(squareCount) { calculateGeometry(squareCount) }

        Row(Modifier.fillMaxSize()) {
            // Left: Canvas Area
            Box(Modifier.weight(1f).fillMaxHeight().background(Color.White)) {
                SpiralCanvas(squares)
            }

            // Right: Control Sidebar
            Card(elevation = 4.dp, modifier = Modifier.fillMaxHeight().width(250.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Control Squares", style = MaterialTheme.typography.h6)
                    Spacer(Modifier.height(16.dp))

                    Text("Squares = $squareCount")
                    Slider(
                        value = squareCount.toFloat(),
                        onValueChange = { squareCount = it.toInt() },
                        valueRange = 5f..15f,
                        steps = 10
                    )
                }
            }
        }
    }
}

@Composable
fun SpiralCanvas(squares: List<FibonacciSquare>) {
    Canvas(modifier = Modifier.fillMaxSize().padding(32.dp)) {
        if (squares.isEmpty()) return@Canvas

        // 1. Calculate Bounds (Same as before)
        val minX = squares.minOf { it.rect.left }
        val minY = squares.minOf { it.rect.top }
        val maxX = squares.maxOf { it.rect.right }
        val maxY = squares.maxOf { it.rect.bottom }

        val geoWidth = maxX - minX
        val geoHeight = maxY - minY

        // 2. Scale (Same as before)
        val scaleX = (size.width * 0.9f) / geoWidth
        val scaleY = (size.height * 0.9f) / geoHeight
        val scaleFactor = min(scaleX, scaleY)

        // 3. Centers (Same as before)
        val geoCenterX = minX + geoWidth / 2
        val geoCenterY = minY + geoHeight / 2
        val screenCenterX = size.width / 2
        val screenCenterY = size.height / 2

        // Helper function to map "World" coordinates to "Screen" pixels
        fun toScreen(x: Float, y: Float): Offset {
            val sx = screenCenterX + (x - geoCenterX) * scaleFactor
            val sy = screenCenterY + (y - geoCenterY) * scaleFactor
            return Offset(sx, sy)
        }

        // 4. Draw Loop (Manual Scaling)
        squares.forEach { sq ->
            // Calculate screen positions manually
            val topLeft = toScreen(sq.rect.left, sq.rect.top)
            val screenSize = sq.rect.width * scaleFactor

            // Draw Square
            // We can now use a constant stroke width (e.g., 2f) because we are drawing in Screen Pixels.
            drawRect(
                color = sq.color,
                topLeft = topLeft,
                size = Size(screenSize, screenSize),
                style = Stroke(width = 2f)
            )

            // --- Arc Logic ---
            var rawPivot: Offset
            var startAngle: Float
            val sweepAngle = -90f

            when (sq.direction) {
                SpiralDirection.START -> {
                    rawPivot = Offset(sq.rect.left, sq.rect.top)
                    startAngle = 90f
                }
                SpiralDirection.UP -> {
                    rawPivot = Offset(sq.rect.left, sq.rect.bottom)
                    startAngle = 0f
                }
                SpiralDirection.LEFT -> {
                    rawPivot = Offset(sq.rect.right, sq.rect.bottom)
                    startAngle = 270f
                }
                SpiralDirection.DOWN -> {
                    rawPivot = Offset(sq.rect.right, sq.rect.top)
                    startAngle = 180f
                }
                SpiralDirection.RIGHT -> {
                    rawPivot = Offset(sq.rect.left, sq.rect.top)
                    startAngle = 90f
                }
            }

            // Transform the pivot to screen coordinates
            val screenPivot = toScreen(rawPivot.x, rawPivot.y)
            val screenRadius = sq.rect.width * scaleFactor

            // Draw the arc
            drawArc(
                color = Color.Black,
                topLeft = Offset(screenPivot.x - screenRadius, screenPivot.y - screenRadius),
                size = Size(screenRadius * 2, screenRadius * 2),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(
                    width = 3f, // Constant 3 px width
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Fibonacci Spiral Visualizer",
        state = rememberWindowState(width = 1400.dp, height = 700.dp)
    ) {
        App()
    }
}
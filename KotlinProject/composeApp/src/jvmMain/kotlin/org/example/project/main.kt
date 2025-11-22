package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
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

// --- Data Models ---
data class FibonacciSquare(
    val index: Int,
    val value: Long,
    val rect: Rect, // Cartesian coordinates
    val color: Color,
    val arcStartAngle: Float
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

fun calculateGeometry(n: Int): List<Any> {
    val fibs = generateFibonacci(n)
    if (fibs.isEmpty()) return emptyList()

    val squares = mutableListOf<FibonacciSquare>()

    // Bounding box tracking [minX, minY, maxX, maxY]
    val bbox = floatArrayOf(0f, 0f, 1f, 1f)

    // First square at (0,0)
    squares.add(
        // _root_ide_package_.org.example.project.FibonacciSquare(
        FibonacciSquare(
            0, fibs[0],
            Rect(0f, 0f, 1f, 1f),
            SPIRAL_COLORS[0],
            0f
        )
    )

    if (n > 1) {
        // Second square above first: (0, 1)
        squares.add(
            // _root_ide_package_.org.example.project.FibonacciSquare(
            FibonacciSquare(
                1, fibs[1],
                Rect(0f, 1f, 1f, 2f),
                SPIRAL_COLORS[1],
                90f
            )
        )
        bbox[3] = 2f // Update maxY
    }

    // Pattern: LEFT, DOWN, RIGHT, UP
    // We map these to the growth direction relative to the bounding box
    for (i in 2 until n) {
        val size = fibs[i].toFloat()
        val direction = (i - 2) % 4

        var newRect: Rect
        var angle: Float

        when (direction) {
            0 -> { // LEFT
                val x = bbox[0] - size
                val y = bbox[1] // Align with bottom of bbox
                newRect = Rect(x, y, x + size, y + size)
                bbox[0] = x
                angle = 180f
            }

            1 -> { // DOWN
                val x = bbox[0] // Align with left of bbox
                val y = bbox[1] - size
                newRect = Rect(x, y, x + size, y + size)
                bbox[1] = y
                angle = 270f
            }

            2 -> { // RIGHT
                // val x = bbox[2]
                // val y = bbox[1]
                // Align with the bottom of bbox (actually needs check)
                // Correction based on spiral logic: Right usually aligns with Top of previous,
                // but let's stick to the bbox expansion logic.
                // Logic: Right side of bbox, aligned with the top of bbox (bbox[3])?
                // Re-reading Python logic:
                // RIGHT: x = bbox[2], y = bbox[1] (min_y)
                newRect = Rect(bbox[2], bbox[1], bbox[2] + size, bbox[1] + size)
                bbox[2] += size
                angle = 0f
            }

            else -> { // UP
                val x = bbox[0] // Align with left
                val y = bbox[3]
                newRect = Rect(x, y, x + size, y + size)
                bbox[3] += size
                angle = 90f
            }
        }

        squares.add(
            FibonacciSquare(
                i, fibs[i], newRect,
                SPIRAL_COLORS[i % SPIRAL_COLORS.size],
                angle
            )
        )
    }
    return squares
}

// --- UI Components ---
@Composable
fun App() {
    MaterialTheme {
        var squareCount by remember { mutableStateOf(10) }

        // Re-calculate geometry only when squareCount changes
        val squares = remember(squareCount) { calculateGeometry(squareCount) }

        Row(Modifier.fillMaxSize()) {
            // Left: Canvas Area
            Box(Modifier.weight(1f).fillMaxHeight().background(Color.White)) {
                SpiralCanvas(squares as List<FibonacciSquare>)
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
                        valueRange = 10f..14f,
                        steps = 3
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

        // 1. Calculate Bounds of the entire geometry
        val minX = squares.minOf { it.rect.left }
        val minY = squares.minOf { it.rect.top }
        val maxX = squares.maxOf { it.rect.right }
        val maxY = squares.maxOf { it.rect.bottom }

        val geoWidth = maxX - minX
        val geoHeight = maxY - minY
        println("Geo Bounds: $geoWidth x $geoHeight")

        // 2. Calculate Scale to fit the screen
        // Using 0.9f to leave a margin
        val scaleX = (size.width * 0.9f) / geoWidth
        val scaleY = (size.height * 0.9f) / geoHeight
        println("Scale X: $scaleX, Scale Y: $scaleY")

        val scaleFactor = min(scaleX, scaleY)
        println("Scale Factor: $scaleFactor")

        // 3. Center calculations
        // We want to center the specific geometry on the screen
        val geoCenterX = minX + geoWidth / 2
        val geoCenterY = minY + geoHeight / 2
        println("Geo Center: $geoCenterX, $geoCenterY")

        val screenCenterX = size.width / 2
        val screenCenterY = size.height / 2
        println("Screen Center: $screenCenterX, $screenCenterY")

        withTransform({
            // Center the drawing on screen
            translate(screenCenterX, screenCenterY)
            // scale(scaleFactor, -scaleFactor) // Flip Y to match Cartesian coords (Up is positive)
            translate(-geoCenterX, -geoCenterY)
        }) {
            squares.forEach { sq ->
                // Draw Square
                println(sq.value)
                drawRect(
                    color = sq.color,
                    topLeft = Offset(sq.rect.left, sq.rect.top),
                    size = Size(sq.rect.width, sq.rect.height),
                    style = Stroke(width = 2f / scaleFactor) // Keep the stroke constant visual width
                )

                // Draw Spiral Arc
                // Turtle draws quarter circles.
                // We need to define the arc within a 2x2 square (the circle bounds)
                // Logic: The arc connects two corners of the square.

                // Determine arc center based on direction/angle
                // Arc 0 (0deg): starts bottom-left, goes to bottom-right?
                // In Turtle, the standard start is right.
                // Let's assume standard Fibonacci spiral corners:
                // It connects opposite corners via a quarter circle.

                // Simple Arc Logic:
                // Determine which corner is the pivot.
                // Angle 0 (Right): Pivot is Top-Left (in standard Cartesian).
                // Angle 90 (Up): Pivot is Bottom-Left.
                // Angle 180 (Left): Pivot is Bottom-Right.
                // Angle 270 (Down): Pivot is Top-Right.

                var arcCenter = Offset.Zero
                var startAngle = 0f

                when (sq.arcStartAngle.toInt()) {
                    0 -> { // Growing Right
                        arcCenter = Offset(sq.rect.left, sq.rect.top + sq.rect.height) // Top Left
                        startAngle = 270f
                    }

                    90 -> { // Growing Up
                        arcCenter = Offset(sq.rect.left, sq.rect.top) // Bottom Left
                        startAngle = 0f
                    }

                    180 -> { // Growing Left
                        arcCenter = Offset(sq.rect.left + sq.rect.width, sq.rect.top) // Bottom Right
                        startAngle = 90f
                    }

                    270 -> { // Growing Down
                        arcCenter = Offset(sq.rect.left + sq.rect.width, sq.rect.top + sq.rect.height) // Top Right
                        startAngle = 180f
                    }
                }

                // Note: The start angles above are adjusted for the fact that we flipped Y-scale.
                // Normal Unit Circle (Standard Math): 0 is East, 90 is North.

                val radius = sq.rect.width
                drawArc(
                    color = Color(0xFFFFD700), // Gold
                    topLeft = Offset(arcCenter.x - radius, arcCenter.y - radius),
                    size = Size(radius * 2, radius * 2),
                    startAngle = startAngle,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(width = 4f / scaleFactor)
                )
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Fibonacci Spiral Visualizer (Compose)",
        state = rememberWindowState(width = 1200.dp, height = 900.dp)
    ) {
        App()
    }
}
package com.netdiag.tool.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.sqrt

/**
 * Smooth curve through every point using monotone cubic interpolation (Fritsch–Carlson).
 * Unlike a plain Catmull-Rom/midpoint curve it never overshoots, so a latency or speed graph
 * can't dip below zero or bulge above a peak between two samples.
 */
fun smoothLinePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path
    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    val n = points.size
    val secants = FloatArray(n - 1) { i ->
        val dx = points[i + 1].x - points[i].x
        if (dx == 0f) 0f else (points[i + 1].y - points[i].y) / dx
    }
    val tangents = FloatArray(n) { i ->
        when (i) {
            0 -> secants[0]
            n - 1 -> secants[n - 2]
            // Flat at local peaks / valleys keeps the curve from overshooting them
            else -> if (secants[i - 1] * secants[i] <= 0f) 0f else (secants[i - 1] + secants[i]) / 2f
        }
    }
    for (i in 0 until n - 1) {
        if (secants[i] == 0f) {
            tangents[i] = 0f
            tangents[i + 1] = 0f
        } else {
            val a = tangents[i] / secants[i]
            val b = tangents[i + 1] / secants[i]
            val s = a * a + b * b
            if (s > 9f) {
                val tau = 3f / sqrt(s)
                tangents[i] = tau * a * secants[i]
                tangents[i + 1] = tau * b * secants[i]
            }
        }
    }

    for (i in 0 until n - 1) {
        val p0 = points[i]
        val p1 = points[i + 1]
        val third = (p1.x - p0.x) / 3f
        path.cubicTo(
            p0.x + third, p0.y + tangents[i] * third,
            p1.x - third, p1.y - tangents[i + 1] * third,
            p1.x, p1.y
        )
    }
    return path
}

/** Closes [line] down to [baselineY] so it can be filled with a gradient under the curve. */
fun fillUnder(line: Path, points: List<Offset>, baselineY: Float): Path = Path().apply {
    addPath(line)
    lineTo(points.last().x, baselineY)
    lineTo(points.first().x, baselineY)
    close()
}

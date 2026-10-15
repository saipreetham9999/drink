package com.drink.app.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.drink.app.camera.domain.PoseFrame

@Composable
fun PoseOverlay(frame: PoseFrame?, modifier: Modifier) {
    Canvas(modifier) {
        if (frame == null || frame.width <= 0 || frame.height <= 0) return@Canvas
        val scale = minOf(size.width / frame.width, size.height / frame.height)
        val left = (size.width - frame.width * scale) / 2
        val top = (size.height - frame.height * scale) / 2
        fun position(index: Int): Offset? = frame.points.getOrNull(index)?.takeIf { it.confidence >= .7f }?.let {
            Offset(left + (if (frame.mirrored) 1 - it.x else it.x) * frame.width * scale, top + it.y * frame.height * scale)
        }
        for ((a, b) in listOf(9 to 10, 11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16)) {
            val start = position(a); val end = position(b)
            if (start != null && end != null) { drawLine(Color.Black, start, end, 7f); drawLine(Color.White, start, end, 3f) }
        }
        for (index in 9..16) position(index)?.let { drawCircle(Color.Black, 8f, it); drawCircle(Color.White, 5f, it) }
    }
}

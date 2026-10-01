package com.example.happyhunt.ui.map

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.happyhunt.domain.Category
import com.example.happyhunt.domain.Kind
import com.example.happyhunt.ui.theme.Hunt
import com.example.happyhunt.ui.theme.HuntColors
import com.example.happyhunt.ui.theme.HuntIcons
import kotlin.math.acos
import kotlin.math.ceil

/**
 * The map's markers, drawn from the app's own icons and colours: a round pin
 * per kind of place in its category's colour, and a taller teardrop for the
 * place that is selected. Named "pin-CAFE", "selected-CAFE" and so on, as the
 * map layers expect.
 */
@Composable
internal fun rememberMarkerImages(): Map<String, Bitmap> {
    val colors = Hunt.colors
    val density = LocalDensity.current
    val painters = Kind.entries.associateWith { rememberVectorPainter(HuntIcons.forKind(it)) }
    return remember(colors, density) {
        buildMap {
            for (kind in Kind.entries) {
                val painter = painters.getValue(kind)
                put("pin-${kind.name}", roundPin(density, colors, kind.category, painter))
                put("selected-${kind.name}", teardrop(density, colors, kind.category, painter))
            }
        }
    }
}

private fun roundPin(density: Density, colors: HuntColors, category: Category, icon: VectorPainter): Bitmap = with(density) {
    val radius = 14.dp.toPx()
    val ring = 2.5.dp.toPx()
    val shadow = 2.dp.toPx()
    val side = ceil((radius + ring + shadow) * 2).toInt()
    draw(side, side, density) {
        val center = Offset(size.width / 2, size.height / 2 - shadow / 2)
        drawCircle(Color.Black.copy(alpha = 0.22f), radius + ring + 0.5f, center + Offset(0f, shadow))
        drawCircle(Color.White, radius + ring, center)
        drawCircle(colors.category(category).pin, radius, center)
        drawIcon(icon, center, 16.dp.toPx())
    }
}

private fun teardrop(density: Density, colors: HuntColors, category: Category, icon: VectorPainter): Bitmap = with(density) {
    val radius = 21.dp.toPx()
    val ring = 3.dp.toPx()
    val outer = radius + ring
    val width = ceil(outer * 2 + 4.dp.toPx()).toInt()
    val height = ceil(outer * 2.55f + 2.dp.toPx()).toInt()
    draw(width, height, density) {
        val center = Offset(size.width / 2, outer + 1.dp.toPx())
        val tip = Offset(size.width / 2, size.height - 1.dp.toPx())
        val shape = teardropPath(center, outer, tip)
        drawPath(shape, Color.Black.copy(alpha = 0.22f), style = Stroke(width = 3.dp.toPx()))
        drawPath(shape, Color.White)
        drawPath(teardropPath(center, radius, Offset(tip.x, tip.y - ring * 1.6f)), colors.category(category).pin)
        drawIcon(icon, center, 22.dp.toPx())
    }
}

/** A circle that narrows to a point below it, the classic map pin. */
private fun teardropPath(center: Offset, radius: Float, tip: Offset): Path {
    val distance = tip.y - center.y
    // The two points where lines from the tip touch the circle.
    val alpha = Math.toDegrees(acos((radius / distance).toDouble())).toFloat()
    return Path().apply {
        moveTo(tip.x, tip.y)
        arcTo(
            rect = Rect(center, radius),
            startAngleDegrees = 90f + alpha,
            sweepAngleDegrees = 360f - 2 * alpha,
            forceMoveTo = false,
        )
        close()
    }
}

private fun DrawScope.drawIcon(icon: VectorPainter, center: Offset, side: Float) {
    translate(center.x - side / 2, center.y - side / 2) {
        with(icon) { draw(Size(side, side), colorFilter = ColorFilter.tint(Color.White)) }
    }
}

private fun draw(width: Int, height: Int, density: Density, block: DrawScope.() -> Unit): Bitmap {
    val image = ImageBitmap(width, height)
    CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(width.toFloat(), height.toFloat()), block)
    return image.asAndroidBitmap()
}

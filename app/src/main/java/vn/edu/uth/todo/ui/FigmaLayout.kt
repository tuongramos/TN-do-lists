package vn.edu.uth.todo.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.math.roundToInt

data class DesignFrame(val width: Int, val height: Int) {
    companion object {
        val Splash = DesignFrame(400, 860)
        val Onboarding = DesignFrame(390, 844)
    }
}

/**
 * Lay out real Compose controls at the original Figma size, then scale the whole
 * frame uniformly. This preserves typography, spacing and touch target mapping.
 * Extra screen area uses the same background; the design is never stretched.
 */
@Composable
fun FigmaViewport(
    frame: DesignFrame,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Layout(
        modifier = modifier.fillMaxSize(),
        content = { Box(Modifier.fillMaxSize(), content = content) }
    ) { measurables, constraints ->
        val designWidth = frame.width.dp.roundToPx()
        val designHeight = frame.height.dp.roundToPx()
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val scale = min(width.toFloat() / designWidth, height.toFloat() / designHeight)
        val placeable = measurables.single().measure(Constraints.fixed(designWidth, designHeight))
        layout(width, height) {
            placeable.placeWithLayer(
                x = ((width - designWidth * scale) / 2).roundToInt(),
                y = ((height - designHeight * scale) / 2).roundToInt()
            ) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
}

/** Position a Compose text's alphabetic baseline at the SVG tspan's exact Y. */
fun Modifier.figmaBaseline(x: Float, y: Float): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    val baseline = placeable[FirstBaseline].let { if (it == androidx.compose.ui.layout.AlignmentLine.Unspecified) 0 else it }
    layout(placeable.width, placeable.height) {
        placeable.place(x.dp.roundToPx(), y.dp.roundToPx() - baseline)
    }
}

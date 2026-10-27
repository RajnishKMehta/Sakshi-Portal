package rajnishkmehta.sakshi.portal.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Composable that draws and animates a padlock icon using Canvas.
 *
 * @param isUnlocked Whether the lock is in the unlocked state, triggering the open animation.
 * @param modifier Modifier to be applied to the canvas.
 */
@Composable
fun LockAnimation(isUnlocked: Boolean, modifier: Modifier = Modifier) {
    val shackleOffset = remember { Animatable(0f) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    LaunchedEffect(isUnlocked) {
        if (isUnlocked) {
            shackleOffset.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400)
            )
        } else {
            shackleOffset.snapTo(0f)
        }
    }

    Canvas(modifier = modifier.size(100.dp)) {
        val width = size.width
        val height = size.height

        val lockBodyWidth = width * 0.6f
        val lockBodyHeight = height * 0.45f
        val lockBodyLeft = (width - lockBodyWidth) / 2f
        val lockBodyTop = height * 0.45f

        val strokeWidth = width * 0.08f

        // Draw shackle
        val shackleRadius = lockBodyWidth / 2f - strokeWidth / 2f
        val shackleCenterX = width / 2f
        val shackleCenterY = lockBodyTop

        // Calculate offset based on animation
        // Shackle moves up
        val animatedOffsetY = -lockBodyHeight * 0.3f * shackleOffset.value

        val shacklePath = Path().apply {
            // Right side of shackle
            moveTo(shackleCenterX + shackleRadius, shackleCenterY)
            lineTo(shackleCenterX + shackleRadius, shackleCenterY - shackleRadius + animatedOffsetY)

            // Arc
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = shackleCenterX - shackleRadius,
                    top = shackleCenterY - shackleRadius * 2 + animatedOffsetY,
                    right = shackleCenterX + shackleRadius,
                    bottom = shackleCenterY + animatedOffsetY
                ),
                startAngleDegrees = 0f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )

            // Left side of shackle - it detaches and goes up when unlocked
            // If locked (offset == 0), it goes all the way down to lock body
            // If unlocked (offset > 0), it moves up
            val leftSideEnd = if (shackleOffset.value > 0.1f) {
                shackleCenterY - shackleRadius + animatedOffsetY
            } else {
                shackleCenterY
            }
            lineTo(shackleCenterX - shackleRadius, leftSideEnd)
        }

        drawPath(
            path = shacklePath,
            color = primaryColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Draw lock body
        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(lockBodyLeft, lockBodyTop),
            size = Size(lockBodyWidth, lockBodyHeight),
            cornerRadius = CornerRadius(width * 0.1f, width * 0.1f)
        )

        // Draw keyhole
        val keyholeRadius = width * 0.05f
        val keyholeCenterX = width / 2f
        val keyholeCenterY = lockBodyTop + lockBodyHeight * 0.4f

        drawCircle(
            color = backgroundColor,
            radius = keyholeRadius,
            center = Offset(keyholeCenterX, keyholeCenterY)
        )
        val keyholePath = Path().apply {
            moveTo(keyholeCenterX - keyholeRadius * 0.5f, keyholeCenterY + keyholeRadius * 0.5f)
            lineTo(keyholeCenterX + keyholeRadius * 0.5f, keyholeCenterY + keyholeRadius * 0.5f)
            lineTo(keyholeCenterX + keyholeRadius * 0.8f, keyholeCenterY + keyholeRadius * 2.5f)
            lineTo(keyholeCenterX - keyholeRadius * 0.8f, keyholeCenterY + keyholeRadius * 2.5f)
            close()
        }
        drawPath(
            path = keyholePath,
            color = backgroundColor
        )
    }
}

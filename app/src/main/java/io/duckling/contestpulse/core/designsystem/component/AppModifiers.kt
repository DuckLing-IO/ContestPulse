package io.duckling.contestpulse.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme

fun Modifier.pressEffect(
    contentDescription: String,
    role: Role = Role.Button,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val motionEnabled = PulseTheme.motionEnabled
    val scale by animateFloatAsState(
        targetValue = if (motionEnabled && isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // tactile response with a soft rebound
            stiffness = Spring.StiffnessMedium, // quick enough to track a finger press
        ),
        label = "pressScale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (motionEnabled && isPressed) 0.8f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // tactile response with a soft rebound
            stiffness = Spring.StiffnessMedium, // quick enough to track a finger press
        ),
        label = "pressAlpha",
    )
    val minimumTouchTarget = PulseTheme.spacing.huge

    this
        .defaultMinSize(minWidth = minimumTouchTarget, minHeight = minimumTouchTarget)
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }
        .semantics { this.contentDescription = contentDescription }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            role = role,
            onClick = onClick,
        )
}

fun Modifier.appCard(): Modifier = composed {
    val colors = PulseTheme.colors
    val radius = PulseTheme.radius
    val elevation = PulseTheme.elevation

    this
        .shadow(
            elevation = elevation.lg,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(radius.lg),
            ambientColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.04f),
            spotColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.08f),
        )
        .background(
            color = colors.surfaceMuted,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(radius.lg),
        )
}

@Composable
fun appHorizontalPadding(): PaddingValues = PaddingValues(horizontal = PulseTheme.spacing.xl)

@Composable
fun appVerticalPadding(): PaddingValues = PaddingValues(vertical = PulseTheme.spacing.md)

@Composable
fun topLevelScreenPadding(): PaddingValues = PaddingValues(
    start = PulseTheme.spacing.xl,
    top = PulseTheme.spacing.xxl,
    end = PulseTheme.spacing.xl,
    bottom = PulseTheme.spacing.giant + PulseTheme.spacing.giant,
)

@Composable
fun FadeTransition(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable AnimatedVisibilityScope.() -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy, // settle without overshoot
                stiffness = Spring.StiffnessMedium, // balanced content reveal speed
            ),
        ) + scaleIn(
            initialScale = 0.92f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy, // settle without overshoot
                stiffness = Spring.StiffnessMedium, // balanced content reveal speed
            ),
        ),
        exit = fadeOut(
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy, // settle without overshoot
                stiffness = Spring.StiffnessMedium, // balanced content removal speed
            ),
        ) + scaleOut(
            targetScale = 0.92f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy, // settle without overshoot
                stiffness = Spring.StiffnessMedium, // balanced content removal speed
            ),
        ),
        content = content,
    )
}

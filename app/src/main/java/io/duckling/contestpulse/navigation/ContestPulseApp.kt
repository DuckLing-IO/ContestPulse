package io.duckling.contestpulse.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.duckling.contestpulse.R
import io.duckling.contestpulse.core.designsystem.component.FadeTransition
import io.duckling.contestpulse.core.designsystem.component.pressEffect
import io.duckling.contestpulse.core.designsystem.theme.PulseTheme

@Composable
fun ContestPulseApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val isTopLevelDestination = ContestPulseDestination.entries.any { destination ->
        currentDestination?.hierarchy?.any { it.route == destination.route } == true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PulseTheme.colors.background),
    ) {
        ContestPulseNavHost(
            navController = navController,
            modifier = Modifier.fillMaxSize(),
        )

        FadeTransition(
            visible = isTopLevelDestination,
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            ContestPulseBottomBar(
                selectedRoute = currentDestination?.route,
                onDestinationSelected = { destination ->
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                isDestinationSelected = { destination ->
                    currentDestination?.hierarchy?.any { it.route == destination.route } == true
                },
            )
        }
    }
}

@Composable
private fun ContestPulseBottomBar(
    selectedRoute: String?,
    onDestinationSelected: (ContestPulseDestination) -> Unit,
    isDestinationSelected: (ContestPulseDestination) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = PulseTheme.colors
    val spacing = PulseTheme.spacing
    val radius = PulseTheme.radius

    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(
                start = spacing.xl,
                end = spacing.xl,
                bottom = spacing.sm,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = PulseTheme.elevation.lg,
                    shape = RoundedCornerShape(radius.xl),
                    ambientColor = Color.Black.copy(alpha = 0.04f),
                    spotColor = Color.Black.copy(alpha = 0.08f),
                )
                .background(
                    color = colors.surface,
                    shape = RoundedCornerShape(radius.xl),
                )
                .padding(spacing.xs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ContestPulseDestination.entries.forEach { destination ->
                val selected = isDestinationSelected(destination)
                val label = stringResource(destination.labelRes)
                val itemBackground by animateColorAsState(
                    targetValue = if (selected) colors.accent else Color.Transparent,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy, // confirm tab selection softly
                        stiffness = Spring.StiffnessHigh, // keep navigation feedback immediate
                    ),
                    label = "bottomBarItemBackground",
                )
                val itemContentColor by animateColorAsState(
                    targetValue = if (selected) colors.onAccent else colors.textPrimary,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy, // move with the selection surface
                        stiffness = Spring.StiffnessHigh, // keep icon and label feedback immediate
                    ),
                    label = "bottomBarItemContent",
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = itemBackground,
                            shape = RoundedCornerShape(radius.lg),
                        )
                        .semantics { this.selected = selected }
                        .pressEffect(
                            contentDescription = stringResource(
                                R.string.nav_open_description,
                                label,
                            ),
                            role = Role.Tab,
                            onClick = {
                                if (selectedRoute != destination.route) {
                                    onDestinationSelected(destination)
                                }
                            },
                        )
                        .padding(vertical = spacing.xs),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        painter = painterResource(destination.iconRes),
                        contentDescription = null,
                        tint = itemContentColor,
                        modifier = Modifier.size(spacing.xl),
                    )
                    Spacer(modifier = Modifier.height(spacing.xxs))
                    Text(
                        text = label,
                        color = itemContentColor,
                        style = PulseTheme.typography.caption1.copy(
                            fontWeight = if (selected) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                        ),
                    )
                }
            }
        }
    }
}

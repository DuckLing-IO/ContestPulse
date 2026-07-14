package io.duckling.contestpulse.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import io.duckling.contestpulse.domain.model.Contest
import io.duckling.contestpulse.feature.contestdetail.ContestDetailRoute
import io.duckling.contestpulse.feature.contestdetail.ContestDetailViewModel
import io.duckling.contestpulse.feature.contestlist.ContestListRoute
import io.duckling.contestpulse.feature.favorites.FavoritesRoute
import io.duckling.contestpulse.feature.settings.SettingsRoute
import io.duckling.contestpulse.feature.customsource.CustomSourcesRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ContestPulseNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    SharedTransitionLayout(modifier = modifier) {
        val sharedTransitionScope = this
        NavHost(
            navController = navController,
            startDestination = ContestPulseDestination.Contests.route,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // smooth page settle
                        stiffness = Spring.StiffnessLow, // relaxed spatial transition
                    ),
                ) + fadeIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // smooth page settle
                        stiffness = Spring.StiffnessLow, // relaxed spatial transition
                    ),
                )
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // smooth page settle
                        stiffness = Spring.StiffnessLow, // relaxed spatial transition
                    ),
                ) + fadeOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // smooth page settle
                        stiffness = Spring.StiffnessLow, // relaxed spatial transition
                    ),
                )
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -(fullWidth * 0.15f).toInt() },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // smooth reverse settle
                        stiffness = Spring.StiffnessLow, // relaxed spatial transition
                    ),
                ) + fadeIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // smooth reverse settle
                        stiffness = Spring.StiffnessLow, // relaxed spatial transition
                    ),
                )
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> (fullWidth * 0.15f).toInt() },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // smooth reverse settle
                        stiffness = Spring.StiffnessLow, // relaxed spatial transition
                    ),
                ) + fadeOut(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy, // smooth reverse settle
                        stiffness = Spring.StiffnessLow, // relaxed spatial transition
                    ),
                )
            },
        ) {
            composable(route = ContestPulseDestination.Contests.route) {
                val animatedVisibilityScope = this
                ContestListRoute(
                    onContestClick = { contestId ->
                        navController.navigate(ContestDetailDestination.createRoute(contestId))
                    },
                    sharedElementModifier = { contest ->
                        Modifier.contestSharedElementModifier(
                            sharedTransitionScope = sharedTransitionScope,
                            contest = contest,
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    },
                )
            }
            composable(route = ContestPulseDestination.Favorites.route) {
                val animatedVisibilityScope = this
                FavoritesRoute(
                    onContestClick = { contestId ->
                        navController.navigate(ContestDetailDestination.createRoute(contestId))
                    },
                    sharedElementModifier = { contest ->
                        Modifier.contestSharedElementModifier(
                            sharedTransitionScope = sharedTransitionScope,
                            contest = contest,
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    },
                )
            }
            composable(route = ContestPulseDestination.Settings.route) {
                SettingsRoute(
                    onOpenCustomSources = {
                        navController.navigate(CustomSourcesDestination.route)
                    },
                )
            }
            composable(route = CustomSourcesDestination.route) {
                CustomSourcesRoute(onBack = { navController.navigateUp() })
            }
            composable(
                route = ContestDetailDestination.route,
                arguments = listOf(
                    navArgument(ContestDetailViewModel.CONTEST_ID_ARGUMENT) {
                        type = NavType.StringType
                    },
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = ContestDetailDestination.deepLinkPattern },
                ),
            ) {
                val animatedVisibilityScope = this
                ContestDetailRoute(
                    onBack = { navController.navigateUp() },
                    sharedElementModifier = { contest ->
                        Modifier.contestSharedElementModifier(
                            sharedTransitionScope = sharedTransitionScope,
                            contest = contest,
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.contestSharedElementModifier(
    sharedTransitionScope: SharedTransitionScope,
    contest: Contest,
    animatedVisibilityScope: AnimatedVisibilityScope,
): Modifier = with(sharedTransitionScope) {
    sharedElement(
        state = rememberSharedContentState(key = "contest-${contest.id}"),
        animatedVisibilityScope = animatedVisibilityScope,
        boundsTransform = { _, _ ->
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy, // preserve spatial continuity
                stiffness = Spring.StiffnessLow, // allow the card to expand calmly
            )
        },
    )
}

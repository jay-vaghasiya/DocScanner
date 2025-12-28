package com.jay.docscanner

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute


/**
 * Adaptive transition specifications for Material 3 motion
 */
object NavTransitions {

    private const val DURATION_MEDIUM = 400
    private const val DURATION_SHORT = 300
    private const val DURATION_LONG = 500

    // Forward navigation - slide from right with fade
    val enterSlideHorizontal: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EaseOutCubic
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = EaseOutCubic
            )
        )
    }

    val exitSlideHorizontal: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> -fullWidth / 4 },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EaseInOutCubic
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = EaseInOutCubic
            )
        )
    }

    // Pop navigation - slide from left with fade
    val popEnterSlideHorizontal: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(
            initialOffsetX = { fullWidth -> -fullWidth / 4 },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EaseOutCubic
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = EaseOutCubic
            )
        )
    }

    val popExitSlideHorizontal: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EaseInOutCubic
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = DURATION_SHORT,
                easing = EaseInOutCubic
            )
        )
    }

    // Vertical transitions for modals/sheets
    val enterSlideVertical: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(
                durationMillis = DURATION_LONG,
                easing = EaseOutCubic
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM
            )
        )
    }

    val exitSlideVertical: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EaseInOutCubic
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = DURATION_SHORT
            )
        )
    }

    // Scale transitions for detail screens
    val enterScale: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        scaleIn(
            initialScale = 0.92f,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EaseOutCubic
            )
        ) + fadeIn(
            animationSpec = tween(
                durationMillis = DURATION_SHORT
            )
        )
    }

    val exitScale: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        scaleOut(
            targetScale = 1.05f,
            animationSpec = tween(
                durationMillis = DURATION_MEDIUM,
                easing = EaseInOutCubic
            )
        ) + fadeOut(
            animationSpec = tween(
                durationMillis = DURATION_SHORT
            )
        )
    }
}

@Composable
fun DocScannerNavGraph(
    navController: NavHostController,
    onScanDocument: () -> Unit,
    scannedImageUris: List<String>,
    pdfFilePath: String?,
    onClearScannedData: () -> Unit,
    onSavePdf: (filePath: String?, fileName: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home
    ) {
        // Home Screen
        composable<Screen.Home>(
            enterTransition = NavTransitions.enterSlideHorizontal,
            exitTransition = NavTransitions.exitSlideHorizontal,
            popEnterTransition = NavTransitions.popEnterSlideHorizontal,
            popExitTransition = NavTransitions.popExitSlideHorizontal
        ) {
            HomeScreen(
                onScanClick = onScanDocument,
//                onHistoryClick = { navController.navigate(Screen.History) },
                onSettingsClick = { navController.navigate(Screen.Settings) },
                scannedImageUris = scannedImageUris,
                onPreviewClick = {
                    if (scannedImageUris.isNotEmpty()) {
                        navController.navigate(
                            Screen.Preview(
                                imageUris = scannedImageUris,
                                pdfPath = pdfFilePath
                            )
                        )
                    }
                }
            )
        }

        // Preview Screen
        composable<Screen.Preview>(
            enterTransition = NavTransitions.enterScale,
            exitTransition = NavTransitions.exitScale,
            popEnterTransition = NavTransitions.popEnterSlideHorizontal,
            popExitTransition = NavTransitions.popExitSlideHorizontal
        ) { backStackEntry ->
            val preview = backStackEntry.toRoute<Screen.Preview>()
            PreviewScreen(
                imageUris = preview.imageUris,
                pdfPath = preview.pdfPath,
                onNavigateBack = { navController.popBackStack() },
                onSavePdf = onSavePdf,
                onSaveSuccess = {
                    onClearScannedData()
                    navController.popBackStack(Screen.Home, inclusive = false)
                }
            )
        }

        // Document Detail Screen
        composable<Screen.DocumentDetail>(
            enterTransition = NavTransitions.enterScale,
            exitTransition = NavTransitions.exitScale,
            popEnterTransition = NavTransitions.popEnterSlideHorizontal,
            popExitTransition = NavTransitions.popExitSlideHorizontal
        ) { backStackEntry ->
            val detail = backStackEntry.toRoute<Screen.DocumentDetail>()
            DocumentDetailScreen(
                documentId = detail.documentId,
                documentName = detail.documentName,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Settings Screen
        composable<Screen.Settings>(
            enterTransition = NavTransitions.enterSlideVertical,
            exitTransition = NavTransitions.exitSlideVertical,
            popEnterTransition = NavTransitions.popEnterSlideHorizontal,
            popExitTransition = NavTransitions.exitSlideVertical
        ) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

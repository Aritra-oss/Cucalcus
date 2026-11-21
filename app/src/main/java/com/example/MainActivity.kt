package com.example

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.preferences.AppBackgroundPreset
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.calculator.CalculatorViewModel
import com.example.ui.components.CucalcusNavActions
import com.example.ui.components.CucalcusRoute
import com.example.ui.components.LaunchSplashOverlay
import com.example.ui.components.LocalCucalcusNavActions
import com.example.ui.converter.ConverterScreen
import com.example.ui.converter.ConverterViewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.CucalcusTheme
import com.example.ui.theme.surfacePaletteForTheme
import com.example.ui.tools.ToolsScreen
import com.example.ui.tools.ToolsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as CucalcusApp
        val initialSettings = app.settingsRepository.currentSettings
        val initialPalette = surfacePaletteForTheme(initialSettings.themeMode)
        val initialIsLight = !initialPalette.isDark
        val initialBgArgb = if (
            initialSettings.backgroundPreset != AppBackgroundPreset.THEME_DEFAULT &&
            initialSettings.backgroundImagePath.isNullOrBlank()
        ) {
            Color(initialSettings.backgroundPreset.argbHex).toArgb()
        } else {
            initialPalette.mainBg.toArgb()
        }
        window.setBackgroundDrawable(ColorDrawable(initialBgArgb))

        enableEdgeToEdge(
            statusBarStyle = if (initialIsLight) {
                SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            },
            navigationBarStyle = if (initialIsLight) {
                SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
            } else {
                SystemBarStyle.dark(AndroidColor.TRANSPARENT)
            }
        )

        setContent {
            val settings by app.settingsRepository.settingsState
                .collectAsStateWithLifecycle()

            var blurredBgBitmap by remember { mutableStateOf<Bitmap?>(null) }
            var isBgReady by remember {
                mutableStateOf(initialSettings.backgroundImagePath.isNullOrBlank())
            }
            LaunchedEffect(settings.backgroundImagePath) {
                blurredBgBitmap = app.backgroundImageManager.loadLightlyBlurredBitmap(settings.backgroundImagePath)
                isBgReady = true
            }

            CucalcusTheme(settings = settings) {
                val colors = CucalcusTheme.colors
                val isLight = !colors.isDark
                LaunchedEffect(isLight) {
                    enableEdgeToEdge(
                        statusBarStyle = if (isLight) {
                            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                        } else {
                            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        },
                        navigationBarStyle = if (isLight) {
                            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                        } else {
                            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        }
                    )
                }

                val reducedMotion = CucalcusTheme.reducedMotion
                var minSplashElapsed by remember { mutableStateOf(reducedMotion) }
                LaunchedEffect(reducedMotion) {
                    if (!reducedMotion) {
                        delay(360)
                    }
                    minSplashElapsed = true
                }
                val showLaunchSplash = !minSplashElapsed || !isBgReady

                val navController = rememberNavController()
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route ?: CucalcusRoute.CALCULATOR.route
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                val showMessage: (String) -> Unit = remember(scope, snackbarHostState) {
                    { msg ->
                        scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(msg)
                        }
                    }
                }

                val calculatorVm: CalculatorViewModel = viewModel(
                    factory = CalculatorViewModel.Factory(
                        app.settingsRepository,
                        app.historyRepository,
                        app.hapticManager
                    )
                )
                val historyVm: HistoryViewModel = viewModel(
                    factory = HistoryViewModel.Factory(app.historyRepository)
                )
                val converterVm: ConverterViewModel = viewModel(
                    factory = ConverterViewModel.Factory(
                        app.currencyRepository,
                        app.settingsRepository
                    )
                )
                val toolsVm: ToolsViewModel = viewModel(
                    factory = ToolsViewModel.Factory(app.currencyRepository)
                )
                val settingsVm: SettingsViewModel = viewModel(
                    factory = SettingsViewModel.Factory(
                        app.settingsRepository,
                        app.historyRepository,
                        app.hapticManager,
                        app.launcherIconManager,
                        app.backgroundImageManager
                    )
                )

                val navigateTo: (CucalcusRoute) -> Unit = remember(navController) {
                    { target ->
                        navController.navigate(target.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                val animMs = if (reducedMotion) 0 else 120

                val navActions = remember(currentRoute, navigateTo) {
                    CucalcusNavActions(currentRoute = currentRoute, onNavigate = navigateTo)
                }

                CompositionLocalProvider(LocalCucalcusNavActions provides navActions) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.mainBg)
                    ) {
                        val activeBitmap = blurredBgBitmap
                        if (activeBitmap != null) {
                            Image(
                                bitmap = activeBitmap.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Slight dim scrim keeping gallery picture visible while preserving crisp contrast
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.44f))
                            )
                        }

                        Scaffold(
                            containerColor = Color.Transparent,
                            contentWindowInsets = WindowInsets(0, 0, 0, 0),
                            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                                    .windowInsetsPadding(WindowInsets.systemBars)
                            ) {
                                NavHost(
                                    navController = navController,
                                    startDestination = CucalcusRoute.CALCULATOR.route,
                                    enterTransition = { fadeIn(tween(animMs)) },
                                    exitTransition = { fadeOut(tween(animMs)) },
                                    popEnterTransition = { fadeIn(tween(animMs)) },
                                    popExitTransition = { fadeOut(tween(animMs)) }
                                ) {
                                    composable(CucalcusRoute.CALCULATOR.route) {
                                        CalculatorScreen(
                                            viewModel = calculatorVm,
                                            onShowMessage = showMessage
                                        )
                                    }
                                    composable(CucalcusRoute.HISTORY.route) {
                                        HistoryScreen(
                                            viewModel = historyVm,
                                            onReuseCalculation = { expr, res ->
                                                calculatorVm.loadExpressionFromHistory(expr, res)
                                                navigateTo(CucalcusRoute.CALCULATOR)
                                            },
                                            onShowMessage = showMessage
                                        )
                                    }
                                    composable(CucalcusRoute.CONVERTER.route) {
                                        ConverterScreen(
                                            viewModel = converterVm,
                                            onShowMessage = showMessage
                                        )
                                    }
                                    composable(CucalcusRoute.TOOLS.route) {
                                        ToolsScreen(viewModel = toolsVm)
                                    }
                                    composable(CucalcusRoute.SETTINGS.route) {
                                        SettingsScreen(
                                            viewModel = settingsVm,
                                            onShowMessage = showMessage
                                        )
                                    }
                                }
                            }
                        }

                        LaunchSplashOverlay(visible = showLaunchSplash)
                    }
                }
            }
        }
    }
}

package com.example

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.createScanCaptureFile
import com.example.data.repository.loadScanBitmapFromUri
import com.example.platform.configureAppCheck
import com.example.ui.calculator.CalculatorScreen
import com.example.ui.calculator.CalculatorViewModel
import com.example.ui.components.CucalcusNavActions
import com.example.ui.components.CucalcusRoute
import com.example.ui.components.LaunchSplashOverlay
import com.example.ui.components.LocalCucalcusNavActions
import com.example.ui.components.ScanProblemDialog
import com.example.ui.converter.ConverterScreen
import com.example.ui.converter.ConverterViewModel
import com.example.ui.history.HistoryScreen
import com.example.ui.history.HistoryViewModel
import com.example.ui.settings.BrandIconBitmapCache
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.CucalcusTheme
import com.example.ui.tools.ToolsScreen
import com.example.ui.tools.ToolsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure Firebase App Check before UI, ViewModels, or Firebase operations.
        configureAppCheck(this, intent)

        val app = application as CucalcusApp
        val initialSettings = app.settingsRepository.currentSettings

        // Match pure black splash screen on frame 0 before Compose attaches.
        window.setBackgroundDrawable(ColorDrawable(AndroidColor.BLACK))

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
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
            LaunchedEffect(Unit) {
                BrandIconBitmapCache.warmUp(applicationContext)
            }

            CucalcusTheme(settings = settings) {
                val colors = CucalcusTheme.colors
                val isLight = !colors.isDark
                val reducedMotion = CucalcusTheme.reducedMotion
                var minSplashElapsed by remember { mutableStateOf(reducedMotion) }
                LaunchedEffect(reducedMotion) {
                    if (!reducedMotion) {
                        delay(640)
                    }
                    minSplashElapsed = true
                }
                val showLaunchSplash = !minSplashElapsed || !isBgReady

                LaunchedEffect(isLight, showLaunchSplash) {
                    val useLightBars = !showLaunchSplash && isLight
                    enableEdgeToEdge(
                        statusBarStyle = if (useLightBars) {
                            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                        } else {
                            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        },
                        navigationBarStyle = if (useLightBars) {
                            SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                        } else {
                            SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                        }
                    )
                }

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
                        app.hapticManager,
                        app.problemScannerRepository
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

                var isMenuExpanded by remember { mutableStateOf(false) }
                var showScanDialog by remember { mutableStateOf(false) }
                var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

                BackHandler(enabled = isMenuExpanded) {
                    isMenuExpanded = false
                }

                val navigateTo: (CucalcusRoute) -> Unit = remember(navController) {
                    { target ->
                        isMenuExpanded = false
                        navController.navigate(target.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }

                val handleScanUri: (Uri) -> Unit = remember(navController, calculatorVm, scope) {
                    { uri ->
                        isMenuExpanded = false
                        navigateTo(CucalcusRoute.CALCULATOR)
                        scope.launch {
                            val bmp = loadScanBitmapFromUri(this@MainActivity, uri)
                            if (bmp != null) {
                                calculatorVm.solveScannedBitmap(bmp)
                            } else {
                                calculatorVm.setScanError("Could not decode the selected image. Please try another photo.")
                            }
                        }
                    }
                }

                val galleryScanLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.PickVisualMedia()
                ) { uri ->
                    if (uri != null) {
                        handleScanUri(uri)
                    }
                }

                val cameraScanLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.TakePicture()
                ) { success ->
                    val uri = pendingCameraUri
                    pendingCameraUri = null
                    if (success && uri != null) {
                        handleScanUri(uri)
                    }
                }

                // Keep route motion short and GPU-friendly. A lightly damped spring gives
                // each section a soft settle without the long oscillation that can feel laggy.
                val routeEnterSpring = spring<Float>(
                    dampingRatio = 0.90f,
                    stiffness = Spring.StiffnessMedium
                )
                val routeExitSpring = spring<Float>(
                    dampingRatio = 1.0f,
                    stiffness = Spring.StiffnessMedium
                )
                val routeOffsetEnterSpring = spring(
                    dampingRatio = 0.90f,
                    stiffness = Spring.StiffnessMedium
                )
                val routeOffsetExitSpring = spring(
                    dampingRatio = 1.0f,
                    stiffness = Spring.StiffnessMedium
                )

                val navActions = remember(currentRoute, isMenuExpanded, navigateTo) {
                    CucalcusNavActions(
                        currentRoute = currentRoute,
                        isMenuExpanded = isMenuExpanded,
                        onToggleMenu = { isMenuExpanded = !isMenuExpanded },
                        onCloseMenu = { isMenuExpanded = false },
                        onNavigate = navigateTo,
                        onScanProblem = {
                            isMenuExpanded = false
                            showScanDialog = true
                        },
                        onSubmitTypedProblem = { problemText ->
                            isMenuExpanded = false
                            navigateTo(CucalcusRoute.CALCULATOR)
                            calculatorVm.solveTypedProblem(problemText)
                        }
                    )
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
                                    enterTransition = {
                                        if (reducedMotion) {
                                            fadeIn(androidx.compose.animation.core.tween(0))
                                        } else {
                                            fadeIn(routeEnterSpring) +
                                                scaleIn(
                                                    animationSpec = routeEnterSpring,
                                                    initialScale = 0.985f
                                                ) +
                                                slideInVertically(
                                                    animationSpec = routeOffsetEnterSpring
                                                ) { fullHeight -> fullHeight / 36 }
                                        }
                                    },
                                    exitTransition = {
                                        if (reducedMotion) {
                                            fadeOut(androidx.compose.animation.core.tween(0))
                                        } else {
                                            fadeOut(routeExitSpring) +
                                                scaleOut(
                                                    animationSpec = routeExitSpring,
                                                    targetScale = 0.995f
                                                ) +
                                                slideOutVertically(
                                                    animationSpec = routeOffsetExitSpring
                                                ) { fullHeight -> -fullHeight / 48 }
                                        }
                                    },
                                    popEnterTransition = {
                                        if (reducedMotion) {
                                            fadeIn(androidx.compose.animation.core.tween(0))
                                        } else {
                                            fadeIn(routeEnterSpring) +
                                                scaleIn(
                                                    animationSpec = routeEnterSpring,
                                                    initialScale = 0.985f
                                                ) +
                                                slideInVertically(
                                                    animationSpec = routeOffsetEnterSpring
                                                ) { fullHeight -> fullHeight / 36 }
                                        }
                                    },
                                    popExitTransition = {
                                        if (reducedMotion) {
                                            fadeOut(androidx.compose.animation.core.tween(0))
                                        } else {
                                            fadeOut(routeExitSpring) +
                                                scaleOut(
                                                    animationSpec = routeExitSpring,
                                                    targetScale = 0.995f
                                                ) +
                                                slideOutVertically(
                                                    animationSpec = routeOffsetExitSpring
                                                ) { fullHeight -> -fullHeight / 48 }
                                        }
                                    }
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

                        if (showScanDialog) {
                            ScanProblemDialog(
                                onDismiss = { showScanDialog = false },
                                onTakePhoto = {
                                    showScanDialog = false
                                    runCatching {
                                        val captureFile = createScanCaptureFile(this@MainActivity)
                                        val uri = FileProvider.getUriForFile(
                                            this@MainActivity,
                                            "${BuildConfig.APPLICATION_ID}.fileprovider",
                                            captureFile
                                        )
                                        pendingCameraUri = uri
                                        cameraScanLauncher.launch(uri)
                                    }.onFailure {
                                        pendingCameraUri = null
                                        showMessage("No camera app available — try Choose from Gallery")
                                    }
                                },
                                onPickFromGallery = {
                                    showScanDialog = false
                                    galleryScanLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            )
                        }

                        LaunchSplashOverlay(visible = showLaunchSplash)
                    }
                }
            }
        }
    }
}

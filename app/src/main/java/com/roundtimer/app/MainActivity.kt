package com.roundtimer.app

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.roundtimer.app.timer.TimerService
import com.roundtimer.app.ui.presets.PresetsScreen
import com.roundtimer.app.ui.presets.PresetsViewModel
import com.roundtimer.app.ui.setup.SetupScreen
import com.roundtimer.app.ui.setup.SetupViewModel
import com.roundtimer.app.ui.theme.RoundTimerTheme
import com.roundtimer.app.ui.timer.ActiveTimerScreen

object Routes {
    const val SETUP = "setup"
    const val TIMER = "timer"
    const val PRESETS = "presets"
}

class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        setContent {
            RoundTimerTheme {
                val nav = rememberNavController()
                navController = nav
                AppNavHost(nav)
            }
        }
        handleOpenTimerIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOpenTimerIntent(intent)
    }

    /** Clique na notificação durante o timer → vai direto para a tela do timer. */
    private fun handleOpenTimerIntent(intent: Intent?) {
        if (intent?.action == TimerService.ACTION_OPEN_TIMER) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            navController?.navigate(Routes.TIMER) { launchSingleTop = true }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController) {
    val context = LocalContext.current
    val container = (context.applicationContext as RoundTimerApp).container

    NavHost(navController = nav, startDestination = Routes.SETUP) {
        composable(Routes.SETUP) {
            val vm: SetupViewModel = viewModel(
                factory = SetupViewModel.Factory(container.settings, container.presetDao),
            )
            SetupScreen(
                viewModel = vm,
                onStartTimer = { nav.navigate(Routes.TIMER) },
                onOpenPresets = { nav.navigate(Routes.PRESETS) },
            )
        }
        composable(Routes.TIMER) {
            ActiveTimerScreen(onFinished = { nav.popBackStack() })
        }
        composable(Routes.PRESETS) {
            val vm: PresetsViewModel = viewModel(
                factory = PresetsViewModel.Factory(container.presetDao, container.settings),
            )
            PresetsScreen(
                viewModel = vm,
                onPresetSelected = { nav.popBackStack() },
                onBack = { nav.popBackStack() },
            )
        }
    }
}

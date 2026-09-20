package app.orionmd.management

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.orionmd.management.data.AppSession
import app.orionmd.management.data.entity.ThemeMode
import app.orionmd.management.ui.analytics.AnalyticsScreen
import app.orionmd.management.ui.common.RentalsBackground
import app.orionmd.management.ui.database.DatabaseScreen
import app.orionmd.management.ui.lock.LockGateway
import app.orionmd.management.ui.revenue.RevenueScreen
import app.orionmd.management.ui.schedule.ScheduleScreen
import app.orionmd.management.ui.settings.SettingsScreen
import app.orionmd.management.ui.theme.RentalsTheme
import app.orionmd.management.util.AppStrings
import app.orionmd.management.util.EnglishStrings
import app.orionmd.management.util.LocalStrings
import app.orionmd.management.util.SpanishStrings

private sealed class Tab(val route: String, val label: (AppStrings) -> String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Schedule : Tab("schedule", { it.tabSchedule }, Icons.Filled.Schedule)
    object Revenue : Tab("revenue", { it.tabRevenue }, Icons.Filled.AttachMoney)
    object Analytics : Tab("analytics", { it.tabAnalytics }, Icons.Filled.Analytics)
    object Database : Tab("database", { it.tabDatabase }, Icons.Filled.Storage)
    object Settings : Tab("settings", { it.tabSettings }, Icons.Filled.Settings)
}

private val bottomTabs = listOf(Tab.Schedule, Tab.Revenue, Tab.Analytics, Tab.Database, Tab.Settings)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Pre-unlock (setup/unlock screens), there's no decrypted database yet to read a
            // saved theme from, so those screens always use the Day/Night system default. The
            // persisted Day/Night/Material Gray choice only takes effect once inside the
            // unlocked app content below, which re-themes itself from Settings.
            RentalsTheme {
                LockGateway {
                    RentalsBackground {
                        RentalsAppContent()
                    }
                }
            }
        }
    }
}

@Composable
private fun RentalsAppContent() {
    val navController = rememberNavController()
    val repository = remember { AppSession.repository!! }
    val settings by repository.observeSettings().collectAsState(initial = null)
    val strings = if (settings?.language == "es") SpanishStrings else EnglishStrings

    // Re-theme from here down using the persisted Day/Night/Material Gray choice, now that the
    // database is unlocked and Settings is readable (the lock screens above use the system
    // default since they render before any theme preference can be read).
    RentalsTheme(themeMode = settings?.themeMode ?: ThemeMode.DAY) {
    CompositionLocalProvider(LocalStrings provides strings) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                NavigationBar(containerColor = Color.Transparent) {
                    bottomTabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        val label = tab.label(strings)
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = label) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Tab.Schedule.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Tab.Schedule.route) { ScheduleScreen() }
                composable(Tab.Revenue.route) { RevenueScreen() }
                composable(Tab.Analytics.route) { AnalyticsScreen() }
                composable(Tab.Database.route) { DatabaseScreen() }
                composable(Tab.Settings.route) { SettingsScreen() }
            }
        }
    }
    }
}

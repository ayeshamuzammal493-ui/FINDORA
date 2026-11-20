package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.FindoraDatabase
import com.example.data.db.FoundReportEntity
import com.example.data.db.LostReportEntity
import com.example.data.db.UserEntity
import com.example.data.prefs.SessionManager
import com.example.data.repo.FindoraRepository
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.ConversationDetailScreen
import com.example.ui.screens.CreateReportScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MessagesInboxScreen
import com.example.ui.screens.ReportDetailSheet
import com.example.ui.state.AuthUiState
import com.example.ui.state.FindoraViewModel
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.ElectricBlueSoft
import com.example.ui.theme.FindoraTheme
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SlateMuted
import com.example.ui.theme.SlateSurface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE),
            navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE)
        )

        val appContext = applicationContext
        val db = FindoraDatabase.getInstance(appContext)
        val sessionManager = SessionManager(appContext)
        val repository = FindoraRepository(appContext, db, sessionManager)

        setContent {
            FindoraTheme {
                val viewModel: FindoraViewModel = viewModel(
                    factory = FindoraViewModel.factory(repository)
                )
                FindoraAppRoot(viewModel = viewModel)
            }
        }
    }
}

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    EXPLORE("Explore", Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explore"),
    MESSAGES("Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_messages"),
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard")
}

private sealed interface OverlayRoute {
    data class CreateOrEditReport(
        val isLost: Boolean,
        val editingLost: LostReportEntity? = null,
        val editingFound: FoundReportEntity? = null
    ) : OverlayRoute

    data class Conversation(val conversationId: String) : OverlayRoute
    data object AdminPanel : OverlayRoute
}

@Composable
private fun FindoraAppRoot(viewModel: FindoraViewModel) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val isAuthBusy by viewModel.isAuthBusy.collectAsStateWithLifecycle()

    when (val state = authState) {
        AuthUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PureWhite),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = ElectricBlue)
            }
        }
        AuthUiState.Unauthenticated -> {
            AuthScreen(
                isBusy = isAuthBusy,
                errorMessage = authError,
                onClearError = viewModel::clearAuthError,
                onLogin = viewModel::login,
                onSignUp = viewModel::signUp,
                onResetPassword = viewModel::resetPassword,
                onQuickDemoLogin = viewModel::quickDemoLogin
            )
        }
        is AuthUiState.Authenticated -> {
            key(state.user.id) {
                AuthenticatedAppShell(
                    currentUser = state.user,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    currentUser: UserEntity,
    viewModel: FindoraViewModel
) {
    val data by viewModel.appData.collectAsStateWithLifecycle()
    val filters by viewModel.searchFilters.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var overlayRoute by remember { mutableStateOf<OverlayRoute?>(null) }
    var inspectingLost by remember { mutableStateOf<LostReportEntity?>(null) }
    var inspectingFound by remember { mutableStateOf<FoundReportEntity?>(null) }

    // Full-screen overlay routes (Create/Edit Report, Conversation Chat, Admin Panel)
    when (val overlay = overlayRoute) {
        is OverlayRoute.CreateOrEditReport -> {
            CreateReportScreen(
                isLostMode = overlay.isLost,
                currentUser = currentUser,
                editingLost = overlay.editingLost,
                editingFound = overlay.editingFound,
                viewModel = viewModel,
                onClose = { overlayRoute = null }
            )
            return
        }
        is OverlayRoute.Conversation -> {
            ConversationDetailScreen(
                conversationId = overlay.conversationId,
                currentUser = currentUser,
                data = data,
                viewModel = viewModel,
                onBack = { overlayRoute = null }
            )
            return
        }
        OverlayRoute.AdminPanel -> {
            AdminDashboardScreen(
                data = data,
                viewModel = viewModel,
                onBack = { overlayRoute = null }
            )
            return
        }
        null -> Unit
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = PureWhite,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(containerColor = PureWhite) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricBlue,
                                    selectedTextColor = DeepNavy,
                                    indicatorColor = ElectricBlueSoft,
                                    unselectedIconColor = SlateMuted,
                                    unselectedTextColor = SlateMuted
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = SlateSurface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        MainTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricBlue,
                                    selectedTextColor = DeepNavy,
                                    indicatorColor = ElectricBlueSoft,
                                    unselectedIconColor = SlateMuted,
                                    unselectedTextColor = SlateMuted
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 840.dp)) {
                        when (currentTab) {
                            MainTab.HOME -> HomeScreen(
                                currentUser = currentUser,
                                data = data,
                                onOpenSearch = { currentTab = MainTab.EXPLORE },
                                onReportLost = { overlayRoute = OverlayRoute.CreateOrEditReport(isLost = true) },
                                onReportFound = { overlayRoute = OverlayRoute.CreateOrEditReport(isLost = false) },
                                onSelectLost = { inspectingLost = it },
                                onSelectFound = { inspectingFound = it },
                                onOpenMatches = { currentTab = MainTab.DASHBOARD },
                                onMarkNotificationsRead = { viewModel.markNotificationsRead(currentUser.id) }
                            )
                            MainTab.EXPLORE -> ExploreScreen(
                                data = data,
                                filters = filters,
                                onUpdateFilters = viewModel::updateFilters,
                                onSetApproxLocation = viewModel::setApproxLocation,
                                onSelectLost = { inspectingLost = it },
                                onSelectFound = { inspectingFound = it },
                                onShowMessage = { /* surfaced via snackbar */ }
                            )
                            MainTab.MESSAGES -> MessagesInboxScreen(
                                currentUser = currentUser,
                                data = data,
                                onOpenConversation = { convId ->
                                    overlayRoute = OverlayRoute.Conversation(convId)
                                }
                            )
                            MainTab.DASHBOARD -> DashboardScreen(
                                currentUser = currentUser,
                                data = data,
                                onEditLost = { lost ->
                                    overlayRoute = OverlayRoute.CreateOrEditReport(isLost = true, editingLost = lost)
                                },
                                onDeleteLost = viewModel::deleteLostReport,
                                onEditFound = { found ->
                                    overlayRoute = OverlayRoute.CreateOrEditReport(isLost = false, editingFound = found)
                                },
                                onDeleteFound = viewModel::deleteFoundReport,
                                onOpenMatchChat = { lost, found ->
                                    viewModel.startConversation(currentUser.id, lost, found) { convId ->
                                        overlayRoute = OverlayRoute.Conversation(convId)
                                    }
                                },
                                onDismissMatch = viewModel::dismissMatch,
                                onUpdateProfile = { name, city, bio ->
                                    viewModel.updateProfile(currentUser.id, name, city, bio)
                                },
                                onUpgradePlan = { tier -> viewModel.upgradePlan(currentUser.id, tier) },
                                onOpenAdminPanel = { overlayRoute = OverlayRoute.AdminPanel },
                                onLogout = viewModel::logout
                            )
                        }
                    }
                }
            }
        }
    }

    if (inspectingLost != null || inspectingFound != null) {
        ReportDetailSheet(
            currentUser = currentUser,
            lostReport = inspectingLost,
            foundReport = inspectingFound,
            onDismiss = {
                inspectingLost = null
                inspectingFound = null
            },
            onStartChat = {
                val lost = inspectingLost
                val found = inspectingFound
                inspectingLost = null
                inspectingFound = null
                viewModel.startConversation(currentUser.id, lost, found) { convId ->
                    overlayRoute = OverlayRoute.Conversation(convId)
                }
            },
            onSubmitSafetyReport = { type, id, reason, details ->
                viewModel.submitSafetyReport(currentUser.id, type, id, reason, details)
            }
        )
    }
}

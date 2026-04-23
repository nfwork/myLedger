package com.myledger.app.ui.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import android.app.Activity
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.JsonObject
import com.myledger.app.AppServices
import com.myledger.app.ui.components.MyBottomBar
import com.myledger.app.ui.screens.AccountsScreen
import com.myledger.app.ui.screens.CategoriesScreen
import com.myledger.app.ui.screens.ChangePasswordScreen
import com.myledger.app.ui.screens.DashboardScreen
import com.myledger.app.ui.screens.EntriesScreen
import com.myledger.app.ui.screens.EntryFormScreen
import com.myledger.app.ui.screens.LoginScreen
import com.myledger.app.ui.screens.ProfileScreen
import com.myledger.app.ui.screens.RegisterScreen
import com.myledger.app.ui.screens.StatisticsScreen
import com.myledger.app.ui.theme.Bg
import com.myledger.app.ui.theme.HeaderText
import com.myledger.app.ui.theme.PrimaryDark
import com.myledger.app.ui.theme.TealLight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun titleFor(route: String?): String? = when (route) {
    "dashboard" -> "概览"
    "entries" -> "流水"
    "stats" -> "统计"
    "profile" -> "我的"
    "entry_new" -> "记一笔"
    "categories" -> "分类管理"
    "accounts" -> "资金账户"
    "password" -> "修改密码"
    null -> null
    else -> if (route.startsWith("entry_edit/")) "编辑流水" else null
}

private fun showBack(route: String?): Boolean = when (route) {
    "entry_new", "categories", "accounts", "password" -> true
    else -> route?.startsWith("entry_edit/") == true
}

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var user by remember { mutableStateOf<JsonObject?>(null) }
    var lastBackPressTime by remember { mutableStateOf(0L) }

    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val title = titleFor(route)
    val bottomVisible = route in setOf("dashboard", "entries", "stats", "profile")

    fun snackMsg(msg: String) {
        scope.launch { snack.showSnackbar(msg) }
    }

    fun navigateToTab(targetRoute: String) {
        nav.navigate(targetRoute) {
            popUpTo(nav.graph.id) {
                inclusive = true
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            if (title != null) {
                // 与 H5 MainLayout.vue .page-head 接近：statusBar + 约 10dp 垂直留白，避免 Material TopAppBar 默认过高
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    PrimaryDark,
                                    Color(0xFF0D9488),
                                    TealLight,
                                ),
                            ),
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        title,
                        color = HeaderText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 0.5.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 44.dp),
                    )
                    if (showBack(route)) {
                        IconButton(
                            onClick = { nav.popBackStack() },
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = HeaderText,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (bottomVisible) {
                MyBottomBar(
                    currentRoute = route,
                    onDashboard = { navigateToTab("dashboard") },
                    onEntries = { navigateToTab("entries") },
                    onNewEntry = { nav.navigate("entry_new") },
                    onStats = { navigateToTab("stats") },
                    onProfile = { navigateToTab("profile") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        val isTopLevelRoute = route in setOf("dashboard", "entries", "stats", "profile", "login")

        BackHandler(enabled = isTopLevelRoute) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000) {
                (context as? Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "再按一次退出应用", Toast.LENGTH_SHORT).show()
            }
        }

        NavHost(
            navController = nav,
            startDestination = "splash",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable("splash") {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                LaunchedEffect(Unit) {
                    val u = withContext(Dispatchers.IO) { AppServices.authRepository.bootstrapUser() }
                    user = u
                    if (u != null) {
                        nav.navigate("dashboard") { popUpTo("splash") { inclusive = true } }
                    } else {
                        nav.navigate("login") { popUpTo("splash") { inclusive = true } }
                    }
                }
            }
            composable("login") {
                LoginScreen(
                    onLoggedIn = {
                        scope.launch {
                            user = withContext(Dispatchers.IO) { AppServices.authRepository.fetchMe() }
                        }
                        nav.navigate("dashboard") {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onRegister = { nav.navigate("register") },
                    onError = { snackMsg(it) },
                )
            }
            composable("register") {
                RegisterScreen(
                    onDone = { nav.popBackStack() },
                    onLogin = { nav.popBackStack() },
                    onError = { snackMsg(it) },
                    onSuccess = { snackMsg(it) },
                )
            }
            composable("dashboard") {
                DashboardScreen(
                    onSeeAllEntries = { navigateToTab("entries") },
                    onError = { snackMsg(it) },
                )
            }
            composable("entries") {
                EntriesScreen(
                    onOpenEntry = { id -> nav.navigate("entry_edit/$id") },
                    onError = { snackMsg(it) },
                )
            }
            composable("entry_new") {
                EntryFormScreen(
                    entryId = null,
                    onDone = { nav.popBackStack() },
                    onError = { snackMsg(it) },
                    onSuccess = { snackMsg(it) },
                )
            }
            composable(
                "entry_edit/{id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id")
                EntryFormScreen(
                    entryId = id,
                    onDone = { nav.popBackStack() },
                    onError = { snackMsg(it) },
                    onSuccess = { snackMsg(it) },
                )
            }
            composable("stats") {
                StatisticsScreen(onError = { snackMsg(it) })
            }
            composable("profile") {
                ProfileScreen(
                    user = user,
                    onUserChange = { user = it },
                    onAccounts = { nav.navigate("accounts") },
                    onCategories = { nav.navigate("categories") },
                    onPassword = { nav.navigate("password") },
                    onLogout = {
                        user = null
                        nav.navigate("login") {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    },
                    onError = { snackMsg(it) },
                    onSuccess = { snackMsg(it) },
                )
            }
            composable("categories") {
                CategoriesScreen(onError = { snackMsg(it) }, onSuccess = { snackMsg(it) })
            }
            composable("accounts") {
                AccountsScreen(onError = { snackMsg(it) }, onSuccess = { snackMsg(it) })
            }
            composable("password") {
                ChangePasswordScreen(
                    onDone = { nav.popBackStack() },
                    onError = { snackMsg(it) },
                    onSuccess = { snackMsg(it) },
                )
            }
        }
    }
}

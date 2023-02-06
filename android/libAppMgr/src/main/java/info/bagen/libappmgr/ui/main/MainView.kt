package info.bagen.libappmgr.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import info.bagen.libappmgr.R
import info.bagen.libappmgr.entity.DAppInfoUI
import info.bagen.libappmgr.ui.app.AppInfoGridView
import info.bagen.libappmgr.ui.app.AppViewModel

@Composable
fun MainView(
    mainViewModel: MainViewModel,
    appViewModel: AppViewModel,
    onSearchAction: ((SearchAction, String) -> Unit)? = null,
    onOpenApp: ((appId: String, dAppInfo: DAppInfoUI?) -> Unit)? = null
) {
    val navController = rememberNavController()
    Scaffold(bottomBar = { /*MainBottomNav(navController, mainViewModel)*/ }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = RouteScreen.Home.route,
            modifier = Modifier
                .padding(innerPadding)
                .background(MaterialTheme.colors.primary)
        ) {
            composable(RouteScreen.Home.route) {
                MainHomeView(appViewModel, onSearchAction, onOpenApp)
            }

            composable(RouteScreen.Contact.route) {
                MainContactView()
            }

            composable(RouteScreen.Message.route) {
                MainMessageView()
            }

            composable(RouteScreen.Me.route) {
                MainMeView()
            }
        }
    }
}

@Composable
fun MainBottomNav(navController: NavHostController, mainViewModel: MainViewModel) {
    BottomNavigation(backgroundColor = MaterialTheme.colors.primaryVariant, elevation = 6.dp) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        mainViewModel.navList.forEach { screen ->
            val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
            BottomNavigationItem(
                icon = {
                    Icon(
                        imageVector = screen.image,
                        contentDescription = null,
                        tint = if (selected) Color(0xFF1992FF) else MaterialTheme.colors.onSecondary
                    )
                },
                label = {
                    Text(
                        text = stringResource(screen.resourceId),
                        color = if (selected) Color(0xFF1992FF) else MaterialTheme.colors.onSecondary
                    )
                },
                selected = selected,
                onClick = {
                    navController.navigate(screen.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselect the same item
                        launchSingleTop = true
                        // Restore state when reselect a previously selected item
                        restoreState = true
                    }
                })
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun MainHomeView(
    appViewModel: AppViewModel,
    onSearchAction: ((SearchAction, String) -> Unit)? = null,
    onOpenApp: ((appId: String, dAppInfo: DAppInfoUI?) -> Unit)? = null
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {

                    val gradient = listOf(
                        Color(0xFF71D78E), Color(0xFF548FE3)
                    )
                    Text(
                        text = stringResource(id = R.string.app_name),
                        modifier = Modifier.align(Alignment.BottomCenter),
                        style = TextStyle(
                            brush = Brush.linearGradient(gradient),
                            fontSize = 50.sp
                        )
                    )
                }
                MainSearchView(onSearchAction)
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AppInfoGridView(appViewModel = appViewModel, onOpenApp)
        }
    }
}

@Composable
fun MainContactView() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "联系人列表界面", modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun MainMessageView() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "消息列表界面", modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
fun MainMeView() {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(text = "我的界面", modifier = Modifier.align(Alignment.Center))
    }
}


@Composable
fun MainSearchView(onSearchAction: ((SearchAction, String) -> Unit)? = null) {
    //var inputText by remember { mutableStateOf("http://linge.plaoc.com/index.html") }
    var inputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    BasicTextField(
        value = inputText,
        onValueChange = { inputText = it },
        readOnly = false,
        enabled = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(36.dp, 36.dp, 36.dp, 24.dp),
        singleLine = true,
        textStyle = TextStyle.Default.copy(color = MaterialTheme.colors.onPrimary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            if (inputText.isEmpty()) return@KeyboardActions
            focusManager.clearFocus() // 取消聚焦，就会间接的隐藏键盘
            val url = if (inputText.startsWith("http") || inputText.startsWith("https")) {
                inputText
            } else {
                "https://cn.bing.com/search?q=${inputText}"
            }
            onSearchAction?.let { it(SearchAction.Search, url) }
            // Toast.makeText(AppContextUtil.sInstance, "搜索：$url", Toast.LENGTH_SHORT).show()
        })
    ) { innerTextField ->
        Box {
            Surface(
                border = BorderStroke(2.dp, MaterialTheme.colors.onPrimary),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colors.primary
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onPrimary
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp, end = 4.dp)
                    ) {
                        if (inputText.isEmpty()) Text(
                            text = "请输入必应搜索关键字",
                            color = MaterialTheme.colors.onSecondary
                        )
                        innerTextField()
                    }

                    if (inputText.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.clickable { inputText = "" }
                        )
                    } else {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_photo_camera_24),
                            contentDescription = null,
                            tint = MaterialTheme.colors.onPrimary,
                            modifier = Modifier.clickable {
                                // Toast.makeText(AppContextUtil.sInstance, "打开扫码界面", Toast.LENGTH_SHORT).show()
                                onSearchAction?.let { it(SearchAction.OpenCamera, "") }
                            }
                        )
                    }
                }
            }
        }
    }
}

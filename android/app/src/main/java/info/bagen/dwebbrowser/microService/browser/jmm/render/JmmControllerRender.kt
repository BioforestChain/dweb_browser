package info.bagen.dwebbrowser.microService.browser.jmm.render

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import info.bagen.dwebbrowser.microService.browser.jmm.JmmController
import info.bagen.dwebbrowser.microService.browser.jmm.render.appinstall.Render
import info.bagen.dwebbrowser.microService.browser.jmm.render.manager.Render
import kotlinx.coroutines.launch
import org.dweb_browser.window.core.WindowRenderScope
import org.dweb_browser.window.render.LocalWindowController

@Composable
fun JmmController.Render(modifier: Modifier, renderScope: WindowRenderScope) {
  Box(modifier = with(renderScope) {
    modifier
      .requiredSize((width / scale).dp, (height / scale).dp) // 原始大小
      .scale(scale)
  }) {
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()

    navController.enableOnBackPressed(false);
    
    val win = LocalWindowController.current
    win.GoBackHandler {
      if (!navController.popBackStack()) {
        scope.launch {
          this@Render.closeSelf()
        }
      }
    }

    val routeToPath by this@Render.routeToPath
    SideEffect {
      if (routeToPath != null) {
        navController.navigate(routeToPath!!)
      }
    }
    NavHost(navController = navController, startDestination = "member") {
      composable("manager") { managerApps.Render() }
      composable(
        "app-install/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType })
      ) { backStackEntry ->
        val installingApp = backStackEntry.arguments?.getString("id")?.let { installingApps[it] }
        if (installingApp != null) {
          installingApp.Render()
        } else {
          //404
          Button(onClick = { navController.navigate("manager") }) {
            Text("管理页面")
          }
          val scope = rememberCoroutineScope()
          Button(onClick = {
            scope.launch {
              closeSelf()
            }
          }) {
            Text("关闭")
          }
        }
      }/*...*/
    }
  }
}
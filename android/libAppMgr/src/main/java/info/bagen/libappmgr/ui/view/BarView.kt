package info.bagen.libappmgr.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.statusBarsHeight
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import info.bagen.libappmgr.R

@Composable
fun TopBarView(
    backIcon: Int = R.drawable.ic_back,
    onBackAction: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(android.R.attr.actionBarSize.dp)
    ) {
        /*Image(
          imageVector = ImageVector.vectorResource(id = backIcon),
          contentDescription = "",
          modifier = Modifier
            .padding(4.dp)
            .clickable { onBackAction })*/

        Text(text = "ceshi")
        content
    }
}

@Composable
fun BottomBarView() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(android.R.attr.actionBarSize.dp)
    ) {

    }
}

/**
 * 自定义系统状态栏
 * 使用该方法进行沉浸,还需在 Activity 中设置 WindowCompat.setDecorFitsSystemWindows(window, false)
 * 沉浸状态栏设置
 * @param color Color 状态栏颜色
 * @param darkIcons Boolean 是否是深色模式
 * @param content [@androidx.compose.runtime.Composable] Function0<Unit> 布局内容
 */
@Composable
fun ImmersionStatusBar(
    color: Color = Color.Transparent,
    darkIcons: Boolean = MaterialTheme.colors.isLight,
    background: Color = MaterialTheme.colors.background,
    content: @Composable () -> Unit,
) {
    ProvideWindowInsets {
        rememberSystemUiController().run {
            // 设置状态栏颜色
            setStatusBarColor(
                color = color,
                darkIcons = darkIcons
            )
            // 将状态栏和导航栏设置为color
            setSystemBarsColor(color = color, darkIcons = darkIcons)
            // 设置导航栏颜色
            setNavigationBarColor(color = color, darkIcons = darkIcons)
        }
        Column(
            Modifier
                .background(background)
                .fillMaxSize(),
        ) {
            // 因为系统状态栏被隐藏掉,需要创建一个自定义头部,高度为系统栏高度
            Spacer(modifier = Modifier
                .statusBarsHeight()
                .fillMaxWidth())
            content()
            // 因为系统导航栏被隐藏掉,需要创建一个自定义导航,高度为导航栏高度
            // Spacer(modifier = Modifier.navigationBarsHeight().fillMaxWidth())
        }
    }
}

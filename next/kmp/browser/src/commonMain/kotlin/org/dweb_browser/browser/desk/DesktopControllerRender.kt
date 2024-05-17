package org.dweb_browser.browser.desk

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.animateRectAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Error
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ktor.client.request.forms.formData
import org.dweb_browser.browser.desk.upgrade.NewVersionView
import org.dweb_browser.core.module.NativeMicroModule
import org.dweb_browser.dwebview.Render
import org.dweb_browser.helper.compose.LocalCompositionChain
import org.dweb_browser.helper.platform.SetSystemBarsColor
import org.dweb_browser.sys.window.core.constant.LocalWindowMM
import org.dweb_browser.sys.window.render.SceneRender
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.rememberAsyncImagePainter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.dweb_browser.core.help.types.MMID
import org.dweb_browser.core.std.dns.nativeFetch
import org.dweb_browser.helper.platform.toImageBitmap
import org.dweb_browser.sys.window.render.AppIcon
import org.dweb_browser.sys.window.render.LocalWindowController
import org.dweb_browser.sys.window.render.LocalWindowLimits
import org.dweb_browser.sys.window.render.LocalWindowsManager
import org.dweb_browser.sys.window.render.imageFetchHook
import kotlin.math.roundToInt
import kotlin.random.Random

@Composable
fun DesktopController.Render(
  taskbarController: TaskbarController,
  microModule: NativeMicroModule.NativeRuntime,
) {
  // TODO 这里的颜色应该是自动适应的，特别是窗口最大化的情况下，遮盖了顶部 status-bar 的时候，需要根据 status-bar 来改变颜色
  SetSystemBarsColor(Color.Transparent, if (isSystemInDarkTheme()) Color.White else Color.Black)
  LocalCompositionChain.current.Provider(
    LocalWindowMM provides microModule,
  ) {
    Box(modifier = Modifier.fillMaxWidth()) {

      // Mike: 桌面 -> 替换为新的原生的desktop实现
//      DesktopView {
//        Render(Modifier.fillMaxSize())
//      }

      // Mike:
      NewDesktopView(taskbarController, microModule)
      /// 窗口视图

      // Mike: app的窗口渲染 --不需要改动
      DesktopWindowsManager {
        SceneRender()
      }
    }
    /// 悬浮框
    Box(contentAlignment = Alignment.TopStart) {
      taskbarController.TaskbarView { FloatWindow() }
//      Box(modifier = Modifier.width(100.dp).height(200.dp).background(color = Color.Red)) {
//        Text("taskbar")
//      }
    }

    /// 错误信息
    for (message in alertMessages) {
      key(message) {
        val dismissHandler = {
          alertMessages.remove(message);
        }
        AlertDialog(onDismissRequest = {
          dismissHandler()
        }, icon = {
          Icon(Icons.TwoTone.Error, contentDescription = "error")
        }, title = {
          Text(message.title)
        }, text = {
          Text(message.message)
        }, confirmButton = {
          Button(onClick = { dismissHandler() }) {
            Text("关闭")
          }
        })
      }
      break
    }
    /// 新版本
    newVersionController.NewVersionView()
  }
}


data class DesktopAppModel(val name: String, val mmid: MMID, val icon: String) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class != other::class) return false

    other as DesktopAppModel

    if (name != other.name) return false
    if (mmid != other.mmid) return false

    return true
  }

  override fun hashCode(): Int {
    var result = name.hashCode()
    result = 31 * result + mmid.hashCode()
    return result
  }
}

@Composable
fun NewDesktopView(
  taskbarController: TaskbarController,
  microModule: NativeMicroModule.NativeRuntime,
) {
  val apps = remember {
    mutableStateListOf<DesktopAppModel>()
  }

  val searchWord = remember {
    mutableStateOf(TextFieldValue(""))
  }

  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  fun doHideKeyboard() {
    keyboardController?.hide()
    focusManager.clearFocus()
  }

  fun doSearch() {
    println("Mike search: ${searchWord.value}")
  }

  LaunchedEffect(Unit) {
    val installApps = taskbarController.desktopController.getDesktopApps().map {
      val icon = it.icons.firstOrNull()?.src ?: ""
      DesktopAppModel(it.name, it.mmid, icon)
    }
    apps.clear()
    apps.addAll(installApps)
  }

  var moved by remember { mutableStateOf(0) }
  val offset by animateIntOffsetAsState(
    targetValue = if (moved < 0) {
      IntOffset(moved, 0)
    } else {
      IntOffset.Zero
    },
    label = "offset"
  )

  var searchIconAlpha by remember { mutableStateOf(0F)}
  val searchIconBgColor by animateColorAsState(
    targetValue = Color.Blue.copy(alpha = searchIconAlpha),
    label = "bg color"
  )

  fun doSearchTirggleAnimation(on: Boolean, searchTextFieldWidth: Float) {
    moved = -1 * if (on) (searchTextFieldWidth.toInt() / 2 - 40) else 0
    searchIconAlpha = if (on) 1.0F else 0.0F
  }

  Box(
    modifier = Modifier,
    contentAlignment = Alignment.TopStart
  ) {

    desktopBackgroundView(modifier = Modifier.clickable {
      doHideKeyboard()
    })

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.windowInsetsPadding(WindowInsets.safeGestures)
    ) {
      val widthRatio = 0.75f
      Row {
        BoxWithConstraints(modifier = Modifier.weight(9f)) {
          BasicTextField(
            searchWord.value,
            onValueChange = {
              searchWord.value = it
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
              onSearch = {
                doHideKeyboard()
                doSearch()
              }),
            decorationBox = {
              Box(contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth()){

                Icon(
                  imageVector = Icons.Outlined.Search,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.offset {
                    offset
                  }
                    .background(searchIconBgColor)
                    .padding(8.dp)
                )

                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 30.dp)
                ) {
                  Box(modifier = Modifier.fillMaxWidth()){
                    it()
                  }
                }
              }
            },
            modifier = Modifier
              .onFocusChanged {
                doSearchTirggleAnimation(it.isFocused, constraints.maxWidth * widthRatio)
              }
              .fillMaxWidth(widthRatio)
              .height(44.dp)
              .background(color = Color.Transparent)
              .clip(RoundedCornerShape(22.dp))
              .border(1.dp, color = Color.Gray, shape = RoundedCornerShape(22.dp))
              .shadow(
                elevation = 3.dp, shape = RoundedCornerShape(22.dp)
              )
          )
        }

        Icon(imageVector = Icons.Filled.Search,
          contentDescription = null,
          modifier = Modifier.weight(1f)
          )
      }

      LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing), //TODO: Mike 设置安全区域。
        contentPadding = PaddingValues(10.dp)
      ) {
        items(apps) { app ->
          Box(
            modifier = Modifier
              .padding(5.dp),
            contentAlignment = Alignment.TopStart,
            propagateMinConstraints = false,
          ) {
            Column(modifier = Modifier
              .align(Alignment.CenterStart)
              .pointerInput(UInt) {
                detectTapGestures(onTap = {
                  println("tap")
                  CoroutineScope(Dispatchers.Default).launch {
                    taskbarController.desktopController.open(app.mmid)
                  }
                },
                  onLongPress = {
                    println("long tap")
                  }
                )
              }
            ) {
              AppIcon(
                app.icon,
                modifier = Modifier.fillMaxWidth().padding(8.dp).aspectRatio(1.0f)
                  .background(color = Color.White, shape = RoundedCornerShape(16.dp)),
                iconFetchHook = microModule.imageFetchHook
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = app.name,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth()
                  .background(color = Color.White, shape = RoundedCornerShape(4.dp))
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun desktopBackgroundView(modifier: Modifier) {
  Box(modifier = modifier.fillMaxSize().background(color = Color.Black.copy(alpha = 0.3f))) {
    //TODO: Mike 这边需要设置背景图片。
  }
}

fun getRandomColor(): Color {
  val red = Random.nextInt(256)
  val green = Random.nextInt(256)
  val blue = Random.nextInt(256)
  return Color(red = red, green = green, blue = blue)
}


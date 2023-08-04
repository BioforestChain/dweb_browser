package info.bagen.dwebbrowser.microService.browser.desk

import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import info.bagen.dwebbrowser.App
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect
import org.dweb_browser.browserUI.ui.view.findActivity
import org.dweb_browser.dwebview.DWebView
import kotlin.math.roundToInt

//一个全局的ViewModel
@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.taskAppViewModels(
  noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null
): Lazy<VM> {
  val factoryPromise = factoryProducer ?: {
    defaultViewModelProviderFactory
  }
  return ViewModelLazy(VM::class, { taskAppViewModelStore }, factoryPromise)
}

val taskAppViewModelStore: ViewModelStore by lazy {
  ViewModelStore()
}

// 定义 TaskbarViewModel
class TaskbarViewModel : ViewModel() {
  private var controller: TaskBarController? = null
  fun initViewModel(controller: TaskBarController, sessionId: String) {
    this.controller = controller
    this.taskbarSessionId = sessionId
  }

  var taskbarSessionId: String? = null
  val taskBarController by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    controller ?: throw Exception("taskBarController is null !!! ")
  }
  val taskbarDWebView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
    DWebView(
      context = App.appContext,
      remoteMM = taskBarController.desktopNMM,
      options = DWebView.Options(
        url = "",
        onDetachedFromWindowStrategy = DWebView.Options.DetachedFromWindowStrategy.Ignore,
      )
    ).also {
      it.setBackgroundColor(Color.Transparent.toArgb())
    }
  }

  val width get() = taskbarDWebView.width
  val height get() = taskbarDWebView.height
  val isLoaded get() = taskbarDWebView.url != null

  val floatViewState: MutableState<Boolean> = mutableStateOf(true)

  fun openTaskActivity() {
    taskbarSessionId?.let { sessionId ->
      App.appContext.startActivity(Intent(App.appContext, TaskbarActivity::class.java).also {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        it.putExtras(Bundle().also { bundle ->
          bundle.putString("taskBarSessionId", sessionId)
        })
      })
    } ?: throw Exception("taskbarSessionId is null !!! ")
  }
}

@Composable
fun FloatTaskbarView(width: Dp = 72.dp, height: Dp = 72.dp) {
  val taskbarViewModel = LocalContext.current.findActivity().let {
    val model by it.taskAppViewModels<TaskbarViewModel>()
    model
  }
  if (taskbarViewModel.floatViewState.value) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current.density
    val rememberOffset = remember {
      val offset = Offset(x = (screenWidth - width).value * density, y = 200.dp.value * density)
      mutableStateOf(offset)
    }
    Box(modifier = Modifier
      .offset {
        IntOffset(rememberOffset.value.x.roundToInt(), rememberOffset.value.y.roundToInt())
      }
      .pointerInput(rememberOffset) {
        detectDragGestures { _, dragAmount ->
          rememberOffset.value = Offset(
            x = rememberOffset.value.x + dragAmount.x, y = rememberOffset.value.y + dragAmount.y
          )
        }
      }
      .size(width, height)
      .clip(CircleShape)
    ) {
      AndroidView(factory = {
        taskbarViewModel.taskbarDWebView.also { webView ->
          webView.parent?.let { parent ->
            (parent as ViewGroup).removeView(webView)
          }
          if (!taskbarViewModel.isLoaded) {
            webView.loadUrl(taskbarViewModel.taskBarController.getTaskbarUrl().toString())
          }
        }
      })
      // 这边屏蔽当前webview响应
      Box(modifier = Modifier
        .fillMaxSize()
        .clickableWithNoEffect {
          taskbarViewModel.floatViewState.value = false
          taskbarViewModel.openTaskActivity()
        })
    }
  }
}
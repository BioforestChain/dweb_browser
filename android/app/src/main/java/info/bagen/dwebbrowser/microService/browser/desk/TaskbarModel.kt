package info.bagen.dwebbrowser.microService.browser.desk

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.annotation.MainThread
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelLazy
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import info.bagen.dwebbrowser.App
import org.dweb_browser.dwebview.DWebView

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
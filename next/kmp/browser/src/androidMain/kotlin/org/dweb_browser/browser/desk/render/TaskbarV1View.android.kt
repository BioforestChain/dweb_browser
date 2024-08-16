package org.dweb_browser.browser.desk.render

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import org.dweb_browser.browser.desk.AndroidTaskbarView
import org.dweb_browser.browser.desk.TaskbarActivity
import org.dweb_browser.browser.desk.TaskbarV1Controller
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.Render
import org.dweb_browser.dwebview.asAndroidWebView
import org.dweb_browser.sys.window.helper.FloatBarMover
import org.dweb_browser.sys.window.helper.FloatBarShell

actual suspend fun ITaskbarV1View.Companion.create(
  controller: TaskbarV1Controller,
  webview: IDWebView,
): ITaskbarV1View = TaskbarV1View(controller, webview)

class TaskbarV1View(
  private val taskbarController: TaskbarV1Controller, override val taskbarDWebView: IDWebView,
) : ITaskbarV1View(taskbarController), AndroidTaskbarView {
  @Composable
  override fun Render() {
    val isActivityMode by state.floatActivityStateFlow.collectAsState()
    if (isActivityMode) {
      /**
       * 全局的浮动窗口，背景高斯模糊
       */
      val context = LocalContext.current
      LaunchedEffect(context) {
        context.startActivity(Intent(
          context, TaskbarActivity::class.java
        ).also { intent ->
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
          intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
          intent.putExtras(Bundle().apply {
            putString("deskSessionId", taskbarController.deskSessionId)
          })
        })
      }
    } else {
      val displaySize = rememberAndroidDisplaySize()
      FloatBarShell(state) { modifier ->
        FloatBarMover(draggableDelegate, modifier) {
          InnerRender(displaySize)
        }
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  @Composable
  override fun InnerRender(displaySize: Size) {
    taskbarDWebView.Render(onCreate = {
      setHorizontalScrollBarVisible(false)
      asAndroidWebView().setOnTouchListener { _, _ ->
        false
      }
    })
  }
}

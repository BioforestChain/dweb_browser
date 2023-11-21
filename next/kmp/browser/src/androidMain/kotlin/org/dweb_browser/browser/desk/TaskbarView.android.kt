package org.dweb_browser.browser.desk

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import org.dweb_browser.core.module.getAppContext
import org.dweb_browser.dwebview.DWebViewOptions
import org.dweb_browser.dwebview.IDWebView
import org.dweb_browser.dwebview.create

actual suspend fun ITaskbarView.Companion.create(taskbarController: TaskbarController): ITaskbarView =
  TaskbarView.from(taskbarController)

class TaskbarView private constructor(
  private val taskbarController: TaskbarController, override val taskbarDWebView: IDWebView
) : ITaskbarView(taskbarController) {
  companion object {
    suspend fun from(taskbarController: TaskbarController): ITaskbarView {
      val dwebView = IDWebView.create(
        context = taskbarController.deskNMM.getAppContext(),
        remoteMM = taskbarController.deskNMM,
        options = DWebViewOptions(
          url = taskbarController.getTaskbarUrl().toString(),
          detachedStrategy = DWebViewOptions.DetachedStrategy.Ignore,
          privateNet = true,
        )
      )
      return TaskbarView(taskbarController, dwebView)
    }
  }

  @Composable
  override fun LocalFloatWindow() {
    NormalFloatWindow()
  }

  @SuppressLint("IntentWithNullActionLaunch")
  @Composable
  override fun GlobalFloatWindow() {
    val context = LocalContext.current
    LaunchedEffect(state.composableHelper) {
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
  }
}

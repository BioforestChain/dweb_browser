package org.dweb_browser.helper.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.uikit.ComposeUIViewControllerDelegate
import androidx.compose.ui.window.ComposeUIViewController

@Composable
actual fun NativeDialog(
  onCloseRequest: () -> Unit,
  properties: NativeDialogProperties,
  setContent: @Composable () -> Unit,
) {
  val parentController = LocalUIViewController.current
  val closeRequestState = rememberUpdatedState(onCloseRequest)
  val setContentState = rememberUpdatedState(setContent)
  val compositionLocalContextState = rememberUpdatedState(currentCompositionLocalContext)
  val dialogController = remember {
    ComposeUIViewController({
      this.delegate = object : ComposeUIViewControllerDelegate {
        override fun viewWillAppear(animated: Boolean) {
          super.viewWillAppear(animated)
          closeRequestState.value()
        }
      }
    }) {
      CompositionLocalProvider(compositionLocalContextState.value) {
        setContentState.value()
      }
    }
  }
  DisposableEffect(parentController) {
    parentController.presentModalViewController(
      modalViewController = dialogController,
      animated = true
    )
    onDispose {
      dialogController.dismissModalViewControllerAnimated(true)
    }
  }
}

//class UIComposeViewController : UIViewController(nibName = null, bundle = null) {
//  val composeView = ComposeView
//  override fun viewDidLoad() {
//    super.viewDidLoad()
//
//  }
//}
package info.bagen.rust.plaoc.webView.dialog


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.DialogProperties

@Composable
fun JsAlertConfiguration.openAlertDialog(requestDismiss: () -> Unit) {
    val config = this
    val closeAlert = {
        this.onConfirm()
        requestDismiss()
    }
    AlertDialog(
        icon = { Icon(Icons.Filled.Notifications, contentDescription = "Alert") },
        onDismissRequest = closeAlert,
        title = { Text(text = config.title) },
        text = { Text(text = config.content) },
        confirmButton = {
            TextButton(onClick = closeAlert) { Text(config.confirmText) }
        },
        properties = DialogProperties(
            dismissOnBackPress = config.dismissOnBackPress,
            dismissOnClickOutside = config.dismissOnClickOutside
        ),
    )
}

@Composable
fun JsPromptConfiguration.openPromptDialog(requestDismiss: () -> Unit) {
    val config = this
    var resultText by remember {
        mutableStateOf(this.defaultValue)
    }
    val submitPrompt = {
        this.onSubmit(resultText)
        requestDismiss()
    }
    val cancelPrompt = {
        this.onCancel()
        requestDismiss()
    }
    AlertDialog(
        icon = { Icon(Icons.Filled.Edit, contentDescription = "Prompt") },
        onDismissRequest = cancelPrompt,
        title = { Text(text = this.title) },
        text = {
            TextField(
                value = resultText,
                singleLine = true,
                onValueChange = {
                    resultText = it
                }, label = { Text(text = this.label) }
            )
        },
        confirmButton = {
            TextButton(onClick = submitPrompt) { Text(config.confirmText) }
        },
        dismissButton = {
            TextButton(onClick = cancelPrompt) { Text(config.cancelText) }
        },
        properties = DialogProperties(
            dismissOnBackPress = this.dismissOnBackPress,
            dismissOnClickOutside = this.dismissOnClickOutside
        ),
    )
}

@Composable
fun JsConfirmConfiguration.openConfirmDialog(requestDismiss: () -> Unit) {
    val config = this
    val submitConfirm = {
        this.onConfirm()
        requestDismiss()
    }
    val cancelConfirm = {
        this.onCancel()
        requestDismiss()
    }
    AlertDialog(
        icon = { Icon(Icons.Filled.QuestionMark, contentDescription = "Confirm") },
        onDismissRequest = cancelConfirm,
        title = { Text(text = config.title) },
        text = { Text(text = config.message) },
        confirmButton = {
            TextButton(onClick = submitConfirm) { Text(config.confirmText) }
        },
        dismissButton = {
            TextButton(onClick = cancelConfirm) { Text(config.cancelText) }
        },
        properties = DialogProperties(
            dismissOnBackPress = config.dismissOnBackPress,
            dismissOnClickOutside = config.dismissOnClickOutside
        ),
    )
}

@Composable
fun JsConfirmConfiguration.openWarningDialog(requestDismiss: () -> Unit) {
    val config = this
    val submitConfirm = {
        this.onConfirm()
        requestDismiss()
    }
    val cancelConfirm = {
        this.onCancel()
        requestDismiss()
    }
    AlertDialog(
        icon = { Icon(Icons.Filled.Warning, contentDescription = "warning") }, // 图片和无障碍服务
        onDismissRequest = cancelConfirm,
        title = { Text(text = config.title) },
        text = { Text(text = config.message) },
        confirmButton = {
            TextButton(onClick = submitConfirm) { Text(config.confirmText) }
        },
        dismissButton = {
            TextButton(onClick = cancelConfirm) { Text(config.cancelText) }
        },
        properties = DialogProperties(
            dismissOnBackPress = config.dismissOnBackPress,
            dismissOnClickOutside = config.dismissOnClickOutside
        ),
    )
}

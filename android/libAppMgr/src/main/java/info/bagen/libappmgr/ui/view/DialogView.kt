package info.bagen.libappmgr.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

data class DialogInfo(
    val type: DialogType = DialogType.HIDE,
    val title: String = "标题",
    val text: String = "内容",
    val confirmText: String = "确认",
    val cancelText: String = "取消",
    val progress: Float = 0f,
)

enum class DialogType {
    HIDE, CUSTOM, PROGRESS, LOADING
}

@Composable
fun DialogView(
    dialogInfo: DialogInfo, onConfirm: (() -> Unit)? = null, onCancel: (() -> Unit)? = null
) {
    when (dialogInfo.type) {
        DialogType.CUSTOM -> {
            CustomAlertDialog(dialogInfo = dialogInfo, onCancel = onCancel, onConfirm = onConfirm)
        }
        DialogType.PROGRESS -> {
            ProgressDialog(dialogInfo = dialogInfo, onCancel = onCancel)
        }
        DialogType.LOADING -> {
            LoadingDialog(dialogInfo = dialogInfo, onCancel = onCancel)
        }
    }
}

/**
 * 显示普通的提示框
 */
@Composable
private fun CustomAlertDialog(
    dialogInfo: DialogInfo, onConfirm: (() -> Unit)? = null, onCancel: (() -> Unit)? = null
) {
    AlertDialog(modifier = Modifier.padding(20.dp),
        onDismissRequest = { onCancel?.let { onCancel() } }, // 点击对话框周围空白部分
        title = {
            Text(text = dialogInfo.title)
        },
        text = { Text(text = dialogInfo.text) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm?.let { onConfirm() } // 回调通知点击了确认按钮
                },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.onPrimary)
            ) {
                Text(text = dialogInfo.confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onCancel?.let { onCancel() } },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colors.onPrimary)
            ) {
                Text(text = dialogInfo.cancelText)
            }
        })
}

/**
 * 显示加载提示框
 */
@Composable
private fun LoadingDialog(
    dialogInfo: DialogInfo, onCancel: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = { onCancel?.let { onCancel() } }) {
        Box(
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左边显示 加载动画，右边显示“正在加载中，请稍后..."
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(22.dp))
                Text(text = "正在加载中，请稍后...")
            }
        }
    }
}

/**
 * 转圈圈的下载进度显示
 */
@Composable
private fun ProgressDialog(
    dialogInfo: DialogInfo, onCancel: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = {}) {
        Box(
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth()
                .padding(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .padding(10.dp)
                    .align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左边显示 加载动画，右边显示“正在加载中，请稍后..."
                CircularProgressIndicator(progress = dialogInfo.progress)
                Spacer(modifier = Modifier.width(18.dp))
                var pro = String.format("%02d", (dialogInfo.progress * 100).toInt())
                Text(text = "正在加载中，请稍后... $pro%")
            }
        }
    }
}

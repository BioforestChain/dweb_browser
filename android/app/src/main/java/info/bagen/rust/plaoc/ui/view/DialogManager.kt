package info.bagen.rust.plaoc.ui.view

import android.content.Context
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog

object DialogManager {

    fun showCustomDialog(
        context: Context,
        title: String = "对话框",
        message: String = "提示内容",
        cancelable: Boolean = false,
        postBtn: String = "确定",
        negBtn: String = "取消",
        onPostClick: (dialog: DialogInterface, which: Int) -> Unit,
        onNegClick: (dialog: DialogInterface, which: Int) -> Unit
    ) {
        var build = AlertDialog.Builder(context)
        build.setTitle(title)
        build.setMessage(message)
        build.setCancelable(cancelable)
        build.setPositiveButton(postBtn, onPostClick)
        build.setNegativeButton(negBtn, onNegClick)
        build.show()
    }
}

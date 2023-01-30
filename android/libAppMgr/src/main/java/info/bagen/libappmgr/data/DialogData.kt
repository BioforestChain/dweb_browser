package info.bagen.libappmgr.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import info.bagen.libappmgr.entity.AppVersion

/**
 * 获取客户定义的对话框默认值
 */
fun rememberCustomData(): CustomDialogData {
    return CustomDialogData()
}

/**
 * 获取客户定义的对话框默认值
 */
fun rememberProgressData(): ProgressDialogData {
    return ProgressDialogData()
}

/**
 * 获取Dialog状态值
 */
fun rememberDialogAllState(
    customDialog: CustomDialogData = rememberCustomData(),
    progressDialog: ProgressDialogData = rememberProgressData()
): DialogShowOrHide {
    return DialogShowOrHide(customDialog = customDialog, progressDialog = progressDialog)
}

/**
 * 对话框提示界面的内容
 */
data class CustomDialogData(
    var title: MutableState<String> = mutableStateOf("提示"),
    var content: MutableState<String> = mutableStateOf("请输入内容"),
    var confirmText: MutableState<String> = mutableStateOf("确定"),
    var dismissText: MutableState<String> = mutableStateOf("取消")
)

fun CustomDialogData.update(
    title: String? = null,
    content: String? = null,
    confirmText: String? = null,
    dismissText: String? = null
) {
    this.title.value = title?.let { it } ?: this.title.value
    this.content.value = content?.let { it } ?: this.content.value
    this.confirmText.value = confirmText?.let { it } ?: this.confirmText.value
    this.dismissText.value = dismissText?.let { it } ?: this.dismissText.value
}

fun CustomDialogData.updateNewVersion(
    appVersion: AppVersion? = null
) {
    update(
        title = "版本更新",
        content = appVersion?.let { "版本信息：${it.version}\n更新内容：${it.releaseNotes}" } ?: "暂无版本信息",
        confirmText = "下载",
        dismissText = "取消")
}

/**
 * 针对需要显示进度的对话框
 */
data class ProgressDialogData(
    var progress: MutableState<Float> = mutableStateOf(0f)
)

/**
 * 用于标识对话框是否显示，目前包括:
 * CustomAlertDialog, LoadingDialog, ProgressDialog
 */
data class DialogShowOrHide(
    var customShow: MutableState<Boolean> = mutableStateOf(false),
    var loadingShow: MutableState<Boolean> = mutableStateOf(false),
    var progressShow: MutableState<Boolean> = mutableStateOf(false),
    var customDialog: CustomDialogData = rememberCustomData(),
    var progressDialog: ProgressDialogData = rememberProgressData(),
)

/**
 * 根据传入值进行显示或者隐藏，默认情况这些对话框只需要显示一个，所以只要指定哪个对话框值即可
 */
fun DialogShowOrHide.updateState(
    customShow: Boolean? = null,
    loadingShow: Boolean? = null,
    progressShow: Boolean? = null,
): DialogShowOrHide {
    this.customShow.value = customShow?.let { customShow } ?: false
    this.loadingShow.value = loadingShow?.let { loadingShow } ?: false
    this.progressShow.value = progressShow?.let { progressShow } ?: false
    return this
}

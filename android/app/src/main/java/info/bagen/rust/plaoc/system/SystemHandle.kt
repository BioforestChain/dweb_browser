package info.bagen.rust.plaoc.system

import android.util.Log
import info.bagen.libappmgr.utils.APP_DIR_TYPE
import info.bagen.libappmgr.utils.FilesUtil
import info.bagen.rust.plaoc.*
import info.bagen.rust.plaoc.system.camera.*
import info.bagen.rust.plaoc.system.clipboard.Clipboard
import info.bagen.rust.plaoc.system.clipboard.ClipboardWriteOption
import info.bagen.rust.plaoc.system.device.DeviceInfo
import info.bagen.rust.plaoc.system.device.Network
import info.bagen.rust.plaoc.system.file.*
import info.bagen.rust.plaoc.system.fileopener.FileOpener
import info.bagen.rust.plaoc.system.fileopener.FileOpenerOption
import info.bagen.rust.plaoc.system.haptics.*
import info.bagen.rust.plaoc.system.notification.NotificationMsgItem
import info.bagen.rust.plaoc.system.notification.NotifyManager
import info.bagen.rust.plaoc.system.permission.PermissionManager
import info.bagen.rust.plaoc.system.share.Share
import info.bagen.rust.plaoc.system.share.ShareOption
import info.bagen.rust.plaoc.system.toast.Toast
import info.bagen.rust.plaoc.system.toast.ToastOption
import info.bagen.rust.plaoc.webView.jsutil.sendToJavaScript
import info.bagen.rust.plaoc.webView.network.dWebView_host
import info.bagen.rust.plaoc.webView.network.initMetaData
import info.bagen.rust.plaoc.webView.network.uiGateWay

val callable_map = mutableMapOf<ExportNative, (data: String) -> Unit>()
private val fileSystem = FileSystem()
private val notifyManager = NotifyManager()
private val vibrateManage = VibrateManage()
private val networkManager = Network()
private val cameraPlugin = CameraPlugin()


/** 拼接入口*/
fun splicingPath(bfsId: String, entry: String): String {
    if (entry.startsWith("./")) {
        return "$bfsId${entry.replace("./", "/")}"
    }
    if (entry.startsWith("/")) {
        return "$bfsId$entry"
    }
    return "$bfsId/$entry"
}

/** 初始化系统函数*/
fun initSystemFn(activity: MainActivity) {
    callable_map[ExportNative.OpenQrScanner] = { activity.openScannerActivity() }
    callable_map[ExportNative.BarcodeScanner] = { activity.openBarCodeScannerActivity() }
    // 打开/关闭手电筒
    callable_map[ExportNative.ToggleTorch] = {
        if (FlashLightUtils.hasFlashlight()) {
            if (FlashLightUtils.isOn) {
                FlashLightUtils.lightOff()
            } else {
                FlashLightUtils.lightOn()
            }
        }
    }
    // 获取手电筒状态
    callable_map[ExportNative.GetTorchState] = {
        createBytesFactory(ExportNative.GetTorchState, FlashLightUtils.isOn.toString())
    }
    callable_map[ExportNative.OpenDWebView] = {
        activity.openDWebViewActivity(it)
    }
    callable_map[ExportNative.ExitApp] = {
        App.dwebViewActivity?.finish()
    }
    // 初始化用户配置
    callable_map[ExportNative.InitMetaData] = {
        initMetaData(it)
    }
    // 执行ui函数
    callable_map[ExportNative.SetDWebViewUI] = {
        uiGateWay(it)
    }
    callable_map[ExportNative.DenoRuntime] = {
        // denoService.denoRuntime(it) // remove by lin.huang 20230129
    }
    callable_map[ExportNative.ReadOnlyRuntime] = {
        println("ReadOnlyRuntime：$it")
        // denoService.onlyReadRuntime(App.appContext.assets, it) // remove by lin.huang 20230129
    }
    // 返回数据给DWebView-js
    callable_map[ExportNative.EvalJsRuntime] =
        { sendToJavaScript(it) }
    /** fs System*/
    callable_map[ExportNative.FileSystemLs] = {
        val handle = mapper.readValue(it, FileLs::class.java)
        fileSystem.ls(handle.path, handle.option.filter, handle.option.recursive)
    }
    callable_map[ExportNative.FileSystemList] = {
        val handle = mapper.readValue(it, FileLs::class.java)
        fileSystem.list(handle.path)
    }
    callable_map[ExportNative.FileSystemMkdir] = {
        val handle = mapper.readValue(it, FileLs::class.java)
        fileSystem.mkdir(handle.path, handle.option.recursive)
    }
    callable_map[ExportNative.FileSystemWrite] = {
        val handle = mapper.readValue(it, FileWrite::class.java)
        fileSystem.write(handle.path, handle.content, handle.option)
    }
    callable_map[ExportNative.FileSystemRead] = {
        val handle = mapper.readValue(it, FileRead::class.java)
        fileSystem.read(handle.path)
    }
    callable_map[ExportNative.FileSystemReadBuffer] = {
        val handle = mapper.readValue(it, FileRead::class.java)
        fileSystem.readBuffer(handle.path)
    }
    callable_map[ExportNative.FileSystemRename] = {
        val handle = mapper.readValue(it, FileRename::class.java)
        fileSystem.rename(handle.path, handle.newPath)
    }
    callable_map[ExportNative.FileSystemRm] = {
        val handle = mapper.readValue(it, FileRm::class.java)
        fileSystem.rm(handle.path, handle.option.deepDelete)
    }
    callable_map[ExportNative.FileSystemStat] = {
        val handle = mapper.readValue(it, FileStat::class.java)
        Log.i("kotlin#FileSystemStat: ", handle.toString())
        fileSystem.stat(handle.path)
    }
    /**获取appId */
    callable_map[ExportNative.GetBfsAppId] = {
        createBytesFactory(ExportNative.GetBfsAppId, dWebView_host)
    }
    /**deviceInfo */
    callable_map[ExportNative.GetDeviceInfo] = {
        DeviceInfo().getDeviceInfo()
    }
    /**申请应用权限 */
    callable_map[ExportNative.ApplyPermissions] = {
        println("kotlin#ApplyPermissions: $it")
        App.dwebViewActivity?.let { it1 -> PermissionManager.requestPermissions(it1, it) }
    }
    /** Notification */
    callable_map[ExportNative.CreateNotificationMsg] = {
        createBytesFactory(ExportNative.CreateNotificationMsg, it)
        val message = mapper.readValue(it, NotificationMsgItem::class.java)
        val channelType = when (message.msg_src) {
            "app_message" -> NotifyManager.ChannelType.DEFAULT
            "push_message" -> NotifyManager.ChannelType.IMPORTANT
            else -> NotifyManager.ChannelType.DEFAULT
        }
        notifyManager.createNotification(
            title = message.title,
            text = message.msg_content,
            bigText = message.msg_content,
            channelType = channelType,
        )
    }

    /** 读取剪切板 */
    callable_map[ExportNative.ReadClipboardContent] = {
        createBytesFactory(ExportNative.ReadClipboardContent, Clipboard.read())
    }
    /** 写入剪切板*/
    callable_map[ExportNative.WriteClipboardContent] = {
        val writeOption = mapper.readValue(it, ClipboardWriteOption::class.java)
        var result = "true"
        Clipboard.write(
            strValue = writeOption.str,
            imageValue = writeOption.image,
            urlValue = writeOption.url,
            labelValue = writeOption.label ?: "OcrText"
        ) { error ->
            println("writeClipboardContent error: $error")
            result = "false"
        }
        createBytesFactory(ExportNative.WriteClipboardContent, result)
    }

    /** Haptics start */
    /** 触碰物体 */
    callable_map[ExportNative.HapticsImpact] = {
        val option = mapper.readValue(it, ImpactOption::class.java)
        val style = when (option.style) {
            "MEDIUM" -> HapticsImpactType.MEDIUM
            "HEAVY" -> HapticsImpactType.HEAVY
            else -> HapticsImpactType.LIGHT
        }
        vibrateManage.impact(style)
    }
    /** 振动通知 */
    callable_map[ExportNative.HapticsNotification] = {
        val option = mapper.readValue(it, NotificationOption::class.java)
        val type = when (option.type) {
            "SUCCESS" -> HapticsNotificationType.SUCCESS
            "WARNING" -> HapticsNotificationType.WARNING
            "ERROR" -> HapticsNotificationType.ERROR
            else -> null
        }

        if (type != null) {
            vibrateManage.notification(type)
        } else {
            println("HapticsNotification type param error $option")
        }


    }
    /** 反馈振动 */
    callable_map[ExportNative.HapticsVibrate] = {
        val option = mapper.readValue(it, VibrateOption::class.java)
        vibrateManage.vibrate(option.duration)
    }
    callable_map[ExportNative.HapticsVibratePreset] = {
        when (it) {
            "CLICK" -> vibrateManage.vibrateClick()
            "DOUBLE_CLICK" -> vibrateManage.vibrateDoubleClick()
            "HEAVY_CLICK" -> vibrateManage.vibrateHeavyClick()
            "TICK" -> vibrateManage.vibrateTick()
            "DISABLED" -> vibrateManage.vibrateDisabled()
            else -> print("HapticsVibratePreset param error")
        }
    }
    /** Haptics end */

    /** Toast */
    callable_map[ExportNative.ShowToast] = {
        val param = mapper.readValue(it, ToastOption::class.java)
        val duration =
            if (param.duration == "long") Toast.DurationType.LONG else Toast.DurationType.SHORT
        val position = when (param.position) {
            "top" -> Toast.PositionType.valueOf("TOP")
            "center" -> Toast.PositionType.valueOf("CENTER")
            else -> Toast.PositionType.valueOf("BOTTOM")
        }
        Toast.show(text = param.text, durationType = duration, positionType = position, view = null)
    }

    /** Share */
    callable_map[ExportNative.SystemShare] = {
        val param = mapper.readValue(it, ShareOption::class.java)

        Share.share(
            title = param.title,
            text = param.text,
            url = param.url,
            files = param.files,
            dialogTitle = param.dialogTitle ?: "分享到："
        ) { error ->
            println("SystemShare error: $error")
        }
    }

    /** Network */
    callable_map[ExportNative.GetNetworkStatus] = {
        createBytesFactory(
            ExportNative.GetNetworkStatus,
            networkManager.getNetworkStatus().toString()
        )
    }

    /** Camera */
    callable_map[ExportNative.TakeCameraPhoto] = {
        val option = mapper.readValue(it, CameraImageOption::class.java)

        cameraPlugin.getPhoto(option.toCameraSettings()) { result ->
            createBytesFactory(ExportNative.TakeCameraPhoto, result)
        }
    }
    callable_map[ExportNative.PickCameraPhoto] = {
        val option = mapper.readValue(it, CameraImageOption::class.java)

        cameraPlugin.getPhoto(option.toCameraSettings()) { result ->
            createBytesFactory(ExportNative.PickCameraPhoto, result)
        }
    }
    callable_map[ExportNative.PickCameraPhotos] = {
        val option = mapper.readValue(it, CameraGalleryImageOption::class.java)

        cameraPlugin.pickImages(option.toCameraSettings()) { result ->
            createBytesFactory(ExportNative.PickCameraPhotos, result)
        }
    }

    /** FileOpener */
    callable_map[ExportNative.FileOpener] = {
        val option = mapper.readValue(it, FileOpenerOption::class.java)

        FileOpener.open(
            filePath = option.filePath,
            contentType = option.contentType,
            openWithDefault = option.openWithDefault
        ) { error ->
            println("FileOpener error: $error")
        }
    }
}



package info.bagen.rust.plaoc

/** 系统调用函数*/
enum class ExportNative(val type: String) {
    OpenQrScanner("openQrScanner"), // 打开扫码-二维码
    BarcodeScanner("barcodeScanner"), // 打开扫码-条形码
    ToggleTorch("toggleTorch"), // 打开/关闭手电筒
    GetTorchState("getTorchState"), // 获取手电筒状态
    ExitApp("ExitApp"), // 退出app
    OpenDWebView("openDWebView"), // 打开DwebView
    InitMetaData("initMetaData"),
    SetDWebViewUI("setDWebViewUI"),

    DenoRuntime("denoRuntime"),
    ReadOnlyRuntime("readOnlyRuntime"),
    EvalJsRuntime("evalJsRuntime"),

    // vfs
    FileSystemLs("fileSystemLs"),
    FileSystemList("fileSystemList"),
    FileSystemMkdir("fileSystemMkdir"),
    FileSystemWrite("fileSystemWrite"),
    FileSystemRead("fileSystemRead"),
    FileSystemReadBuffer("fileSystemReadBuffer"),
    FileSystemRename("fileSystemRename"),
    FileSystemRm("fileSystemRm"),
    FileSystemStat("fileSystemStat"),

    GetBfsAppId("getBfsAppId"),
    ApplyPermissions("applyPermissions"),
    GetDeviceInfo("getDeviceInfo"),
    CreateNotificationMsg("createNotificationMsg"), // 创建通知消息
    ReadClipboardContent("readClipboardContent"), // 读取剪切板
    WriteClipboardContent("writeClipboardContent"), // 写入剪切板

    // Haptics相关
    HapticsImpact("hapticsImpact"),
    HapticsNotification("hapticsNotification"),
    HapticsVibrate("hapticsVibrate"),
    HapticsVibratePreset("hapticsVibratePreset"),

    // Toast
    ShowToast("showToast"),

    // Share
    SystemShare("systemShare"),

    // Network
    GetNetworkStatus("getNetworkStatus"),

    // Camera
    TakeCameraPhoto("takeCameraPhoto"),
    PickCameraPhoto("pickCameraPhoto"),
    PickCameraPhotos("pickCameraPhotos"),

    // fileOpen
    FileOpener("fileOpener"),
}

/** worker */
enum class WorkerNative {
    ReadOnlyRuntime,
    DenoRuntime,
    WorkerName,
    WorkerData
}

enum class ExportNativeUi(val type: String) {
    // Navigation
    SetNavigationBarVisible("setNavigationBarVisible"),
    SetNavigationBarColor("setNavigationBarColor"),
    GetNavigationBarVisible("getNavigationBarVisible"),
    SetNavigationBarOverlay("setNavigationBarOverlay"),
    GetNavigationBarOverlay("getNavigationBarOverlay"),

    // Status Bar
    GetStatusBarBackgroundColor("getStatusBarBackgroundColor"),
    SetStatusBarBackgroundColor("setStatusBarBackgroundColor"),
    GetStatusBarIsDark("getStatusBarIsDark"),
    GetStatusBarVisible("getStatusBarVisible"),
    SetStatusBarVisible("setStatusBarVisible"),
    GetStatusBarOverlay("getStatusBarOverlay"),
    SetStatusBarOverlay("setStatusBarOverlay"),
    SetStatusBarStyle("setStatusBarStyle"),

    // keyboard
    GetKeyBoardSafeArea("getKeyBoardSafeArea"),
    GetKeyBoardHeight("getKeyBoardHeight"),
    GetKeyBoardOverlay("getKeyBoardOverlay"),
    SetKeyBoardOverlay("setKeyBoardOverlay"),
    ShowKeyBoard("showKeyBoard"),
    HideKeyBoard("hideKeyBoard"),

    // Top Bar
    TopBarNavigationBack("topBarNavigationBack"),
    GetTopBarShow("getTopBarShow"),
    SetTopBarShow("setTopBarShow"),
    GetTopBarOverlay("getTopBarOverlay"),
    SetTopBarOverlay("setTopBarOverlay"),

    //  GetTopBarAlpha("GetTopBarAlpha"),
    SetTopBarAlpha("SetTopBarAlpha"),
    GetTopBarTitle("getTopBarTitle"),
    SetTopBarTitle("setTopBarTitle"),
    HasTopBarTitle("hasTopBarTitle"),
    GetTopBarHeight("getTopBarHeight"),
    GetTopBarActions("getTopBarActions"),
    SetTopBarActions("setTopBarActions"),
    GetTopBarBackgroundColor("getTopBarBackgroundColor"),
    SetTopBarBackgroundColor("setTopBarBackgroundColor"),
    GetTopBarForegroundColor("getTopBarForegroundColor"),
    SetTopBarForegroundColor("setTopBarForegroundColor"),

    // bottomBar
    GetBottomBarEnabled("getBottomBarEnabled"),
    SetBottomBarEnabled("getBottomBarEnabled"),

    //  GetBottomBarAlpha("getBottomBarAlpha"),
    SetBottomBarAlpha("setBottomBarAlpha"),
    SetBottomBarOverlay("SetBottomBarOverlay"),
    GetBottomBarOverlay("GetBottomBarOverlay"),
    GetBottomBarHeight("getBottomBarHeight"),
    SetBottomBarHeight("setBottomBarHeight"),
    GetBottomBarActions("getBottomBarActions"),
    SetBottomBarActions("setBottomBarActions"),
    GetBottomBarBackgroundColor("getBottomBarBackgroundColor"),
    SetBottomBarBackgroundColor("setBottomBarBackgroundColor"),
    GetBottomBarForegroundColor("getBottomBarForegroundColor"),
    SetBottomBarForegroundColor("setBottomBarForegroundColor"),

    // Dialog
    OpenDialogAlert("openDialogAlert"),
    OpenDialogPrompt("openDialogPrompt"),
    OpenDialogConfirm("openDialogConfirm"),
    OpenDialogWarning("openDialogWarning")
}

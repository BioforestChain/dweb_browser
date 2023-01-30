package info.bagen.libappmgr.ui.app

import android.annotation.SuppressLint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import info.bagen.libappmgr.R
import info.bagen.libappmgr.entity.AppInfo
import info.bagen.libappmgr.entity.DAppInfoUI
import info.bagen.libappmgr.ui.download.DownLoadState
import info.bagen.libappmgr.ui.download.DownloadAppMaskView
import info.bagen.libappmgr.ui.download.DownloadDialogView
import info.bagen.libappmgr.ui.view.DialogView

@Composable
private fun BoxScope.AppIcon(appViewState: AppViewState) {
    AsyncImage(
        model = appViewState.iconPath.value,
        contentDescription = null,
        imageLoader = ImageLoader.Builder(LocalContext.current).components {
            add(SvgDecoder.Factory()) // 为了支持 SVG 图片加载
        }.build(),
        modifier = Modifier
            .padding(3.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp)),
        //.clip(CircleShape),
        contentScale = ContentScale.FillWidth,
        // placeholder = BitmapPainter(image = ImageBitmap.imageResource(id = R.drawable.ic_launcher)),
        error = BitmapPainter(image = ImageBitmap.imageResource(id = R.drawable.ic_launcher))
    )
}

@Composable
private fun BoxScope.AppName(appViewState: AppViewState) {
    val name = when (appViewState.maskViewState.downLoadState.value) {
        DownLoadState.LOADING -> "下载中"
        DownLoadState.PAUSE -> "暂停"
        DownLoadState.INSTALL -> "正在安装"
        else -> appViewState.name.value
    }
    Text(
        text = name,
        maxLines = 2,
        color = MaterialTheme.colors.onSurface,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .align(Alignment.BottomCenter)
    )
}

/**
 * AppView 只存放App的信息，包括图标，字段
 */
@SuppressLint("UnrememberedMutableState")
@Composable
fun BoxScope.AppInfoItem(
    appViewModel: AppViewModel, appViewState: AppViewState, onOpenApp: (() -> Unit)?
) {
    Box(modifier = Modifier
        //.size(72.dp)
        .size(50.dp)
        .align(Alignment.TopCenter)
        .pointerInput(appViewState) {
            detectTapGestures(onPress = {}, // 触摸事件
                onTap = { // 点击事件
                    appViewState.appInfo?.dAppUrl?.let {
                        onOpenApp?.let { it() }
                    } ?: run {
                        appViewModel.handleIntent(AppViewIntent.LoadAppNewVersion(appViewState))
                    }
                }, onDoubleTap = {}, // 双击事件
                onLongPress = { // 长按事件，获取的坐标是相对于整个图标的
                    appViewModel.handleIntent(
                        AppViewIntent.PopAppMenu(
                            appViewState,
                            show = true
                        )
                    )
                })
        }) {
        AppIcon(appViewState)
        if (appViewState.showBadge.value) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .size(12.dp)
                    .background(Color.Red)
                    .align(Alignment.TopEnd)
            )
        }
        AppDropdownMenu(appViewModel = appViewModel, appViewState = appViewState)
    }
    AppName(appViewState)
}

@Composable
fun AppInfoView(appViewModel: AppViewModel, appViewState: AppViewState, onOpenApp: (() -> Unit)?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            //.height(108.dp)
            .height(86.dp)
    ) {
        AppInfoItem(appViewModel, appViewState, onOpenApp)
        if (appViewState.maskViewState.show.value) {
            DownloadAppMaskView(
                downLoadViewModel = appViewState.maskViewState.downLoadViewModel,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(6.dp))
                    //.clip(CircleShape)
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(6.dp),
                callbackState = { state, dInfo ->
                    appViewModel.handleIntent(
                        AppViewIntent.MaskDownloadCallback(
                            state,
                            dInfo,
                            appViewState
                        )
                    )
                },
                checkInstallOrOverride = { appInfo, zipFile ->
                    compareDownloadApp(appViewModel, appViewState, appInfo, zipFile)
                })
        }
    }
}

private fun compareDownloadApp(
    appViewModel: AppViewModel, appViewState: AppViewState, appInfo: AppInfo?, zipFile: String
): NewAppUnzipType {
    var ret = NewAppUnzipType.INSTALL
    if (appViewState.bfsDownloadPath != null && appInfo != null) {
        run OutSide@{
            appViewModel.uiState.appViewStateList.forEach { tempAppViewState ->
                if (tempAppViewState.appInfo?.bfsAppId == appInfo.bfsAppId) {
                    if (compareAppVersionHigh(
                            tempAppViewState.appInfo!!.version,
                            appInfo.version
                        )
                    ) {
                        ret = NewAppUnzipType.OVERRIDE
                        appViewModel.handleIntent(
                            AppViewIntent.OverrideDownloadApp(appViewState, appInfo, zipFile)
                        )
                    } else {
                        ret = NewAppUnzipType.LOW_VERSION
                        appViewModel.handleIntent(AppViewIntent.RemoveDownloadApp(appViewState))
                    }
                    return@OutSide
                }
            }
        }
        if (ret == NewAppUnzipType.INSTALL) {
            appViewModel.handleIntent(
                AppViewIntent.UpdateDownloadApp(
                    appViewState,
                    appInfo,
                    zipFile
                )
            )
        }
    }
    return ret
}

private fun compareAppVersionHigh(localVersion: String, compareVersion: String): Boolean {
    var localSplit = localVersion.split(".")
    val compareSplit = compareVersion.split(".")
    var tempLocalVersion = localVersion
    if (localSplit.size < compareSplit.size) {
        val cha = compareSplit.size - localSplit.size
        for (i in 0 until cha) {
            tempLocalVersion += ".0"
        }
        localSplit = tempLocalVersion.split(".")
    }
    try {
        for (i in compareSplit.indices) {
            val local = Integer.parseInt(localSplit[i])
            val compare = Integer.parseInt(compareSplit[i])
            if (compare > local) return true
        }
    } catch (e: Exception) {
        Log.e("AppViewModel", "compareAppVersionHigh issue -> $localVersion, $compareVersion")
    }
    return false
}

@Composable
fun AppInfoGridView(
    appViewModel: AppViewModel, onOpenApp: ((appId: String, url: DAppInfoUI?) -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),//GridCells.Adaptive(minSize = 60.dp), // 一行五个，或者指定大小
            contentPadding = PaddingValues(24.dp, 0.dp)
        ) {
            items(appViewModel.uiState.appViewStateList) { item ->
                AppInfoView(appViewModel, item) {
                    onOpenApp?.let { it(item.appInfo?.bfsAppId ?: "", item.dAppInfoUI) }
                }
            }
        }
    }
    AppDialogView(appViewModel = appViewModel, onOpenApp)
}

@Composable
fun AppDialogView(
    appViewModel: AppViewModel, onOpenApp: ((appId: String, url: DAppInfoUI?) -> Unit)? = null
) {
    when (appViewModel.uiState.appDialogInfo.value.lastType) {
        AppDialogType.DownLoading -> {
            val appViewState = appViewModel.uiState.curAppViewState
            DownloadDialogView(
                path = appViewModel.uiState.appDialogInfo.value.data as String
            ) { state, dialogInfo ->
                when (state) {
                    DownLoadState.COMPLETED, DownLoadState.FAILURE -> {
                        appViewModel.handleIntent(
                            AppViewIntent.DialogDownloadCallback(state, dialogInfo, appViewState!!)
                        )
                    }
                    else -> {}
                }
            }
        }
        AppDialogType.OpenDApp -> {
            appViewModel.uiState.curAppViewState?.let { appViewState ->
                appViewModel.handleIntent(AppViewIntent.ShowAppViewBadge(appViewState, false))
                onOpenApp?.let { open ->
                    open(appViewState.appInfo?.bfsAppId ?: "", appViewState.dAppInfoUI)
                }
            }
            appViewModel.handleIntent(AppViewIntent.DialogHide)
        }
        else -> {
            DialogView(appViewModel.uiState.appDialogInfo.value.dialogInfo,
                onConfirm = { appViewModel.handleIntent(AppViewIntent.DialogConfirm) },
                onCancel = { appViewModel.handleIntent(AppViewIntent.DialogHide) })
        }
    }
}

@Composable
fun AppDropdownMenu(appViewModel: AppViewModel, appViewState: AppViewState) {
    DropdownMenu(expanded = appViewState.showPopView.value, onDismissRequest = {
        appViewModel.handleIntent(AppViewIntent.PopAppMenu(appViewState, show = false))
    }) {
        Row {
            Column(modifier = Modifier
                .clickable {
                    appViewModel.handleIntent(
                        AppViewIntent.PopAppMenu(
                            appViewState,
                            show = false
                        )
                    )
                    appViewModel.handleIntent(AppViewIntent.ShareAppMenu(appViewState))
                }
                .padding(20.dp, 10.dp, 20.dp, 10.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    modifier = Modifier
                        .size(25.dp)
                        .align(Alignment.CenterHorizontally)
                )
                Text(
                    text = "分享",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            if (appViewState.dAppInfoUI != null) {
                Column(modifier = Modifier
                    .clickable {
                        appViewModel.handleIntent(
                            AppViewIntent.PopAppMenu(
                                appViewState,
                                show = false
                            )
                        )
                        appViewModel.handleIntent(AppViewIntent.UninstallApp(appViewState))
                    }
                    .padding(20.dp, 10.dp, 20.dp, 10.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier
                            .size(25.dp)
                            .align(Alignment.CenterHorizontally)
                    )
                    Text(
                        text = "卸载",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

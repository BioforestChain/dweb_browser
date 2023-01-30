package info.bagen.libappmgr.ui.dcim

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.bagen.libappmgr.data.PreferencesHelper
import info.bagen.libappmgr.entity.*
import kotlinx.coroutines.*

data class DCIMUIState(
    val showSpinner: MutableState<Boolean> = mutableStateOf(false), // 用于判断是否显示下拉
    val showViewer: MutableState<Boolean> = mutableStateOf(false), // 用于判断是否显示大图
    val showPreview: MutableState<Boolean> = mutableStateOf(false), // 用于判断显示大图时时是否是预览
    val showViewerBar: MutableState<Boolean> = mutableStateOf(true), // 用于判断大图界面是否显示工具栏和按钮

    val dcimInfoList: MutableList<DCIMInfo> = mutableStateListOf(), // 用于保存图片列表信息
    val dcimSpinnerList: MutableList<DCIMSpinner> = mutableStateListOf(), // 用于保存标签列表
    val checkedList: MutableList<DCIMInfo> = mutableStateListOf(), // 用于保存选中的图片信息

    var curDCIMInfo: MutableState<DCIMInfo> = mutableStateOf(DCIMInfo("")), // 用于保存当前选中的图片
    var curDcimSpinner: DCIMSpinner = DCIMSpinner(name = DCIMViewModel.All), // 表示加载的是都有的图片和视频

    val exoPlayerList: ArrayList<ExoPlayerData> = arrayListOf(),
)

sealed class DCIMIntent {
    class UpdateSpinnerState(val show: Boolean? = null) : DCIMIntent()
    class UpdateViewerState(val show: Boolean? = null) : DCIMIntent()
    class UpdatePreviewState(val show: Boolean? = null) : DCIMIntent()
    class UpdateViewerBarState(val show: Boolean? = null) : DCIMIntent()
    object InitDCIMInfoList : DCIMIntent()
    object HideViewer : DCIMIntent()
    class SetCurrentDCIMInfo(val dcimInfo: DCIMInfo? = null, val checkPreview: Boolean = false) :
        DCIMIntent()

    class SetCurrentDCIMSpinner(val dcimSpinner: DCIMSpinner? = null) : DCIMIntent()
    class RefreshDCIMInfoList(val dcimSpinner: DCIMSpinner) : DCIMIntent()
    class RefreshCheckList(val dcimInfo: DCIMInfo?) : DCIMIntent()
    class OverlayCheckList(val dcimInfo: DCIMInfo) : DCIMIntent()

    class AddExoPlayer(val exoPlayerData: ExoPlayerData) : DCIMIntent()
    class RemoveExoPlayer(val exoPlayerData: ExoPlayerData) : DCIMIntent()

    class SendCheckList(val send: Boolean = true) : DCIMIntent()
}

class DCIMViewModel(private val repository: DCIMRepository = DCIMRepository()) : ViewModel() {
    val uiState: MutableState<DCIMUIState> = mutableStateOf(DCIMUIState())

    fun handlerIntent(action: DCIMIntent) {
        viewModelScope.launch {
            when (action) {
                is DCIMIntent.UpdateSpinnerState -> {
                    uiState.value.showSpinner.value =
                        action.show ?: !uiState.value.showSpinner.value
                }
                is DCIMIntent.UpdateViewerState -> {
                    uiState.value.showViewer.value = action.show ?: !uiState.value.showViewer.value
                }
                is DCIMIntent.UpdatePreviewState -> {
                    uiState.value.showPreview.value =
                        action.show ?: !uiState.value.showPreview.value
                }
                is DCIMIntent.UpdateViewerBarState -> {
                    uiState.value.showViewerBar.value =
                        action.show ?: !uiState.value.showViewerBar.value
                }
                is DCIMIntent.InitDCIMInfoList -> {
                    // 1. 加载数据
                    repository.loadDCIMInfo { dcimMaps.clear(); dcimMaps.putAll(it) }
                    // 2. 初始化
                    initDCIMInfoList()
                }
                is DCIMIntent.RefreshDCIMInfoList -> refreshDCIMInfoList(action.dcimSpinner)
                is DCIMIntent.RefreshCheckList -> refreshCheckList(action.dcimInfo)
                is DCIMIntent.OverlayCheckList -> overlayCheckList(action.dcimInfo)
                is DCIMIntent.AddExoPlayer -> exoPlayerList.add(action.exoPlayerData)
                is DCIMIntent.RemoveExoPlayer -> removeExoPlayer(action.exoPlayerData)
                is DCIMIntent.HideViewer -> hideViewer()
                is DCIMIntent.SetCurrentDCIMInfo -> {
                    if (!action.checkPreview || uiState.value.showPreview.value) {
                        uiState.value.curDCIMInfo.value =
                            action.dcimInfo ?: uiState.value.checkedList[0]
                    } else {
                        // 打开大图时，有两个入口：点击图片 和 点击“预览”
                        // 如果当前是点击图片打开的，checklist中的图片未必全部都在当前的dcimInfoList中，所以这边做了限制，不跳转
                        action.dcimInfo?.let { actionDCIMInfo ->
                            uiState.value.dcimInfoList.forEach { dcimInfo ->
                                if (dcimInfo.path == actionDCIMInfo.path) {
                                    uiState.value.curDCIMInfo.value = actionDCIMInfo
                                }
                            }
                        }
                    }
                }
                is DCIMIntent.SetCurrentDCIMSpinner -> {
                    uiState.value.curDcimSpinner =
                        action.dcimSpinner ?: uiState.value.dcimSpinnerList[0]
                }
                is DCIMIntent.SendCheckList -> {
                    mCallBack?.let { callBack ->
                        if (action.send) {
                            val list = arrayListOf<String>()
                            uiState.value.checkedList.forEach { list.add(it.path) }
                            callBack.send(list)
                        } else {
                            callBack.cancel()
                        }
                    }
                }
            }
        }
    }

    /**
     * 初始化数据
     */
    private suspend fun initDCIMInfoList() {
        val dcimInfoList: ArrayList<DCIMInfo> = arrayListOf()
        val dcimSpinnerList: ArrayList<DCIMSpinner> = arrayListOf()

        var total = 0
        dcimSpinnerList.add(DCIMSpinner(AllVideo, 0))
        dcimMaps.iterator().forEach { list ->
            dcimInfoList.addAll(list.value)
            dcimSpinnerList.add(DCIMSpinner(list.key, list.value.size, list.value[0].path))
            total += list.value.size
        }
        // 进行排序操作
        dcimInfoList.sortWith { o1, o2 -> o2.time.compareTo(o1.time) } // Collections.sort变种
        dcimSpinnerList.add(0, DCIMSpinner(All, total, dcimInfoList[0].path))
        // 更新列表
        uiState.value = uiState.value.copy(
            dcimInfoList = dcimInfoList, dcimSpinnerList = dcimSpinnerList
        )

        withContext(Dispatchers.IO) {
            var videos = 0
            var firstVideo: DCIMInfo? = null
            var firstDCIMInfo: DCIMInfo? = null
            dcimInfoList.forEach { dcimInfo ->
                if (firstDCIMInfo == null) firstDCIMInfo = dcimInfo
                if (dcimInfo.type == DCIMType.VIDEO) {
                    videos++
                    if (videos == 1) {
                        firstVideo = dcimInfo
                    }
                }
            }
            dcimSpinnerList[1].count = videos
            firstVideo?.let {
                it.updateDuration()
                dcimSpinnerList[1].path = it.bitmap ?: ""
            }
            firstDCIMInfo?.let {
                dcimSpinnerList[0].path = it.bitmap ?: ""
            }

            uiState.value = uiState.value.copy(
                dcimSpinnerList = dcimSpinnerList
            )
        }

        if (!PreferencesHelper.isMediaLoading()) {
            // 开始刷新时间
            val list = arrayListOf<DCIMInfo>()
            list.addAll(dcimInfoList)
            list.forEach {
                if (it.type == DCIMType.VIDEO && it.bitmap == null) {
                    val job = GlobalScope.launch(Dispatchers.Default) {
                        it.updateDuration()
                    }
                    jobList.add(job)
                }
            }
        }
    }

    /**
     * Spinner选择不同的类型的，列表进行重新加载
     */
    private fun refreshDCIMInfoList(dcimSpinner: DCIMSpinner) {
        val dcimInfoList: ArrayList<DCIMInfo> = arrayListOf()
        uiState.value.curDcimSpinner = dcimSpinner
        uiState.value.showSpinner.value = false // 隐藏Spinner

        when (dcimSpinner.name) {
            All -> {
                // 先加载都有，然后按照时间排序，然后开始加载视频信息
                // 添加三个顶部特殊标签
                dcimMaps.iterator().forEach { list ->
                    dcimInfoList.addAll(list.value)
                }
                // 进行排序操作
                dcimInfoList.sortWith { o1, o2 -> o2.time.compareTo(o1.time) } // Collections.sort变种
            }
            AllPhoto -> {
                dcimMaps.iterator().forEach { list ->
                    list.value.forEach {
                        if (it.type != DCIMType.VIDEO) {
                            dcimInfoList.add(it)
                        }
                    }
                }
                // 进行排序操作
                dcimInfoList.sortWith { o1, o2 -> o2.time.compareTo(o1.time) } // Collections.sort变种
            }
            AllVideo -> {
                dcimMaps.iterator().forEach { list ->
                    list.value.forEach {
                        if (it.type == DCIMType.VIDEO) {
                            dcimInfoList.add(it)
                        }
                    }
                }
                // 进行排序操作
                dcimInfoList.sortWith { o1, o2 -> o2.time.compareTo(o1.time) } // Collections.sort变种
            }
            else -> {
                dcimMaps.iterator().forEach {
                    if (it.key == dcimSpinner.name) {
                        dcimInfoList.addAll(it.value)
                        // 进行排序操作
                        dcimInfoList.sortWith { o1, o2 -> o2.time.compareTo(o1.time) } // Collections.sort变种
                    }
                }
            }
        }
        uiState.value = uiState.value.copy(
            dcimInfoList = dcimInfoList
        )
    }

    /**
     * 修改选中列表的数据，由于需要刷新列表和状态，所以统一这边处理
     */
    private fun refreshCheckList(dcimInfo: DCIMInfo? = null) {
        dcimInfo?.let {
            if (it.checked.value) {
                uiState.value.checkedList.remove(it)
            } else {
                if (uiState.value.checkedList.size == 99) return // 如果已经是99个了。那么就不再新增
                uiState.value.checkedList.add(it)
            }
            it.checked.value = !it.checked.value
        }
        var index = 1
        uiState.value.checkedList.forEach {
            it.index.value = index
            index++
        }
    }

    private fun overlayCheckList(dcimInfo: DCIMInfo) {
        uiState.value.curDCIMInfo.value.checked.value =
            !uiState.value.curDCIMInfo.value.checked.value
        uiState.value.curDCIMInfo.value.overlay.value =
            !uiState.value.curDCIMInfo.value.overlay.value
    }

    private suspend fun removeExoPlayer(exoPlayerData: ExoPlayerData) {
        exoPlayerList.forEach {  // 为了把其他在播放的进行暂停
            it.exoPlayer.pause()
            it.exoPlayer.seekTo(0)
            it.playerState.value = PlayerState.Play
        }
        exoPlayerData.exoPlayer.release()
        exoPlayerList.remove(exoPlayerData)
    }

    private suspend fun hideViewer() {
        uiState.value.showViewerBar.value = true // 退出预览界面后，需要重新显示工具栏
        if (uiState.value.showPreview.value) { // 当前处理逻辑表示预览界面显示的是预览内容，不是Spinner对应的列表
            uiState.value.showPreview.value = false
            // 为了将之前预览状态下移除的列表删掉
            val tempList = arrayListOf<DCIMInfo>()
            uiState.value.checkedList.forEach {
                if (it.overlay.value) {
                    it.overlay.value = false
                    it.checked.value = false
                    tempList.add(it)
                }
            }
            uiState.value.checkedList.removeAll(tempList)
            refreshCheckList()
        }
    }

    private val dcimMaps: HashMap<String, ArrayList<DCIMInfo>> = hashMapOf()
    private val exoPlayerList = arrayListOf<ExoPlayerData>()
    private var jobList: ArrayList<Job> = arrayListOf() // 用于保存当前的GlobalScope

    companion object {
        const val All = "所有图片和视频"
        const val AllPhoto = "所有照片"
        const val AllVideo = "所有视频"
    }

    /**
     * 用于接收回调
     * send：有选择图片或者视频，返回具体路径
     * cancel：取消
     */
    interface CallBack {
        fun send(fileList: ArrayList<String>)
        fun cancel()
    }

    private var mCallBack: CallBack? = null
    fun setCallback(callback: CallBack) {
        mCallBack = callback
    }

    fun clearJobList() {
        jobList.forEach { it.cancel() } // 停止后台继续执行的协程
        dcimMaps.clear()
    }
}

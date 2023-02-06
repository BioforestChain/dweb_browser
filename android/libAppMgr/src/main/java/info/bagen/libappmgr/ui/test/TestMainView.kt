package info.bagen.libappmgr.ui.test

import android.annotation.SuppressLint
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.net.Uri
import android.util.ArrayMap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import info.bagen.libappmgr.R
import info.bagen.libappmgr.ui.view.TestWebView
import info.bagen.libappmgr.utils.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import java.util.*

data class TestAppInfo(
    val name: String = "",
    val icon: String = "",
    val path: String = "",
    var date: String = "",
    var checked: Boolean = false
)

data class TestBaseResult(
    val code: Int,
    val msg: String,
    val newslist: List<TestHotLink>,
)

data class TestHotLink(
    val id: String,
    val ctime: String,
    val title: String,
    val description: String,
    val picUrl: String,
    val url: String,
    val source: String,
)

@Composable
fun TestAppView(appInfo: TestAppInfo, onClick: ((TestAppInfo) -> Unit)? = null) {
    Column {
        if (appInfo.icon.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(60.dp)
            ) {
                AsyncImage(
                    model = "",
                    contentDescription = "",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray)
                        .clickable { onClick?.let { it(appInfo) } }
                )
                Text(
                    text = appInfo.name.substring(0, 1),
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 30.sp,
                    color = Color.White
                )
            }
        } else {
            AsyncImage(
                model = appInfo.icon,
                placeholder = painterResource(id = R.drawable.ic_launcher),
                contentDescription = "",
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onClick?.let { it(appInfo) } }
            )
        }
        Text(
            text = appInfo.name, modifier = Modifier
                .width(60.dp)
                .align(Alignment.CenterHorizontally),
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TestListAppView(
    topName: String,
    listAppInfo: ArrayList<TestAppInfo>,
    onClick: ((TestAppInfo) -> Unit)? = null
) {
    Column {
        Text(
            text = topName, fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(30.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            modifier = Modifier.height(180.dp)
        ) {
            items(listAppInfo) {
                TestAppView(appInfo = it, onClick = onClick)
            }
        }
    }
}

@Composable
fun TestListLinks(
    topName: String,
    listNews: MutableList<TestAppInfo>,
    onClick: ((TestAppInfo) -> Unit)? = null
) {
    Column {
        Text(
            text = topName, fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        )
        var index = 1
        listNews.forEach { hotLink ->
            Row(
                modifier = Modifier
                    .padding(12.dp, 3.dp, 12.dp, 3.dp)
                    .clickable { onClick?.let { it(hotLink) } }) {
                Text(
                    text = "$index", color = when (index) {
                        1 -> Color.Red
                        2, 3 -> Color.Cyan
                        else -> Color.Gray
                    }, fontSize = 18.sp, modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .width(30.dp)
                )
                Text(
                    text = "${hotLink.name}",
                    fontSize = 16.sp,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            index++
        }
    }
}


@Composable
fun TestListFavorite(
    topName: String,
    listAppInfo: MutableList<TestAppInfo>,
    onClick: ((TestAppInfo) -> Unit)? = null
) {
    if (listAppInfo.size > 0) {
        Column {
            Text(
                text = topName, fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(listAppInfo) {
                    TestAppView(appInfo = it, onClick = onClick)
                }
            }
        }
    }
}

@Composable
fun TestBottomBar(viewModel: TestViewModel, onClick: ((NavController) -> Unit)?) {

    var index = remember { mutableStateOf(0) }

    BottomNavigation(
        backgroundColor = Color.White,
        elevation = 3.dp
    ) {
        for (i in 0 until viewModel.bottomNavController.size) {
            BottomNavigationItem(
                selected = i == index.value,
                enabled = viewModel.bottomNavController[i].clickable.value,
                onClick = {
                    onClick?.let { it(viewModel.bottomNavController[i]) }
                },
                icon = {
                    Icon(
                        modifier = Modifier.size(30.dp),
                        painter = painterResource(id = viewModel.bottomNavController[i].iconRes),
                        contentDescription = "",
                        tint = if (viewModel.bottomNavController[i].clickable.value) Color.Black else Color.Gray
                    )
                })
        }
    }
}

@SuppressLint(
    "CoroutineCreationDuringComposition", "UnrememberedMutableState",
    "UnusedMaterialScaffoldPaddingParameter"
)

@Preview
@Composable
fun TestMainView(
    viewModel: TestViewModel = TestViewModel(),
    onClick: ((TestAppInfo) -> Unit)? = null
) {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    GlobalScope.launch {
        // 加载全网热搜
        var doc =
            Jsoup.connect("https://top.baidu.com/board?tab=realtime").ignoreHttpErrors(true).get()
        var elementContent = doc.getElementsByClass("content_1YWBm")
        var count = 1
        elementContent?.forEach { element ->
            if (count > 16) return@forEach
            var title = element.getElementsByClass("c-single-text-ellipsis").text()
            var path = element.select("a").first()?.attr("href")
            viewModel.hotSearchList.add(TestAppInfo(title, path = path ?: ""))
            count++
        }

        // 加载书签
        var fl = AppContextUtil.sInstance!!.getString(key = "favoriteList", "")
        if (fl.isNotEmpty()) {
            var favList: List<TestAppInfo> =
                Gson().fromJson(fl, object : TypeToken<List<TestAppInfo>>() {}.type)
            favList.forEach { item ->
                viewModel.favoriteList.add(item)
            }
        }

        // 加载历史
        var hl = AppContextUtil.sInstance!!.getString(key = "historyList", "")
        if (hl.isNotEmpty()) {
            var historyList: List<TestAppInfo> =
                Gson().fromJson(hl, object : TypeToken<List<TestAppInfo>>() {}.type)
            if (historyList.isNotEmpty()) viewModel.historyList.addAll(historyList)
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .clickable { onClick?.let { it(TestAppInfo()) } }
            .fillMaxSize()
            .background(MaterialTheme.colors.primary),
        bottomBar = {
            Column {
                if (viewModel.showFavPop.value) {
                    TestFavoritePop(viewModel)
                }
                if (viewModel.progress.value in 0 until 100) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        progress = viewModel.progress.value / 100f
                    )
                }
                TestBottomBar(viewModel) {
                    when (it.type) {
                        NavControllerType.Back -> {
                            when (viewModel.navUIType.value) {
                                NavUIType.HOME -> {}
                                NavUIType.LIST -> {
                                    viewModel.navUIType.value = NavUIType.HOME
                                }
                                NavUIType.WEBVIEW -> {
                                    viewModel.navUIType.value = NavUIType.HOME
                                }
                            }
                        }
                        NavControllerType.Next -> {}
                        NavControllerType.Book -> {
                            viewModel.curAppInfo?.let {
                                // 存储到书签
                                viewModel.favoriteList.add(viewModel.curAppInfo!!)
                                viewModel.curAppInfo!!.date =
                                    ConstUtil.simpleDateFormatDayWeek.format(Date())
                                AppContextUtil.sInstance!!.saveString(
                                    key = "favoriteList",
                                    JsonUtil.toJson(viewModel.favoriteList)
                                )
                                scope.launch {
                                    scaffoldState.snackbarHostState.showSnackbar("添加书签成功")
                                }
                            }
                        }
                        NavControllerType.Favorite -> {
                            viewModel.showFavPop.value = !viewModel.showFavPop.value
                        }
                        NavControllerType.Home -> {
                            viewModel.showWebView.value = false
                            viewModel.showFavPop.value = false
                            viewModel.showFavOrHistory.value = 0
                            viewModel.navUIType.value = NavUIType.HOME
                            viewModel.bottomNavController.forEach { navType ->
                                navType.clickable.value = navType.type == NavControllerType.Favorite
                            }
                        }
                    }
                }
            }
        },
        topBar = {
            if (viewModel.navUIType.value == NavUIType.HOME) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    TestSearchView { path ->
                        var uri = Uri.parse(path)
                        viewModel.curAppInfo =
                            TestAppInfo(name = "${uri.getQueryParameter("q")} - 搜索", path = path)
                        onClick?.let { it(viewModel.curAppInfo!!) }
                    }
                }
            }
        },
        content = {
            Box(modifier = Modifier.clickable(enabled = viewModel.navUIType.value == NavUIType.HOME) {
                onClick?.let { it(TestAppInfo()) }
            }) {
                when (viewModel.navUIType.value) {
                    NavUIType.HOME -> {
                        viewModel.deleteList.clear()
                        LazyColumn(modifier = Modifier.padding(6.dp)) {
                            item {
                                TestListAppView(
                                    topName = "热门网站",
                                    listAppInfo = viewModel.hotWebList
                                ) { appInfo ->
                                    viewModel.curAppInfo = appInfo
                                    onClick?.let { click -> click(viewModel.curAppInfo!!) }
                                }
                            }
                            item {
                                TestListFavorite(
                                    topName = "我的书签",
                                    listAppInfo = viewModel.favoriteList,
                                    onClick
                                )
                            }
                            item {
                                TestListLinks(
                                    topName = "全网热搜",
                                    listNews = viewModel.hotSearchList
                                ) { appInfo ->
                                    viewModel.curAppInfo = appInfo
                                    onClick?.let { click -> click(viewModel.curAppInfo!!) }
                                }
                            }
                            item {
                                Spacer(modifier = Modifier.height(50.dp))
                            }
                        }
                    }
                    NavUIType.WEBVIEW -> {
                        TestWebView(
                            viewModel = viewModel,
                            onBack = {
                                if (it != null && it!!.canGoBack()) {
                                    it!!.goBack()
                                }
                            },
                            onReceivedError = {},
                            onProgressChange = { viewModel.progress.value = it }
                        )
                    }
                    NavUIType.LIST -> {
                        TestPopHistoryOrFavorite(viewModel)
                    }
                    else -> {
                        Text(
                            text = "正在加载数据，请稍后...", modifier = Modifier
                                .fillMaxSize()
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        },
        snackbarHost = {
            SnackbarHost(it) { data ->
                Snackbar(
                    snackbarData = data,
                    backgroundColor = Color.Blue,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    )
}

@Composable
fun BoxScope.TestSearchView(onClick: ((String) -> Unit)? = null) {
    var search: MutableState<String> = remember { mutableStateOf("") }
    TextField(
        value = search.value,
        onValueChange = { search.value = it },
        placeholder = { Text("搜索或输入网址") },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .clip(RoundedCornerShape(16.dp)),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = {
            var url = "https://cn.bing.com/search?q=${search.value}"
            // var url = "https://www.baidu.com/s?wd=${search.value}"
            onClick?.let { it(url) }
        })
    )
}

@Composable
fun TestFavoritePop(viewModel: TestViewModel) {
    var index = remember { mutableStateOf(0) }
    BottomNavigation(
        backgroundColor = Color.White,
        elevation = 3.dp
    ) {
        for (i in 0 until viewModel.favoritePopController.size) {
            var fpc = viewModel.favoritePopController[i]
            BottomNavigationItem(
                selected = i == index.value,
                enabled = fpc.clickable.value,
                onClick = {
                    when (fpc.type) {
                        NavControllerType.Book -> {
                            viewModel.showFavOrHistory.value =
                                if (viewModel.showFavOrHistory.value == 1) 0 else 1
                            viewModel.showListType.value = "favorite"
                            viewModel.navUIType.value =
                                if (viewModel.showFavOrHistory.value == 0) NavUIType.HOME else NavUIType.LIST
                        }
                        NavControllerType.History -> {
                            viewModel.showFavOrHistory.value =
                                if (viewModel.showFavOrHistory.value == 2) 0 else 2
                            viewModel.showListType.value = "history"
                            viewModel.navUIType.value =
                                if (viewModel.showFavOrHistory.value == 0) NavUIType.HOME else NavUIType.LIST
                        }
                        NavControllerType.Share -> {
                            val sendIntent: Intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    viewModel.curAppInfo?.path ?: "not found url"
                                )
                                type = "text/plain"
                                flags = FLAG_ACTIVITY_NEW_TASK
                            }
                            AppContextUtil.sInstance!!.startActivity(sendIntent)
                        }
                    }
                },
                icon = {
                    Column(modifier = Modifier.size(40.dp)) {
                        Icon(
                            modifier = Modifier
                                .size(25.dp)
                                .align(Alignment.CenterHorizontally),
                            painter = painterResource(id = fpc.iconRes),
                            contentDescription = "",
                            tint = Color.Black
                        )
                        if (fpc.name != null) {
                            Text(
                                text = fpc.name!!,
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                fontSize = 12.sp
                            )
                        }
                    }
                })
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TestPopHistoryOrFavorite(viewModel: TestViewModel) {
    if (viewModel.showFavOrHistory.value > 0) {
        viewModel.bottomNavController.forEach {
            if (it.type == NavControllerType.Home) it.clickable.value = true
        }
        viewModel.deleteList.clear()
        var maps = ArrayMap<String, List<TestAppInfo>>()
        var curKey = ""
        var list = arrayListOf<TestAppInfo>()
        var title = ""
        when (viewModel.showListType.value) {
            "favorite" -> {
                title = "书签列表"
                for (i in viewModel.favoriteList.size downTo 1) {
                    var item = viewModel.favoriteList[i - 1]
                    item.checked = false
                    if (curKey == item.date) {
                        list.add(item)
                    } else {
                        if (list.size > 0) {
                            maps[curKey] = list
                            list = arrayListOf()
                        }
                        curKey = item.date
                        list.add(item)
                    }
                }
                if (list.size > 0) maps[curKey] = list
            }
            "history" -> {
                title = "历史记录"
                for (i in viewModel.historyList.size downTo 1) {
                    var item = viewModel.historyList[i - 1]
                    item.checked = false
                    if (curKey == item.date) {
                        list.add(item)
                    } else {
                        if (list.size > 0) {
                            maps[curKey] = list
                            list = arrayListOf()
                        }
                        curKey = item.date
                        list.add(item)
                    }
                }
                if (list.size > 0) maps[curKey] = list
            }
            else -> null
        }

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(8.dp)
                )

                Image(
                    bitmap = ImageBitmap.imageResource(id = R.drawable.ic_delete),
                    contentDescription = "",
                    modifier = Modifier
                        .padding(8.dp)
                        .size(30.dp)
                        .align(Alignment.CenterEnd)
                        .clickable {
                            if (viewModel.deleteList.size > 0) {
                                when (viewModel.showListType.value) {
                                    "favorite" -> {
                                        viewModel.favoriteList.removeAll(viewModel.deleteList)
                                        AppContextUtil.sInstance!!.saveString(
                                            key = "favoriteList",
                                            JsonUtil.toJson(viewModel.favoriteList)
                                        )
                                    }
                                    "history" -> {
                                        viewModel.historyList.removeAll(viewModel.deleteList)
                                        AppContextUtil.sInstance!!.saveString(
                                            key = "historyList",
                                            JsonUtil.toJson(viewModel.historyList)
                                        )
                                    }
                                }
                            }
                        }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                maps.forEach { (key, value) ->
                    stickyHeader {
                        Text(
                            text = key, modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Gray.copy(0.5f))
                                .padding(6.dp), fontSize = 16.sp, color = Color.DarkGray
                        )
                    }
                    items(value) { item ->
                        TestListItem(viewModel, appInfo = item)
                    }
                }
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun TestListItem(viewModel: TestViewModel, appInfo: TestAppInfo) {
    var check = mutableStateOf(appInfo.checked)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .width(50.dp)
    ) {
        Checkbox(
            checked = check.value,
            onCheckedChange = {
                appInfo.checked = it
                check.value = it
                if (it) {
                    viewModel.deleteList.add(appInfo)
                } else {
                    viewModel.deleteList.remove(appInfo)
                }
            },
            modifier = Modifier
                .padding(6.dp)
                .align(Alignment.CenterVertically)
        )

        if (appInfo.icon.isEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(6.dp)
                    .size(44.dp)
            ) {
                AsyncImage(
                    model = "",
                    contentDescription = "",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Gray)
                )
                Text(
                    text = appInfo.name.substring(0, 1),
                    modifier = Modifier.align(Alignment.Center),
                    fontSize = 30.sp,
                    color = Color.White
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(6.dp)
                    .size(44.dp)
            ) {
                AsyncImage(
                    model = appInfo.icon,
                    placeholder = painterResource(id = R.drawable.ic_launcher),
                    contentDescription = "",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterVertically)
        ) {
            Text(text = appInfo.name, maxLines = 1, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(text = appInfo.path, maxLines = 1, fontSize = 12.sp)
        }
    }
}

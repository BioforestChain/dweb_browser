package info.bagen.dwebbrowser.microService.sys.jmm.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.microService.sys.jmm.JmmMetadata
import java.text.DecimalFormat

@Composable
fun MALLBrowserView(jmmViewModel: JmmManagerViewModel) {
  val jmmMetadata =jmmViewModel.uiState.downloadInfo.value.jmmMetadata
  Box(
    modifier = Modifier.fillMaxSize()
  ) {
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(start = 16.dp, end = 16.dp)
    ) {
      item { Spacer(modifier = Modifier.height(16.dp)) }
      item { HeadContent(jmmMetadata = jmmMetadata) }
      item { Spacer(modifier = Modifier.height(16.dp)) }
      item { CaptureListView(jmmMetadata = jmmMetadata) }
      item { Spacer(modifier = Modifier.height(16.dp)) }
      item { AppIntroductionView(jmmMetadata = jmmMetadata) }
      jmmMetadata.permissions?.let { permissions ->
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item { Text(text = "权限列表", fontSize = 24.sp, fontStyle = FontStyle.Normal) }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        itemsIndexed(permissions) { index, mmid ->
          InstallItemPermissionView(index, mmid, permissions.size)
        }
      }
      item { Spacer(modifier = Modifier.height(60.dp)) }
    }

    DownLoadButton(jmmViewModel)
  }
}

@Composable
fun InstallBrowserView(jmmViewModel: JmmManagerViewModel) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.LightGray)
      .padding(start = 16.dp, end = 16.dp)
  ) {
    LazyColumn {
      item { Spacer(modifier = Modifier.height(16.dp)) }
      item { InstallItemDeleteView() }
      item { Spacer(modifier = Modifier.height(16.dp)) }
      item { Text(text = "要安装此应用吗？", fontSize = 16.sp, color = Color.Gray) }
      item { Spacer(modifier = Modifier.height(16.dp)) }
      item { Text(text = "权限", fontSize = 16.sp, color = Color.Gray) }
      item { Spacer(modifier = Modifier.height(16.dp)) }
      jmmViewModel.uiState.downloadInfo.value.jmmMetadata.permissions?.let { permissions ->
        itemsIndexed(permissions) { index, mmid ->
          InstallItemPermissionView(index, mmid, permissions.size)
        }
      }
    }

    Column(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
    ) {
      Button(
        onClick = { /*TODO*/ },
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .width(300.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = Color.Gray, contentColor = Color.Black
        ),
        shape = RoundedCornerShape(32.dp)
      ) {
        Text(text = "取消", fontSize = 16.sp)
      }
      Button(
        onClick = { /*TODO*/ },
        modifier = Modifier
          .align(Alignment.CenterHorizontally)
          .width(300.dp),
        colors = ButtonDefaults.buttonColors(
          backgroundColor = Color.Blue, contentColor = Color.White
        ),
        shape = RoundedCornerShape(32.dp)
      ) {
        Text(text = "安装", fontSize = 16.sp)
      }
    }
  }
}

@Composable
fun UninstallBrowserView(jmmViewModel: JmmManagerViewModel) {
  Box(modifier = Modifier.fillMaxSize()) {
    Text(text = "当前是卸载界面", modifier = Modifier.align(Alignment.Center))
  }
}

@Composable
private fun BoxScope.DownLoadButton(jmmViewModel: JmmManagerViewModel) {
  val downLoadInfo = jmmViewModel.uiState.downloadInfo.value
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(60.dp)
      .align(Alignment.BottomCenter)
      .background(Color.White.copy(0.8f))
  ) {
    var showLinearProgress = false
    val text = when (downLoadInfo.downLoadStatus) {
      DownLoadStatus.IDLE, DownLoadStatus.CANCEL -> {
        "下载 (${downLoadInfo.jmmMetadata.size.toSpaceSize()})"
      }
      DownLoadStatus.NewVersion -> {
        "更新 (${downLoadInfo.jmmMetadata.size.toSpaceSize()})"
      }
      DownLoadStatus.DownLoading -> {
        showLinearProgress = true
        "下载中".displayDownLoad(downLoadInfo.size, downLoadInfo.dSize)
      }
      DownLoadStatus.PAUSE -> {
        showLinearProgress = true
        "暂停".displayDownLoad(downLoadInfo.size, downLoadInfo.dSize)
      }
      DownLoadStatus.DownLoadComplete -> "安装中..."
      DownLoadStatus.INSTALLED -> "打开"
      DownLoadStatus.FAIL -> "重新下载"
    }
    val boxModifier = Modifier
      .size(300.dp, 50.dp)
      .align(Alignment.Center)
      .clip(RoundedCornerShape(32.dp))
      .clickable { jmmViewModel.handlerIntent(JmmIntent.ButtonFunction) }
      .background(if (showLinearProgress) Color.Gray else Color.Blue)


    Box(modifier = boxModifier) {
      if (showLinearProgress) {
        LinearProgressIndicator(
          progress = 1.0f * downLoadInfo.dSize / downLoadInfo.size,
          modifier = Modifier.fillMaxSize(),
          backgroundColor = Color.Gray,
          color = Color.Blue
        )
      }
      Text(
        text = text,
        fontSize = 20.sp,
        color = Color.White,
        modifier = Modifier.align(Alignment.Center)
      )
    }
  }
}

private fun String.displayDownLoad(total: Long, progress: Long): String {
  val GB = 1024 * 1024 * 1024 // 定义GB的计算常量
  val MB = 1024 * 1024 // 定义MB的计算常量
  val KB = 1024 // 定义KB的计算常量
  val df = DecimalFormat("0.0");//格式化小数
  var dValue: String
  val totalValue = if (total / GB >= 1) {
    dValue = df.format(1.0 * progress / GB)
    df.format(total / GB) + " GB";
  } else if (total / MB >= 1) {
    dValue = df.format(1.0 * progress / MB)
    df.format(total / MB) + " MB";
  } else if (total / KB >= 1) { //如果当前Byte的值大于等于1KB
    dValue = df.format(1.0 * progress / KB)
    df.format(total / KB) + " KB";
  } else {
    dValue = "$progress"
    "$total B";
  }
  return if (dValue.isEmpty()) "$this ($totalValue)" else "$this ($dValue/$totalValue)"
}

private fun String.toSpaceSize(): String {
  if (this.isEmpty()) return "0"
  val size = this.toFloat()
  val GB = 1024 * 1024 * 1024 // 定义GB的计算常量
  val MB = 1024 * 1024 // 定义MB的计算常量
  val KB = 1024 // 定义KB的计算常量
  val df = DecimalFormat("0.0");//格式化小数
  return if (size / GB >= 1) {
    df.format(size / GB) + " GB";
  } else if (size / MB >= 1) {
    df.format(size / MB) + " MB";
  } else if (size / KB >= 1) { //如果当前Byte的值大于等于1KB
    df.format(size / KB) + " KB";
  } else {
    "$size B";
  }
}

@Composable
private fun HeadContent(jmmMetadata: JmmMetadata) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .height(80.dp)
  ) {
    AsyncImage(
      model = jmmMetadata.icon,
      contentDescription = null,
      modifier = Modifier
        .size(80.dp)
        .clip(RoundedCornerShape(6.dp))
    )

    Spacer(modifier = Modifier.width(16.dp))

    Box(
      modifier = Modifier.fillMaxSize()
    ) {
      Column(modifier = Modifier.align(Alignment.TopStart)) {
        Text(text = jmmMetadata.title, fontSize = 24.sp)
        Text(text = jmmMetadata.subtitle, fontSize = 12.sp, color = Color.Gray)
      }

      Column(modifier = Modifier.align(Alignment.BottomStart)) {

        Row {
          Text(text = "版本：${jmmMetadata.version}", fontSize = 12.sp, color = Color.Gray)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "大小：${jmmMetadata.size.toSpaceSize()}", fontSize = 12.sp, color = Color.Gray)
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "作者：${jmmMetadata.author}", fontSize = 12.sp, color = Color.Gray)
        }
      }
    }

    Spacer(modifier = Modifier.width(16.dp))
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CaptureListView(jmmMetadata: JmmMetadata) {
  jmmMetadata.images?.let { images ->
    LazyRow {
      items(images) {
        Card(
          onClick = { /*TODO*/ }, modifier = Modifier
            .padding(end = 16.dp)
            .size(135.dp, 240.dp)
        ) {
          AsyncImage(model = it, contentDescription = null)
        }
      }
    }
  }
}

@Composable
private fun AppIntroductionView(jmmMetadata: JmmMetadata) {
  val expanded = remember { mutableStateOf(false) }
  Column {
    Text(text = "应用介绍", fontSize = 24.sp, fontStyle = FontStyle.Normal)

    Box(modifier = Modifier
      .animateContentSize()
      .clickable { expanded.value = !expanded.value }) {
      Text(
        text = jmmMetadata.introduction,
        maxLines = if (expanded.value) Int.MAX_VALUE else 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
fun InstallItemDeleteView() {
  val switchChecked = remember { mutableStateOf(true) }
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(Color.White)
      .padding(16.dp)
  ) {
    Column(modifier = Modifier.align(Alignment.CenterStart)) {
      Text(text = "删除安装程序包", fontSize = 24.sp)
      Text(text = "应用程序安装后立即从设备中删除安装包", fontSize = 12.sp, color = Color.Gray)
    }
    Switch(modifier = Modifier.align(Alignment.BottomEnd),
      checked = switchChecked.value,
      colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White, // 圆圈的颜色
        checkedTrackColor = Color.Blue, // 打开的进度的颜色
        checkedTrackAlpha = 1.0f,
        uncheckedThumbColor = Color.White, // 圆圈的颜色
        uncheckedTrackColor = Color.Gray, // 关闭的进度的颜色
      ),
      onCheckedChange = { checked -> switchChecked.value = checked })
  }
}

@Composable
fun InstallItemPermissionView(index: Int, mmid: String, size: Int) {
  Box(
    modifier = Modifier
      .clip(
        RoundedCornerShape(
          topStart = if (index == 0) 16.dp else 0.dp,
          topEnd = if (index == 0) 16.dp else 0.dp,
          bottomStart = if (index == size - 1) 16.dp else 0.dp,
          bottomEnd = if (index == size - 1) 16.dp else 0.dp
        )
      )
      .fillMaxWidth()
      .background(Color.White)
      .padding(16.dp)
  ) {
    Row {
      AsyncImage(
        model = R.mipmap.ic_launcher_round,
        contentDescription = null,
        modifier = Modifier
          .align(Alignment.CenterVertically)
          .size(20.dp)
      )
      Spacer(modifier = Modifier.width(16.dp))
      Text(
        text = mmid, modifier = Modifier.align(
          Alignment.CenterVertically
        )
      )
    }
  }
}
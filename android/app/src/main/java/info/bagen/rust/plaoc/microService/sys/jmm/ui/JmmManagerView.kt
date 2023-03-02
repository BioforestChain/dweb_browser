package info.bagen.rust.plaoc.microService.sys.jmm.ui

import android.util.Log
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import info.bagen.rust.plaoc.R
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import java.text.DecimalFormat

@Composable
fun MALLBrowserView(
  jmmViewModel: JmmManagerViewModel, onDownLoad: (String, String) -> Unit
) {
  jmmViewModel.uiState.jmmMetadata?.let { jmmMetadata ->
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
        item { Spacer(modifier = Modifier.height(60.dp)) }
      }

      DownLoadButton(jmmViewModel, onDownLoad)
    }
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
      jmmViewModel.uiState.jmmMetadata?.permissions?.let { permissions ->
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
          backgroundColor = Color.Gray,
          contentColor = Color.Black
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
          backgroundColor = Color.Blue,
          contentColor = Color.White
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
private fun BoxScope.DownLoadButton(
  jmmViewModel: JmmManagerViewModel, onDownLoad: (String, String) -> Unit
) {
  Log.e("lin.huang", "JmmManagerView::DownLoadButton $jmmViewModel")
  val downLoadInfo = jmmViewModel.uiState.downloadInfo.value
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .height(60.dp)
      .align(Alignment.BottomCenter)
      .background(Color.White.copy(0.8f))
  ) {
    Button(
      onClick = {
        onDownLoad(
          jmmViewModel.uiState.jmmMetadata!!.main_url, jmmViewModel.uiState.jmmMetadata!!.title
        )
      },
      modifier = Modifier
        .padding(8.dp)
        .width(300.dp)
        .align(Alignment.Center),
      colors = ButtonDefaults.buttonColors(
        backgroundColor = Color.Blue,
        contentColor = Color.White
      ),
      shape = RoundedCornerShape(32.dp)
    ) {
      val text = when (downLoadInfo.downLoadStatus) {
        DownLoadStatus.IDLE -> "下载 (${jmmViewModel.uiState.jmmMetadata!!.size.toSpaceSize()})"
        DownLoadStatus.DownLoading -> "下载中 (${downLoadInfo.dSize} / ${downLoadInfo.size}) "
        DownLoadStatus.PAUSE -> "暂停 (${downLoadInfo.dSize} / ${downLoadInfo.size}) "
        DownLoadStatus.Install -> "安装中..."
        DownLoadStatus.OPEN -> "打开"
        DownLoadStatus.FAIL -> "重新下载"
      }
      Text(text = text, fontSize = 20.sp)
    }
  }
}

private fun String.toSpaceSize(): String {
  val size = this.toFloat()
  val GB = 1024 * 1024 * 1024 // 定义GB的计算常量
  val MB = 1024 * 1024 // 定义MB的计算常量
  val KB = 1024 // 定义KB的计算常量
  val df = DecimalFormat("0.00");//格式化小数
  return if (size / GB >= 1) {
    df.format(size / GB) + " GB ";
  } else if (size / MB >= 1) {
    df.format(size / MB) + " MB ";
  } else if (size / KB >= 1) {
    //如果当前Byte的值大于等于1KB
    df.format(size / KB) + " KB ";
  } else {
    "$size B ";
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
      model = jmmMetadata.iconUrl,
      contentDescription = null,
      modifier = Modifier
        .size(80.dp)
        .clip(RoundedCornerShape(6.dp))
    )

    Spacer(modifier = Modifier.width(16.dp))

    Box(
      modifier = Modifier.fillMaxSize()
    ) {
      Text(
        text = jmmMetadata.title, fontSize = 24.sp, modifier = Modifier.align(Alignment.TopStart)
      )

      Column(modifier = Modifier.align(Alignment.BottomStart)) {
        Text(text = "广告检测 人工复检 绿色应用", fontSize = 12.sp, color = Color.Gray)

        Text(text = "版本：1.0.0", fontSize = 12.sp, color = Color.Gray)
      }
    }

    Spacer(modifier = Modifier.width(16.dp))
  }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun CaptureListView(jmmMetadata: JmmMetadata) {
  LazyRow {
    items(jmmMetadata.appCaptures) {
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

@Composable
private fun AppIntroductionView(jmmMetadata: JmmMetadata) {
  val expanded = remember { mutableStateOf(true) }
  Column {
    Text(text = "应用介绍", fontSize = 24.sp, fontStyle = FontStyle.Italic)

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
        model = R.mipmap.ic_launcher_round, contentDescription = null,
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
package info.bagen.rust.plaoc.microService.sys.jmm.ui

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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
import info.bagen.rust.plaoc.microService.sys.jmm.JmmMetadata
import java.text.DecimalFormat

@Composable
fun MALLBrowserView(
  jmmViewModel: JmmManagerViewModel,
  onDownLoad: (String, String) -> Unit
) {
  jmmViewModel.uiState.jmmMetadata?.let { jmmMetadata ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
      LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { HeadContent(jmmMetadata = jmmMetadata) }
        item { ImageListView(jmmMetadata = jmmMetadata) }
        item { AppIntroductionView(jmmMetadata = jmmMetadata) }
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .background(Color.White.copy(0.8f))
      ) {
        Text(
          text = "安装( ${jmmMetadata.size.toSpaceSize()} )",
          modifier = Modifier
            .align(Alignment.Center)
            .padding(8.dp)
            .size(200.dp, 40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Blue)
            .clickable { onDownLoad(jmmMetadata.main_url, jmmMetadata.title) },
          textAlign = TextAlign.Center,
          color = Color.White,
          fontSize = 20.sp
        )
      }
    }
  }
}

private fun String.toSpaceSize(): String {
  /*val size = this.toLong()
  Log.e("lin.huang", "String::toSpaceSize $this,$size=====${size < 1024 * 1024 * 1024 *1024}")
  if (size < 1024) return "$size B"
  if (size < 1024 * 1024) return "${size / 1024} KB"
  if (size < 1024 * 1024 * 1024) return "${size / 1024 / 1024} MB"
  if (size < 1024 * 1024 * 1024 * 1024) return "${size / 1024 / 1024 / 1024} GB"
  return "${size / 1024 / 1024 / 1024 / 1024} TB"*/

  val size = this.toFloat()
  val GB = 1024 * 1024 * 1024;//定义GB的计算常量
  val MB = 1024 * 1024;//定义MB的计算常量
  val KB = 1024;//定义KB的计算常量
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
fun InstallBrowserView(jmmViewModel: JmmManagerViewModel) {
  Box(modifier = Modifier.fillMaxSize()) {
    Text(text = "当前是安装界面", modifier = Modifier.align(Alignment.Center))
  }
}

@Composable
fun UninstallBrowserView(jmmViewModel: JmmManagerViewModel) {
  Box(modifier = Modifier.fillMaxSize()) {
    Text(text = "当前是卸载界面", modifier = Modifier.align(Alignment.Center))
  }
}

@Composable
private fun HeadContent(jmmMetadata: JmmMetadata) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
  ) {
    AsyncImage(
      model = jmmMetadata.iconUrl,
      contentDescription = null,
      modifier = Modifier
        .size(80.dp)
        .clip(RoundedCornerShape(6.dp))
    )

    Spacer(modifier = Modifier.width(16.dp))

    Column(
      modifier = Modifier
        .weight(1.0f)
        .align(Alignment.CenterVertically)
    ) {
      Text(text = jmmMetadata.title, fontSize = 24.sp)

      Text(text = "广告检测 人工复检 绿色应用")

      Text(text = "版本：1.0.0")
    }

    Spacer(modifier = Modifier.width(16.dp))
  }
}

@Composable
private fun ImageListView(jmmMetadata: JmmMetadata) {
  LazyRow {
  }
}

@Composable
private fun AppIntroductionView(jmmMetadata: JmmMetadata) {
  val expanded = remember { mutableStateOf(false) }
  Column {
    Text(text = "应用介绍", fontSize = 24.sp, fontStyle = FontStyle.Italic)

    Box(
      modifier = Modifier
        .animateContentSize()
        .clickable { expanded.value = !expanded.value }
    ) {
      Text(
        text = jmmMetadata.introduction,
        maxLines = if (expanded.value) Int.MAX_VALUE else 2,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
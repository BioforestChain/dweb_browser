package info.bagen.dwebbrowser.microService.desktop.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import info.bagen.dwebbrowser.R
import info.bagen.dwebbrowser.microService.desktop.model.AppInfo
import info.bagen.dwebbrowser.microService.desktop.model.LocalOpenList
import org.dweb_browser.browserUI.bookmark.clickableWithNoEffect

@Composable
internal fun DrawerView() {
  val localHeight = LocalConfiguration.current.screenHeightDp
  val localWidth = LocalConfiguration.current.screenWidthDp
  val localOpenList = LocalOpenList.current
  Box(modifier = Modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .width((localWidth * 0.3f - 32).dp)
        .fillMaxHeight()
        .padding(top = 56.dp, bottom = (localHeight * 0.15f).dp)
        .shadow(2.dp, shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
        .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
        .background(MaterialTheme.colorScheme.background)
        .align(Alignment.CenterEnd),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {

      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(8.dp)
      ) {
        itemsIndexed(localOpenList) { index: Int, item: AppInfo ->
          AsyncImage(
            model = item.jmmMetadata.icon,
            contentDescription = "Icon",
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(8.dp))
              .clickableWithNoEffect {
                item.screenType.value =
                  if (item.expand) AppInfo.ScreenType.Full else AppInfo.ScreenType.Half
                localOpenList.remove(item)
                localOpenList.add(item)
              }
          )
        }
      }

      Spacer(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(MaterialTheme.colorScheme.outlineVariant)
      )

      Icon(
        imageVector = ImageVector.vectorResource(R.drawable.ic_main_home),
        contentDescription = "Home",
        modifier = Modifier
          .padding(8.dp)
          .clickableWithNoEffect {
            localOpenList.forEach { it.screenType.value = AppInfo.ScreenType.Hide }
          },
        tint = MaterialTheme.colorScheme.primary
      )
    }
  }
}

@Preview
@Composable
internal fun DrawerPreview() {
  DrawerView()
}
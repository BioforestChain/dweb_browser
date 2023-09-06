<script lang="ts" setup>
import wallpaper_url from "src/assets/wallpaper.webp";
import { getWidgetInfo, watchBrowserAppInfo, watchDesktopAppInfo } from "src/provider/api.ts";
import type { $DeskLinkMetaData, $TileSizeType, $WidgetAppData, $WidgetCustomData } from "src/types/app.type.ts";
import { Ref, StyleValue, onMounted, onUnmounted, ref } from "vue";
import TileItem from "../components/tile-item/tile-item.vue";
import TilePanel from "../components/tile-panel/tile-panel.vue";
import WidgetApp from "../components/widget-app/widget-app.vue";
import WidgetCustom from "../components/widget-custom/widget-custom.vue";
import WidgetAppOverlay from "../components/widget-menu-overlay/widget-menu-overlay.vue";
import WidgetWebApp from "../components/widget-webapp/widget-webapp.vue";

type $LayoutInfo = (
  | {
      type: "app";
      data: $WidgetAppData;
    }
  | {
      type: "webapp";
      data: $DeskLinkMetaData;
    }
  | {
      type: "link";
      data: $DeskLinkMetaData;
    }
  | {
      type: "widget";
      data: $WidgetCustomData;
    }
  | {
      type: "blank";
    }
) & {
  xywh: $XYWH;
};

interface $XYWH {
  x?: number;
  y?: number;
  w: $TileSizeType;
  h: $TileSizeType;
}

const layoutInfoListRef: Ref<$LayoutInfo[]> = ref([]);

// 监听app消息的更新
const updateApps = async () => {
  const widgetList = await getWidgetInfo();

  let appList: $WidgetAppData[] = [];

  updateLayoutInfoList(widgetList, appList);

  const appInfoWatcher = watchDesktopAppInfo();
  void (async () => {
    onUnmounted(() => {
      appInfoWatcher.return();
    });
    for await (const list of appInfoWatcher) {
      console.log("desktop app=>", list);
      appList = list;
      updateLayoutInfoList(widgetList, appList);
    }
  })();
};

const updateLayoutInfoList = (widgetList: $WidgetCustomData[], appList: $WidgetAppData[]) => {
  const layoutInfoList: $LayoutInfo[] = [];
  for (const data of widgetList) {
    layoutInfoList.push({
      type: "widget",
      data,
      xywh: { w: data.size.column, h: data.size.row },
    });
  }
  for (const data of appList) {
    layoutInfoList.push({
      type: "app",
      data,
      xywh: { w: 1, h: 1 },
    });
  }

  layoutInfoListRef.value = layoutInfoList;
};

const updateWebApps = async () => {
  let webAppList: $DeskLinkMetaData[] = [];
  const webAppsWatcher = watchBrowserAppInfo();
  void (async () => {
    onUnmounted(() => {
      webAppsWatcher.return();
    });
    for await (const list of webAppsWatcher) {
      console.log("desktop webApp=>", list);
      webAppList = list;
      updateWebAppList(webAppList);
    }
  })();
};

const updateWebAppList = (webAppList:$DeskLinkMetaData[]) => {
  const list: $LayoutInfo[] = [];
  for (const data of webAppList) {
    list.push({
      type: "webapp",
      data,
      xywh: { w: 1, h: 1 },
    });
  }
  layoutInfoListRef.value = layoutInfoListRef.value.concat(list)
}

onMounted(() => {
  updateApps();
  updateWebApps();
});

const bgStyle = {
  backgroundImage: `url(${wallpaper_url})`,
} satisfies StyleValue;
</script>
<template>
  <div class="desktop" draggable="false">
    <div class="wallpaper" title="墙纸" :style="bgStyle"></div>
    <TilePanel>
      <TileItem v-for="(info, index) in layoutInfoListRef" :key="index" :width="info.xywh.w" :height="info.xywh.h">
        <WidgetApp v-if="info.type === 'app'" :key="index" :index="index" :app-meta-data="info.data"></WidgetApp>
        <WidgetWebApp
          v-if="info.type === 'webapp'"
          :key="index"
          :index="index"
          :app-meta-data="info.data"
        ></WidgetWebApp>
        <WidgetCustom
          v-if="info.type === 'widget'"
          :key="index"
          :index="index"
          :widget-meta-data="info.data"
        ></WidgetCustom>
      </TileItem>
      <WidgetAppOverlay></WidgetAppOverlay>
    </TilePanel>
  </div>
</template>
<style scoped lang="scss">
.desktop {
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
  grid-template-areas: "view";
  height: 100%;
  user-select: none;
  .wallpaper {
    grid-area: view;
    z-index: 0;
    display: grid;
    place-items: center;
    background-size: cover;
    background-position: center;
  }
}
</style>

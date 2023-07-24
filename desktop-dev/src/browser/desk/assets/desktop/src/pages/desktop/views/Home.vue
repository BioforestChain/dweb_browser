<script lang="ts" setup>
import wallpaper_url from "@/assets/wallpaper.webp";
import { getWidgetInfo, watchAppInfo } from "@/provider/api";
import type { $WidgetAppData, $TileSizeType, $WidgetCustomData } from "@/types/app.type";
import { onMounted, onUnmounted, Ref, ref, StyleValue } from "vue";
import WidgetApp from "../components/widget-app/index.vue";
import TileItem from "../components/tile-item/index.vue";
import TilePanel from "../components/tile-panel/index.vue";
import WidgetCustom from "../components/widget-custom/index.vue";

type $LayoutInfo = (
  | {
      type: "app";
      data: $WidgetAppData;
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

const layoutInfoListRef: Ref<$LayoutInfo[]> = ref([
  // {
  //   title: "app",
  //   short_name: "app name",
  //   icon: "https://dweb.waterbang.top/logo.svg",
  //   id: "waterbang.dweb",
  // },
]);

// 监听app消息的更新
const updateApps = async () => {
  const widgetList = await getWidgetInfo();

  const appInfoWatcher = watchAppInfo();
  onUnmounted(() => {
    appInfoWatcher.return();
  });
  for await (const appList of appInfoWatcher) {
    updateLayoutInfoList(widgetList, appList);
  }
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

onMounted(async () => {
  await updateApps();
});

const bgStyle = {
  backgroundImage: `url(${wallpaper_url})`,
} satisfies StyleValue;
</script>
<template>
  <div class="desktop">
    <div class="logo" :style="bgStyle">
      <img src="@/assets/logo.svg" alt="Dweb Browser" class="icon" />
      <div class="gradient_text">Dweb Browser</div>
    </div>
    <TilePanel>
      <TileItem v-for="(info, index) in layoutInfoListRef" :key="index" :width="info.xywh.w" :height="info.xywh.h">
        <widget-app v-if="info.type === 'app'" :key="index" :index="index" :app-meta-data="info.data"></widget-app>
        <WidgetCustom v-if="info.type === 'widget'" :key="index" :index="index" :widget-meta-data="info.data"></WidgetCustom>
      </TileItem>
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
  .logo {
    grid-area: view;
    z-index: 0;
    display: grid;
    place-items: center;
    background-size: cover;
    background-position: center;
    .icon {
      width: 13.5em;
      height: 13.5em;
      mix-blend-mode: color-burn;
    }
    .gradient_text {
      width: 100%;
      height: 2em;
      font-size: 20px;
      font-weight: 500;
      line-height: 1em;
      display: flex;
      justify-content: center;
      color: #fff;
      mix-blend-mode: overlay;
    }
  }
}
</style>

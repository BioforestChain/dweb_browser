<script lang="ts" setup>
import { getWidgetInfo, watchDesktopAppInfo } from "src/provider/api.ts";
import {
  MICRO_MODULE_CATEGORY,
  type $TileSizeType,
  type $WidgetAppData,
  type $WidgetCustomData,
} from "src/types/app.type.ts";
import { onMounted, onUnmounted, Ref, ref } from "vue";
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
      data: $WidgetAppData;
    }
  | {
      type: "link";
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
  id: string;
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

  const appInfoWatcher = await watchDesktopAppInfo();
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
      id: data.appId,
    });
  }

  for (const data of appList) {
    if (data.categories.includes(MICRO_MODULE_CATEGORY.Web_Browser) && data.mmid !== "web.browser.dweb") {
      layoutInfoList.push({
        type: "webapp",
        data,
        xywh: { w: 1, h: 1 },
        id: data.mmid,
      });
    } else {
      layoutInfoList.push({
        type: "app",
        data,
        xywh: { w: 1, h: 1 },
        id: data.mmid,
      });
    }
  }

  const typeOrders = ["widget", "app", "webapp"];
  layoutInfoListRef.value = layoutInfoList.sort((a, b) => typeOrders.indexOf(a.type) - typeOrders.indexOf(b.type));
  // layoutInfoListRef.value = Array(25).fill({
  //   type: "app",
  //   data: {
  //     id: "web.browser.dweb",
  //     dweb_deeplinks: ["dweb://search", "dweb://openinbrowser"],
  //     dweb_protocols: [],
  //     dweb_permissions: [],
  //     name: "Web Browser",
  //     short_name: "甲乙丙丁戊己庚辛壬癸",
  //     icons: [
  //       {
  //         src: "file:///sys/icons/web.browser.dweb.svg",
  //         type: "image/svg+xml",
  //       },
  //     ],
  //     categories: ["application", "web-browser"],
  //     shortcuts: [],
  //     version: "0.0.1",
  //     mmid: "web.browser.dweb",
  //     ipc_support_protocols: {
  //       cbor: true,
  //       protobuf: true,
  //       raw: true,
  //     },
  //     running: false,
  //     winStates: [],
  //   },
  //   xywh: {
  //     w: 1,
  //     h: 1,
  //   },
  // } as any);
};

onMounted(() => {
  updateApps();
});

const $desktop = ref<HTMLDivElement>();
const rowSize = ref(140);

let resizeOb: undefined | ResizeObserver;
onMounted(() => {
  resizeOb = new ResizeObserver((entries) => {
    const { width, height } = entries[0].contentRect;
    // console.log("xxxx=>", height, window.innerHeight);
    rowTemplateSize(height);
  });
  resizeOb.observe($desktop.value!);
});
onUnmounted(() => {
  if (resizeOb !== undefined) {
    if ($desktop.value) {
      resizeOb.unobserve($desktop.value);
    }
    resizeOb.disconnect();
  }
});

const rowTemplateSize = (height: number) => {
  if (layoutInfoListRef.value.length == 1) return 119;
  const row = Math.ceil(layoutInfoListRef.value.length / Math.floor(window.innerWidth / 94));
  // console.log("row", row, window.innerHeight, Math.floor(window.innerWidth / 94));
  const higth = height ?? window.innerHeight;
  // console.log("panelhigth", higth, $desktop.value?.clientHeight);
  const line = higth / row;
  if (line > 120) return (rowSize.value = 120);
  // console.log("result=>", line);
  rowSize.value = line;
};
</script>
<template>
  <div class="desktop" draggable="false" ref="$desktop">
    <TilePanel :rowTemplateSize="rowSize">
      <TileItem
        v-for="info in layoutInfoListRef"
        :key="info.id"
        :width="info.xywh.w"
        :height="info.xywh.h"
        :title="`${info.id}/${info.xywh.w}/${info.xywh.h}`"
      >
        <WidgetApp v-if="info.type === 'app'" :app-meta-data="info.data"></WidgetApp>
        <WidgetWebApp v-if="info.type === 'webapp'" :app-meta-data="info.data"></WidgetWebApp>
        <WidgetCustom v-if="info.type === 'widget'" :widget-meta-data="info.data"></WidgetCustom>
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
  -webkit-touch-callout: none;
}
</style>

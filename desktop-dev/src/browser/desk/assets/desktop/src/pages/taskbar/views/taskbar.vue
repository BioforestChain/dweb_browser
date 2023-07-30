<script setup lang="ts">
import AppIcon from "src/components/app-icon/app-icon.vue";
import {
  $WatchEffectAppMetadataToAppIconReturn,
  watchEffectAppMetadataToAppIcon,
} from "src/components/app-icon/appMetaDataHelper.ts";
import { $AppIconInfo } from "src/components/app-icon/types";
import { watchTaskbarAppInfo } from "src/provider/api.ts";
import { $WidgetAppData } from "src/types/app.type.ts";
import { onMounted, onUnmounted, ref, ShallowRef, shallowRef, triggerRef, watchEffect } from "vue";
import { exportApis, mainApis } from "../bridge-apis.ts";
import { icons } from "./icons/index.ts";

/** 打开桌面面板 */
const openDesktop = () => {
  mainApis.openDesktopView();
};

const appRefList = shallowRef<
  Array<
    {
      metaData: $WidgetAppData;
      ref: ShallowRef<$AppIconInfo>;
    } & $WatchEffectAppMetadataToAppIconReturn
  >
>([]);

// 监听app消息的更新
const updateApps = async () => {
  const appInfoWatcher = watchTaskbarAppInfo();
  onUnmounted(() => {
    appInfoWatcher.return();
  });
  for await (const appList of appInfoWatcher) {
    console.log("22221=>", appList);
    updateLayoutInfoList(appList);
  }
};
const updateLayoutInfoList = (appList: $WidgetAppData[]) => {
  for (const appRef of appRefList.value) {
    appRef.off();
  }
  appRefList.value = appList.map((metaData) => {
    const ref = shallowRef<$AppIconInfo>({
      src: "",
      markable: false,
      monochrome: false,
      monocolor: undefined,
    });
    const res = watchEffectAppMetadataToAppIcon({ metaData }, ref);

    return Object.assign(res, { metaData, ref: ref });
  });
  triggerRef(appRefList);
};

onMounted(async () => {
  await updateApps();
});

/// 同步div的大小到原生的窗口上
const taskbarEle = ref<HTMLDivElement>();
let resizeOb: ResizeObserver | undefined;
watchEffect(() => {
  resizeOb?.disconnect();
  resizeOb = undefined;
  if (taskbarEle.value) {
    resizeOb = new ResizeObserver((entries) => {
      for (const entry of entries) {
        const { width, height } = entry.contentRect;
        console.log("resize", entry.contentRect);
        mainApis.resize(Math.ceil(width), Math.ceil(height));
      }
    });
    resizeOb.observe(taskbarEle.value);
  }
});
</script>
<script lang="ts">
export const RENDER_APIS = {};
exportApis(RENDER_APIS);
</script>
<template>
  <div class="taskbar" ref="taskbarEle">
    <div class="panel">
      <div class="app-icon" v-for="(app, index) in appRefList" :key="index">
        <AppIcon :icon="app.ref.value"></AppIcon>
      </div>
      <div class="app-icon">
        <img class="img" :src="icons.anquanzhongxin" draggable="false" />
      </div>
      <div class="app-icon">
        <img class="img" :src="icons.kandianying" draggable="false" />
      </div>
      <div class="app-icon">
        <img class="img" :src="icons.naozhong" draggable="false" />
      </div>
      <div class="app-icon">
        <img class="img" :src="icons.xiangji" draggable="false" />
      </div>
      <hr class="divider" />

      <div class="app-icon" @click="openDesktop">
        <img class="img" :src="icons.quanbufenlei" draggable="false" />
      </div>
    </div>
  </div>
</template>
<style scoped lang="scss">
.taskbar {
  display: block;
  height: min-content;
  width: min-content;
  -webkit-app-region: drag;
  cursor: move;
  user-select: none;
}
.panel {
  padding: 1em;
  display: flex;
  flex-direction: column;
  gap: 1em;
}
.divider {
  width: 100%;
  height: 1px;
  border-radius: 1px;
  border: 0;
  background: linear-gradient(to right, transparent, currentColor, transparent);
}
.app-icon {
  cursor: pointer;
  -webkit-app-region: no-drag;
  width: 60px;
  height: 60px;
  border-radius: 15px;
  background-color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0px 3px 5px rgba(0, 0, 0, 0.3);
  .img {
    width: 90%;
    height: auto;
  }
}
</style>

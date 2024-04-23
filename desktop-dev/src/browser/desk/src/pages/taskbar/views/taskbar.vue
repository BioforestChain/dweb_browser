<script setup lang="ts">
import AppIcon from "@/components/app-icon/app-icon.vue";
import {
  $WatchEffectAppMetadataToAppIconReturn,
  watchEffectAppMetadataToAppIcon,
} from "@/components/app-icon/appMetaDataHelper.ts";
import { $AppIconInfo } from "@/components/app-icon/types";
import MenuBox from "@/components/menu-box/menu-box.vue";
import SvgIcon from "@/components/svg-icon/svg-icon.vue";
import {
  doToggleTaskbar,
  openApp,
  quitApp,
  resizeTaskbar,
  toggleDesktopView,
  toggleMaximize,
  vibrateHeavyClick,
  watchTaskbarAppInfo,
  watchTaskBarStatus,
} from "@/provider/api.ts";
import { $TaskBarState, $WidgetAppData } from "@/types/app.type.ts";
import { computed, onMounted, onUnmounted, ref, ShallowRef, shallowRef, triggerRef } from "vue";
import { icons } from "./icons/index.ts";
import x_circle_svg from "./icons/x-circle.svg";

/** 打开桌面面板 */
const toggleDesktopButton = async () => {
  const boundList = await toggleDesktopView();
};

const isDesktop = computed(() => navigator.userAgent.toLowerCase().indexOf(" electron/") > -1);

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
  const appInfoWatcher = await watchTaskbarAppInfo();
  onUnmounted(() => {
    appInfoWatcher.return();
  });
  for await (const appList of appInfoWatcher) {
    // console.log("taskbar AppList=>", appList);
    updateLayoutInfoList(appList);
  }
};

const updateTaskbarStatus = async () => {
  const taskBarStatusWatcher = await watchTaskBarStatus();
  onUnmounted(() => {
    taskBarStatusWatcher.return();
  });
  for await (const taskBarStatus of taskBarStatusWatcher) {
    console.log("taskbar status=>", taskBarStatus);
    updateTaskBarStatus(taskBarStatus);
  }
};

//触发列表更新
const updateLayoutInfoList = (appList: $WidgetAppData[]) => {
  for (const appRef of appRefList.value) {
    appRef.off();
  }
  appRefList.value = appList.map((metaData) => {
    const ref = shallowRef<$AppIconInfo>({
      src: "",
      maskable: false,
      monochrome: false,
      monocolor: undefined,
    });
    const res = watchEffectAppMetadataToAppIcon({ metaData }, ref);

    return Object.assign(res, { metaData, ref: ref });
  });
  triggerRef(appRefList); // 强制触发深度更新
};

// 触发taskBar状态更新
const updateTaskBarStatus = (taskBarStatus: $TaskBarState) => {
  isFocusTaskBar.value = taskBarStatus.focus;
};

const doOpen = async (metaData: $WidgetAppData) => {
  // 是否是单app模式
  if (isSingleIconMode.value) {
    // 是的话需要打开float
    await doToggleTaskbar(true);
    return;
  }
  if (showMenuOverlayRef.value === metaData.mmid) {
    return;
  }
  await openApp(metaData.mmid);
};
const doToggleMaximize = async (metaData: $WidgetAppData) => {
  await toggleMaximize(metaData.mmid);
};
const doExit = async (metaData: $WidgetAppData) => {
  await quitApp(metaData.mmid);
  tryCloseMenuOverlay(metaData);
};

const showMenuOverlayRef = ref<$WidgetAppData["mmid"] | undefined>();
// 响应右键点击事件
const tryOpenMenuOverlay = (metaData: $WidgetAppData) => {
  vibrateHeavyClick();
  if (metaData.running) {
    showMenuOverlayRef.value = metaData.mmid;
  }
};
const tryCloseMenuOverlay = (metaData: $WidgetAppData) => {
  if (showMenuOverlayRef.value === metaData.mmid) {
    showMenuOverlayRef.value = undefined;
  }
};
window.addEventListener("blur", () => {
  showMenuOverlayRef.value = undefined;
});

onMounted(async () => {
  updateApps();
  updateTaskbarStatus();
});

const calcMaxHeight = () => `${screen.availHeight - 45}px`;
/**
 * @TODO 因为这里限制了maxHeight，所以肯定会带来滚动问题，所以未来应用很多的时候，需要开启左右滑动的功能来进行滚动
 */
const maxHeight = ref(calcMaxHeight());

window.addEventListener("resize", () => {
  maxHeight.value = calcMaxHeight();
});

/// 同步div的大小到原生的窗口上
const taskbarEle = ref<HTMLDivElement>();
const isFocusTaskBar = ref(false);
// 是否使用单个App图标的模式
const signalIcon = computed(() => {
  // 寻找符合调教的应用
  const findApp = appRefList.value.find((app) =>
    app.metaData.winStates.find((win) => win.mode === "maximize" && win.focus),
  );
  return findApp;
});
// 是否是单app 模式
const isSingleIconMode = computed(() => {
  if (isFocusTaskBar.value) return false;
  return signalIcon.value !== undefined;
});
// 只显示需要显示的app
const showAppIcons = computed(() => {
  if (!isFocusTaskBar.value) {
    return signalIcon.value ? [signalIcon.value] : appRefList.value;
  }
  return appRefList.value;
});

let resizeOb: ResizeObserver | undefined;
onMounted(() => {
  const element = taskbarEle.value;
  if (element) {
    resizeOb = new ResizeObserver(async (entries) => {
      for (const entry of entries) {
        const { width: _width, height: _height } = entry.contentRect;
        const height = Math.ceil(_height);
        const width = Math.ceil(_width);
        console.log("ResizeObserver", height, width);
        await resizeTaskbar(width, height);
      }
    });
    resizeOb.observe(element);
    requestAnimationFrame(() => {
      console.log("requestAnimationFrame", element.clientWidth, element.clientHeight);
      resizeTaskbar(element.clientWidth, element.clientHeight);
    });
  }
});

const iconSize = "45px";
</script>
<template>
  <div class="taskbar min-w-[4.0rem]" ref="taskbarEle">
    <div class="panel" v-for="(appIcon, index) in showAppIcons" :key="index">
      <button class="app-icon-wrapper z-grid" :class="{ active: appIcon.metaData.running }">
        <transition name="scale">
          <MenuBox @menu="tryOpenMenuOverlay(appIcon.metaData)">
            <AppIcon
              class="z-view"
              :icon="appIcon.ref.value"
              :size="iconSize"
              bg-color="#FFF"
              bg-disable-translucent
              @click="doOpen(appIcon.metaData)"
              @dblclick="doToggleMaximize(appIcon.metaData)"
            >
              <button
                v-if="showMenuOverlayRef === appIcon.metaData.mmid"
                class="exit-button"
                @blur="tryCloseMenuOverlay(appIcon.metaData)"
                @click="doExit(appIcon.metaData)"
              >
                <SvgIcon class="exit-icon" :src="x_circle_svg" alt="exit app"></SvgIcon>
              </button>
            </AppIcon>
          </MenuBox>
        </transition>
        <!-- <div class="running-dot z-view" v-if="appIcon.metaData.running">
          <span class="dot"></span>
        </div> -->
      </button>
    </div>
    <hr v-if="appRefList.length !== 0" class="my-divider" />
    <button class="desktop-button app-icon-wrapper z-grid" @click="toggleDesktopButton">
      <AppIcon
        class="z-view"
        :icon="icons.layout_panel_top"
        :size="iconSize"
        bg-image="linear-gradient(to bottom, #f64f59, #c471ed, #12c2e9)"
        bg-disable-translucent
      ></AppIcon>
    </button>
  </div>
</template>
<style scoped lang="scss">
.taskbar {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: min-content;
  width: min-content;
  max-height: v-bind(maxHeight);
  -webkit-app-region: drag;
  cursor: move;
  user-select: none;
  overflow: hidden;
}
.panel {
  display: flex;
  flex-direction: column;
  gap: 1em;
  flex: 1;
  flex-wrap: wrap;
  justify-content: space-around;
  padding: 0.4rem;
}
.my-divider {
  width: 90%;
  height: 1px;
  border-radius: 1px;
  border: 0;
  background: linear-gradient(to right, transparent, currentColor, transparent);
  margin: 0;
  flex-shrink: 0;
}
.desktop-button {
  padding: 0.4rem;
  box-sizing: content-box;
  flex-shrink: 0;
}
button {
  -webkit-app-region: no-drag;
}
.app-icon-wrapper {
  cursor: pointer;
  width: 45px;
  height: 45px;
  .img {
    width: 90%;
    height: auto;
  }
}
.active {
  box-shadow:
    rgb(0 0 0 / 40%) 0px 2px 4px,
    rgb(0 0 0 / 30%) 0px 7px 13px -3px,
    rgb(0 0 0 / 20%) 0px -3px 0px inset;
  border-radius: 5px;
}
//.running-dot {
//  width: 100%;
//  display: flex;
//  align-items: center;
//  justify-content: flex-end;
//  pointer-events: none;
//  .dot {
//    display: inline-block;
//   --dot-size: 0.35em;
//    width: var(--dot-size);
//    height: var(--dot-size);
//    border-radius: 50%;
//    background-color: rgba($color: #fff, $alpha: 0.5);
//    transform: translateX(calc((1em - var(--dot-size) * 2 / 3)));
//  }
//}
.exit-button {
  width: 100%;
  height: 100%;
  border-radius: 50%;

  backdrop-filter: blur(4px) saturate(1.25) contrast(1.25);
  background: rgba(0, 0, 0, 0.3);
  mask-image: radial-gradient(white, #ffffff00);
  .exit-icon {
    color: rgb(0 0 0 / 80%);
  }
}
</style>

<style lang="scss">
:root {
  overflow: hidden !important;
}
</style>

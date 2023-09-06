<script setup lang="ts">
import AppIcon from "src/components/app-icon/app-icon.vue";
import {
$WatchEffectAppMetadataToAppIconReturn,
watchEffectAppMetadataToAppIcon,
} from "src/components/app-icon/appMetaDataHelper.ts";
import { $AppIconInfo } from "src/components/app-icon/types";
import SvgIcon from "src/components/svg-icon/svg-icon.vue";
import {
doToggleTaskbar,
openApp,
quitApp,
resizeTaskbar,
toggleDesktopView,
toggleMaximize,
watchTaskBarStatus,
watchTaskbarAppInfo
} from "src/provider/api.ts";
import { $TaskBarState, $WidgetAppData } from "src/types/app.type.ts";
import { ShallowRef, computed, onMounted, onUnmounted, ref, shallowRef, triggerRef } from "vue";
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
  const appInfoWatcher = watchTaskbarAppInfo();
  onUnmounted(() => {
    appInfoWatcher.return();
  });
  for await (const appList of appInfoWatcher) {
    console.log("taskbar AppList=>", appList);
    updateLayoutInfoList(appList);
  }
};

const updateTaskbarStatus = async () => {
  const taskBarStatusWatcher = watchTaskBarStatus()
  onUnmounted(() => {
    taskBarStatusWatcher.return()
  });
  for await (const taskBarStatus of taskBarStatusWatcher) {
    console.log("taskbar status=>", taskBarStatus);
    updateTaskBarStatus(taskBarStatus)
  }
}

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
  triggerRef(appRefList);
};

// 触发taskBar状态更新
const updateTaskBarStatus = (taskBarStatus:$TaskBarState) => {
  if (taskBarStatus.focus) {
    isFocusTaskBar.value = true
  }
}

const doOpen = async (metaData: $WidgetAppData) => {
  if (isSingleIconMode.value) {
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
const isFocusTaskBar = ref(false)
// 是否使用单个App图标的模式
const signalIcon = computed(() => {
  // 寻找符合调教的应用
  const findApp = appRefList.value.find((app) => app.metaData.winStates.find((win) => win.mode ==="maximize" && win.focus));
  return findApp;
});
const isSingleIconMode = computed(() => signalIcon.value !== undefined);
// 只显示需要显示的app
const showAppIcons = computed(() => {
  if (!isFocusTaskBar.value) {
    return signalIcon.value ? [signalIcon.value] : appRefList.value
  }
  return appRefList.value
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
        await resizeTaskbar(width, height);
      }
    });
    resizeOb.observe(element);
    requestAnimationFrame(() => {
      resizeTaskbar(element.clientWidth, element.clientHeight);
    });
  }
});
const iconSize = "45px";
</script>
<template>
  <div class="taskbar" ref="taskbarEle">
    <div class="panel" :class="{ 'p-4': isSingleIconMode }">
      <button
        class="app-icon-wrapper z-grid"
        v-for="(appIcon, index) in showAppIcons"
        :key="index"
        :class="{ active: appIcon.metaData.running }"
      >
        <transition name="scale">
          <AppIcon
            class="z-view"
            :icon="appIcon.ref.value"
            :size="iconSize"
            bg-color="#FFF"
            bg-disable-translucent
            @click="doOpen(appIcon.metaData)"
            @dblclick="doToggleMaximize(appIcon.metaData)"
            @contextmenu="tryOpenMenuOverlay(appIcon.metaData)"
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
        </transition>
        <div class="running-dot z-view" v-if="appIcon.metaData.running">
          <span class="dot"></span>
        </div>
      </button>
    </div>
    <template v-if="!isSingleIconMode">
      <hr v-if="appRefList.length !== 0" class="my-divider" />
      <button class="desktop-button app-icon-wrapper z-grid m-4" @click="toggleDesktopButton">
        <AppIcon
          class="z-view"
          :icon="icons.layout_panel_top"
          :size="iconSize"
          bg-image="linear-gradient(to bottom, #f64f59, #c471ed, #12c2e9)"
          bg-disable-translucent
        ></AppIcon>
      </button>
    </template>
  </div>
</template>
<style scoped lang="scss">
.taskbar {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: min-content;
  // width: min-content;
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
}
.my-divider {
  width: 90%;
  height: 1px;
  border-radius: 1px;
  border: 0;
  background: linear-gradient(to right, transparent, currentColor, transparent);
  margin-top: 16px;
  flex-shrink: 0;
}
.desktop-button {
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
  box-shadow: rgba(0, 0, 0, 0.19) 0px 10px 20px, rgba(0, 0, 0, 0.23) 0px 6px 6px;
  border-radius: 16px;
}
.running-dot {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  pointer-events: none;
  .dot {
    display: inline-block;
    --dot-size: 0.35em;
    width: var(--dot-size);
    height: var(--dot-size);
    border-radius: 50%;
    background-color: rgba($color: #fff, $alpha: 0.5);
    transform: translateX(calc((1em - var(--dot-size) * 2 / 3)));
  }
}
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

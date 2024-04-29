<script lang="ts" setup>
import AppIcon from "@/components/app-icon/app-icon.vue";
import { watchEffectAppMetadataToAppIcon } from "@/components/app-icon/appMetaDataHelper.ts";
import { $AppIconInfo } from "@/components/app-icon/types";
import AppName from "@/components/app-name/app-name.vue";
import MenuBox from "@/components/menu-box/menu-box.vue";
import SvgIcon from "@/components/svg-icon/svg-icon.vue";
import { closeBrowser, detailApp, openApp, quitApp, vibrateHeavyClick } from "@/provider/api.ts";
import "@/provider/shim.ts";
import type { $WidgetAppData } from "@/types/app.type.ts";
import "@plaoc/plugins";
import { vOnClickOutside } from "@vueuse/components";
import { useThrottleFn } from "@vueuse/core";
import { computed, reactive, ref, shallowRef, watch } from "vue";
import AppUnInstallDialog from "../app-uninstall-dialog/app-uninstall-dialog.vue";
import { widgetInputBlur } from "../widget-custom/widget-custom.vue";
import { ownReason, showOverlay } from "../widget-menu-overlay/widget-menu-overlay.vue";
import delete_svg from "/delete.svg";
import details_svg from "/details.svg";
import quit_svg from "/quit.svg";
import share_svg from "/share.svg";

const $appHtmlRefHook = ref<HTMLDivElement | null>(null);

const isShowMenu = ref(false);
const isShowOverlay = computed(() => ownReason(isShowMenu));

const snackbar = reactive({
  show: false,
  type: "success", // success,primary,rbga,#fff
  timeOut: 2000,
  text: "退出成功！",
});

const props = defineProps({
  appMetaData: {
    type: Object as () => $WidgetAppData,
    required: true,
  },
});
const appid = computed(() => props.appMetaData.mmid);
const appname = computed(() => props.appMetaData.short_name ?? props.appMetaData.name);
const appicon = shallowRef<$AppIconInfo>({ src: "", monochrome: false, maskable: false });
watch(
  () => props.appMetaData.icons,
  () => {
    watchEffectAppMetadataToAppIcon({ metaData: props.appMetaData }, appicon);
  },
);
watchEffectAppMetadataToAppIcon({ metaData: props.appMetaData }, appicon);

const opening = ref(false);
const closing = ref(false);
const animationiteration = ref(false);
watch(animationiteration, () => {
  if (animationiteration.value === true) {
    animationiteration.value = false;
    /// 如果running的状态发生改变，那么修改ing的值
    if (props.appMetaData.running) {
      opening.value = false;
    } else {
      closing.value = false;
    }
  }
});

const emit = defineEmits(["uninstall"]);

let menuCloser: CloseWatcher | null = null;
//长按事件的回调
const $menu = {
  show: () => {
    vibrateHeavyClick(); // 震动
    isShowMenu.value = true;
    showOverlay(isShowMenu, true);
    menuCloser?.close();
    const closer = (menuCloser = new CloseWatcher());
    menuCloser.addEventListener("close", () => {
      if (closer === menuCloser) {
        menuCloser = null;
      }
      $menu.close();
    });
  },
  close: () => {
    isShowMenu.value = false;
    showOverlay(isShowMenu, false);
    menuCloser?.close();
  },
};

const doOpen = () =>
  useThrottleFn(async () => {
    opening.value = true;
    if ((await openApp(appid.value).catch(() => (opening.value = false))) === false) {
      snackbar.text = `${appname.value} 启动失败`;
      snackbar.timeOut = 1500;
      snackbar.type = "error";
      snackbar.show = true;
    }
    opening.value = false;
  }, 500)();

async function doQuit() {
  // 如果是移除 browserApp需要顺便把窗口移除
  if (appid.value === "web.browser.dweb") {
    closeBrowser();
  }
  if (await quitApp(appid.value)) {
    snackbar.text = `${appname.value} 已退出后台。`;
    snackbar.timeOut = 1500;
    snackbar.type = "primary";
    snackbar.show = true;
  }
}
async function showAppDetailApp() {
  // console.log(props.appMetaData);
  opening.value = true;
  if ((await detailApp(appid.value).catch(() => false)) === false) {
    snackbar.text = `${appname.value} 详情页打开失败`;
    snackbar.timeOut = 1500;
    snackbar.type = "error";
    snackbar.show = true;
  }
  opening.value = false;
}
const showUninstallDialog = ref(false);
function showUninstall() {
  showUninstallDialog.value = true;
}

const onJmmUnInstallDialogClosed = (confirmed: boolean) => {
  showUninstallDialog.value = false;
  if (confirmed) {
    emit("uninstall");
  }
};
function outsideCloseMenu(e: PointerEvent) {
  e.preventDefault();
  $menu.close();
}
</script>
<template>
  <div ref="$appHtmlRefHook" class="app" draggable="false">
    <v-menu :modelValue="isShowMenu" @update:modelValue="$menu.close" location="bottom" transition="menu-popuper">
      <template v-slot:activator="{ props }">
        <MenuBox
          v-bind="props"
          class="app-wrap ios-ani"
          :class="{ overlayed: isShowOverlay, focused: isShowMenu }"
          @click="doOpen"
          @click.capture="widgetInputBlur"
          @menu="$menu.show"
        >
          <AppIcon
            :class="{
              'animate-slow-bounce': opening,
              'animate-app-pulse': closing,
            }"
            @animationiteration="animationiteration = true"
            size="58px"
            :icon="appicon"
          ></AppIcon>
          <AppName :style="{ opacity: isShowMenu ? 0 : 1 }"> {{ appname }}</AppName>
        </MenuBox>
      </template>

      <div class="menu ios-ani" v-on-click-outside="outsideCloseMenu">
        <button v-ripple class="item quit" @click="doQuit" :disabled="!appMetaData.running">
          <SvgIcon class="icon" :src="quit_svg" alt="退出" />
          <p class="title">退出</p>
        </button>

        <button
          v-ripple
          v-if="!['web.browser.dweb', 'gui.jmm.browser.dweb'].includes(appMetaData.mmid)"
          class="item details"
          @click="showAppDetailApp"
        >
          <SvgIcon class="icon" :src="details_svg" alt="详情" />
          <p class="title">详情</p>
        </button>
        <button
          v-ripple
          v-if="!['web.browser.dweb', 'gui.jmm.browser.dweb'].includes(appMetaData.mmid)"
          class="item delete"
          @click="showUninstall"
        >
          <SvgIcon class="icon" :src="delete_svg" alt="卸载" />
          <p class="title">卸载</p>
        </button>
        <button v-ripple class="item delete" disabled>
          <SvgIcon class="icon" :src="share_svg" alt="分享" />
          <p class="title">分享</p>
        </button>
      </div>
    </v-menu>
  </div>
  <v-snackbar v-model="snackbar.show" :color="snackbar.type" :timeout="snackbar.timeOut" variant="tonal">
    <div class="text-center">{{ snackbar.text }}</div>
  </v-snackbar>

  <AppUnInstallDialog
    :appId="appid"
    :appIcon="appicon"
    :appName="appname"
    :show="showUninstallDialog"
    @close="onJmmUnInstallDialogClosed"
  ></AppUnInstallDialog>
</template>
<style lang="scss">
.menu-popuper-enter-from,
.menu-popuper-leave-to {
  & > .menu {
    transform: scale(0.5);
    filter: blur(20px) opacity(0);
  }
  // transform: translate(0, 0) scale(0.5);
}

.memu-activator-enter-active,
.memu-activator-leave-active {
  z-index: 3;
}
</style>
<style scoped lang="scss">
:scope {
  --icon-size: 60px;
}

.app {
  align-self: flex-start;

  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  margin-bottom: 10px;
  .app-wrap {
    display: flex;
    flex-direction: column;
    place-items: center;
    z-index: 0;
    &.focused {
      scale: 1.05;
    }
    &.overlayed {
      z-index: 3;
    }
  }
}

.menu {
  display: flex;
  flex-direction: row;
  justify-content: space-around;

  font-size: 0.9em;
  color: #151515;
  background-color: rgba(255, 255, 255, 0.62);
  backdrop-filter: contrast(1.5) brightness(1.5);
  border-radius: 1.5em;
  overflow: hidden;
  margin-top: 0.5em;
  &.ios-ani {
    transform-origin: top left;
  }
  .item {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    font-size: 1em;
    padding: 0.8em;
    min-width: 6em;
    border-radius: 0.8em;
    &:disabled {
      mix-blend-mode: overlay;
    }
    .icon {
      --icon-size: 1.8em;
      width: var(--icon-size);
      height: var(--icon-size);
    }
    .title {
      font-size: 1em;
      padding-top: 0.5em;
      white-space: nowrap;
    }
  }
}
</style>

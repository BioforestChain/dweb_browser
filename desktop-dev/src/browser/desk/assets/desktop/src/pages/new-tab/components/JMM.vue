<script lang="ts" setup>
import { openApp, quitApp, vibrateHeavyClick } from "@/provider/api";
import { CloseWatcher } from "@/provider/shim";
import type { $DesktopAppMetaData } from "@/types/app.type";
import { onLongPress } from "@vueuse/core";
import { onMounted, reactive, ref } from "vue";
import JmmUnInstallDialog from "./JmmUnInstallDialog.vue";
import squircle_svg from "./squircle.svg?raw";
import SvgIcon from "./SvgIcon.vue";

const $appHtmlRefHook = ref<HTMLDivElement | null>(null);

const isShowMenu = ref(false);
const isShowOverlay = ref(false);

const snackbar = reactive({
  show: false,
  type: "success", // success,primary,rbga,#fff
  timeOut: 2000,
  text: "退出成功！",
});

const props = defineProps({
  appMetaData: {
    type: Object as () => $DesktopAppMetaData,
    required: true,
  },
  index: {
    type: Number,
    required: true,
  },
});
const emit = defineEmits(["uninstall"]);

onMounted(() => {});
let menuCloser: CloseWatcher | null = null;
//长按事件的回调
const showMenu = () => {
  vibrateHeavyClick(); // 震动
  isShowMenu.value = true;
  menuCloser = new CloseWatcher();
  menuCloser.addEventListener("close", () => {
    isShowMenu.value = false;
  });
};
function closeMenu() {
  isShowMenu.value = false;
  menuCloser?.close();
  menuCloser = null;
}

onLongPress($appHtmlRefHook, showMenu, {
  modifiers: { prevent: true },
});

async function doOpen() {
  await openApp(props.appMetaData.id);
}

async function doQuit() {
  if (await quitApp(props.appMetaData.id)) {
    snackbar.text = `${props.appMetaData.short_name} 已退出后台。`;
    snackbar.timeOut = 1500;
    snackbar.type = "primary";
    snackbar.show = true;
  }
}
function showAppDetailApp() {
  console.log(props.appMetaData);
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
</script>
<template>
  <div ref="$appHtmlRefHook" class="app" draggable="false">
    <v-menu :modelValue="isShowMenu" @update:modelValue="closeMenu" location="bottom" transition="menu-popuper">
      <template v-slot:activator="{ props }">
        <div
          class="app-wrap ios-ani"
          :class="{ overlayed: isShowOverlay, focused: isShowMenu }"
          @click="isShowMenu = false"
        >
          <div class="app-icon" v-bind="props" @click="doOpen" @contextmenu="showMenu">
            <div class="bg backdrop-blur-sm" v-html="squircle_svg"></div>
            <img class="fg" :src="appMetaData.icon" />
          </div>
          <div class="app-name line-clamp-2 backdrop-blur-sm" :style="{ opacity: isShowMenu ? 0 : 1 }">
            {{ appMetaData.short_name }}
          </div>
        </div>
        <Transition name="fade" @beforeEnter="() => (isShowOverlay = true)" @afterLeave="() => (isShowOverlay = false)">
          <div class="overlay ios-ani" v-if="isShowMenu" @click="closeMenu"></div>
        </Transition>
      </template>

      <div class="menu ios-ani" v-show="isShowMenu">
        <button v-ripple class="item quit" @click="doQuit" :disabled="!$props.appMetaData.running">
          <SvgIcon class="icon" src="../../../../src/assets/images/quit.svg" alt="退出" />
          <p class="title">退出</p>
        </button>

        <button v-ripple class="item details" @click="showAppDetailApp">
          <SvgIcon class="icon" src="../../../../src/assets/images/details.svg" alt="详情" />
          <p class="title">详情</p>
        </button>
        <button v-ripple class="item delete" @click="showUninstall">
          <SvgIcon class="icon" src="../../../../src/assets/images/delete.svg" alt="卸载" />
          <p class="title">卸载</p>
        </button>
      </div>
    </v-menu>
  </div>
  <v-snackbar v-model="snackbar.show" :color="snackbar.type" :timeout="snackbar.timeOut" variant="tonal">
    <div class="text-center">{{ snackbar.text }}</div>
  </v-snackbar>

  <JmmUnInstallDialog
    :appId="props.appMetaData.id"
    :appIcon="props.appMetaData.icon"
    :appName="props.appMetaData.name"
    :show="showUninstallDialog"
    @close="onJmmUnInstallDialogClosed"
  ></JmmUnInstallDialog>
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
  z-index: 2;
}
</style>
<style scoped lang="scss">
:scope {
  --icon-size: 60px;
}

.overlay {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  z-index: 1;
  // pointer-events: none;
  // background-color: rgba(0, 0, 0, 0.3);

  backdrop-filter: blur(40px);

  // .glass-material-overlay {
  //   backdrop-filter: blur(20px);
  //   width: 100%;
  //   height: 100%;
  // }

  &.fade-enter-from,
  &.fade-leave-to {
    opacity: 0;
  }
}
.backdrop-blur-sm {
  --tw-backdrop-blur: contrast(1.5) brightness(1.2) blur(6px);
}

.app {
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  margin-bottom: 10px;
  .app-wrap {
    display: flex;
    flex-direction: column;
    place-items: center;
    &.focused {
      scale: 1.05;
    }
    &.overlayed {
      z-index: 2;
    }
    .app-icon {
      width: 60px;
      height: 60px;
      display: grid;
      grid-template-columns: 1fr;
      grid-template-rows: 1fr;
      grid-template-areas: "view";
      place-items: center;

      .bg {
        grid-area: view;
        color: rgba(255, 255, 255, 0.2);
        border-radius: 16%; // 高斯模糊的圆角
        :deep(svg) {
          width: 100%;
          height: 100%;
          stroke: rgb(0 0 0 / 50%);
          stroke-width: 1px;
          stroke-linejoin: round;
        }
        z-index: 0;
      }
      .fg {
        z-index: 1;
        grid-area: view;
        width: 90%;
      }
    }
    .app-name {
      width: 76px;
      font-size: 14px;
      margin-top: 10px;

      color: rgb(0 0 0 / 80%);
      -webkit-text-stroke: rgb(255 255 255 / 25%);
      -webkit-text-stroke-width: 0.55px;

      text-align: center;
      word-break: break-word;
      white-space: nowrap;
      border-radius: 0.5em; // 高斯模糊的圆角
      padding: 0.1em 0.25em;
      box-sizing: content-box;
    }
  }
}

.menu {
  display: flex;
  flex-direction: row;
  font-size: 0.9em;
  color: #151515;
  background-color: rgba(255, 255, 255, 0.62);
  backdrop-filter: contrast(1.5) brightness(1.5);
  border-radius: 1.5em;
  overflow: hidden;
  margin-top: 0.5em;
  &.ios-ani {
    transition-property: transform, filter;
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

<script lang="ts" setup>
import { clickApp, deleteApp, detailApp, quitApp } from "@/api/new-tab";
import type { $AppMetaData } from "@/types/app.type";
// import { CloseWatcher } from "@dweb-browser/plaoc";
import { onLongPress } from "@vueuse/core";
import { onMounted, reactive, ref } from "vue";
const $appHtmlRefHook = ref<HTMLDivElement | null>(null);

//控制背景虚化
const show = ref(false);
const snackbar = reactive({
  show: false,
  type: "success", // success,primary,rbga,#fff
  timeOut: 2000,
  text: "退出成功！",
});

const props = defineProps({
  appMetaData: {
    type: Object as () => $AppMetaData,
    required: true,
  },
  index: {
    type: Number,
    required: true,
  },
});
const emit = defineEmits<{
  (onLongPress: "onLongPress", filter: boolean): void;
  (onLongPress: "onUninstall", index: number): void;
}>();

onMounted(() => {});

//长按事件的回调
const onLongPressCallbackHook = () => {
  show.value = true;
  emit("onLongPress", show.value);
  // const closer = new CloseWatcher();
  // closer.addEventListener("close", () => {
  //   filter.value = false;
  // });
};

onLongPress($appHtmlRefHook, onLongPressCallbackHook, {
  modifiers: { prevent: true },
});
function showUninstall() {
  snackbar.text = `确定卸载 ${props.appMetaData.short_name} 吗 ？`;
  snackbar.timeOut = 3000;
  snackbar.type = "primary";
  snackbar.show = true;
}
// 卸载app
async function uninstall() {
  snackbar.show = false;
  const response = await deleteApp(props.appMetaData.id);
  if (response.ok) {
    emit("onUninstall", props.index);
    snackbar.text = `卸载成功！`;
    snackbar.type = "success";
    snackbar.timeOut = 1000;
    snackbar.show = true;
  }
}
function showQuit() {
  quitApp(props.appMetaData.id);
  snackbar.text = `${props.appMetaData.short_name} 已退出后台。`;
  snackbar.timeOut = 1000;
  snackbar.type = "primary";
  snackbar.show = true;
}
function menuOpen() {
  show.value = false;
}
</script>
<template>
  <div ref="$appHtmlRefHook" class="app" draggable="true">
    <v-menu
      :modelValue="show"
      @update:modelValue="menuOpen"
      location="bottom"
      transition="slide-y-transition"
    >
      <template v-slot:activator="{ props }">
        <div class="app-wrap" :class="{ focused: show }" @click="show = false">
          <div
            class="app-icon"
            v-bind="props"
            @click="clickApp(appMetaData.id)"
          >
            <img class="img" :src="appMetaData.icon" />
          </div>
          <div class="app-name" v-show="!show">
            {{ appMetaData.short_name }}
          </div>
        </div>
        <Transition name="fade">
          <div class="overlay" v-show="show" @click="show = false"></div>
        </Transition>
      </template>

      <div class="menu" v-show="show">
        <div v-ripple class="item quit" @click="showQuit">
          <img class="icon" src="../../../assets/images/quit.svg" alt="退出" />
          <p class="title">退出</p>
        </div>

        <div v-ripple class="item details" @click="detailApp(appMetaData.id)">
          <img
            class="icon"
            src="../../../assets/images/details.svg"
            alt="详情"
          />
          <p class="title">详情</p>
        </div>
        <div v-ripple class="item delete" @click="showUninstall">
          <img
            class="icon"
            src="../../../assets/images/delete.svg"
            alt="卸载"
          />
          <p class="title">卸载</p>
        </div>
      </div>
    </v-menu>
  </div>
  <v-snackbar
    v-model="snackbar.show"
    :color="snackbar.type"
    :timeout="snackbar.timeOut"
    variant="outlined"
  >
    {{ snackbar.text }}
    <template v-slot:actions>
      <v-btn color="indigo" variant="text" @click="uninstall" v-show="snackbar.timeOut === 3000"> 卸载 </v-btn>
    </template>
  </v-snackbar>
</template>
<style scoped lang="scss">
:scope {
  --icon-size: 60px;
}

.focused {
  z-index: 2;
}

.overlay {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  backdrop-filter: blur(5px);
  z-index: 1;
  // pointer-events: none;
  background-color: rgba(0, 0, 0, 0.3);
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
    padding: 0.8em 0.25em;
    .app-icon {
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
    .app-name {
      font-size: 14px;
      width: 76px;
      font-weight: bold;
      color: #333;
      margin-top: 10px;
      text-align: center;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis; /* 超出部分显示省略号 */
    }
  }
}

.menu {
  display: flex;
  flex-direction: row;
  font-size: 0.9em;
  background-color: rgba(255, 255, 255, 0.805);
  // backdrop-filter: contrast(2) brightness(2);
  border-radius: 1.5em;
  overflow: hidden;
  margin-top: 0.5em;
  .item {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    font-size: 1em;
    padding: 0.8em;
    min-width: 6em;
    .icon {
      --icon-size: 1.8em;
      width: var(--icon-size);
      height: var(--icon-size);
    }
    .title {
      font-size: 1em;
      padding-top: 0.5em;
      white-space: nowrap;
      color: #151515;
    }
  }
}
</style>

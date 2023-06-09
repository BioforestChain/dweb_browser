<script lang="ts" setup>
import { clickApp } from "@/api/new-tab";
import type { $AppMetaData } from "@/types/app.type";
import { CloseWatcher } from "@dweb-browser/plaoc";
import { onLongPress } from "@vueuse/core";
import { onMounted, ref } from "vue";
const $appHtmlRefHook = ref<HTMLDivElement | null>(null);
//控制背景虚化
const filter = ref(false);

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
  (onLongPress: "onLongPress", filter: boolean, index: number): void;
}>();

onMounted(() => {});

//长按事件的回调
const onLongPressCallbackHook = () => {
  filter.value = true;
  emit("onLongPress", filter.value, props.index);
  const closer = new CloseWatcher();
  console.log("CloseWatcherxxxxxxxx",closer)
  closer.addEventListener("close", () => {
    console.log("CloseWatcherxxxxxxxx")
    filter.value = false;
  });
};

onLongPress($appHtmlRefHook, onLongPressCallbackHook, {
  modifiers: { prevent: true },
});
// 监听页面点击事件用来取消模糊
document.addEventListener("click", function (event) {
  const appsDom = $appHtmlRefHook.value;
  if (!appsDom) return;
  // 检查点击的目标元素是否是 app 元素或其子元素
  const isClickedInsideApp = appsDom.contains(event.target as Node);
  // 如果点击的目标元素不是 app 元素或其子元素，则移除模糊效果
  if (!isClickedInsideApp) {
    filter.value = false;
  }
});
</script>

<template>
  <div ref="$appHtmlRefHook" class="app" draggable="true">
    <v-tooltip activator="parent" location="start">Tooltip</v-tooltip>
    <div class="app-icon" @click="clickApp(appMetaData.id)">
      <img class="img" :src="appMetaData.icon" />
    </div>
    <div class="app-name">{{ appMetaData.short_name }}</div>
    <v-tooltip
      class="toolbar"
      activator="parent"
      location="start"
      v-model="filter"
      close-on-bac="true"
    >
      <!-- <div class="share"><img src="../../../assets/images/share.svg" alt="分享"><p>分享</p></div> -->
      <div class="quit">
        <img src="../../../assets/images/quit.svg" alt="退出" />
        <p>退出</p>
      </div>
      <div class="details">
        <img src="../../../assets/images/details.svg" alt="详情" />
        <p>详情</p>
      </div>
      <div class="delete">
        <img src="../../../assets/images/delete.svg" alt="卸载" />
        <p>卸载</p>
      </div>
    </v-tooltip>
  </div>
</template>
<style scoped>
.app {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  cursor: pointer;
  margin-bottom: 10px;
}

.app-icon {
  width: 60px;
  height: 60px;
  border-radius: 15px;
  background-color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: 0px 3px 5px rgba(0, 0, 0, 0.3);
}

.img {
  width: 90%;
  height: auto;
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

.toolbar div {
  margin: 2px 5px;
}
.toolbar div img {
  width: 20px;
  height: 20px;
}
.toolbar div p {
  font-size: 12px;
  white-space: nowrap;
  color: #151515;
}
</style>

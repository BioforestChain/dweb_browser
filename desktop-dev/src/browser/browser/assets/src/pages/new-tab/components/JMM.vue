<script lang="ts" setup>
import { clickApp } from "@/api/new-tab";
import type { $AppMetaData } from "@/types/app.type";
import { onLongPress } from "@vueuse/core";
import { onMounted, ref } from "vue";
const $appHtmlRefHook  = ref<HTMLDivElement | null>(null);
//控制背景虚化
const filter = ref(false)

const props = defineProps({
  appMetaData: {
    type: Object as () => $AppMetaData,
    required: true,
  },
  index:{
    type: Number,
    required: true,
  },
});
const emit = defineEmits<{(onLongPress: 'onLongPress', filter: boolean,index:number): void}>()

onMounted(() => {});


// function deleteApp() {}
// function shareApp() {}
//长按事件的回调
const onLongPressCallbackHook = () => {
  filter.value= true
  emit("onLongPress",filter.value,props.index)
};

onLongPress($appHtmlRefHook, onLongPressCallbackHook, {
  modifiers: { prevent: true },
});
</script>

<template>
  <div ref="$appHtmlRefHook" class="app" draggable="true" @click="clickApp(appMetaData.id)">
    <v-tooltip activator="parent" location="start">Tooltip</v-tooltip>
    <div class="app-icon">
      <img class="img" :src="appMetaData.icon" />
    </div>
    <div class="app-name">{{ appMetaData.short_name }}</div>
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
.filter {
  filter: none; /* 移除模糊效果 */
}
</style>

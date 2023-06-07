<script lang="ts" setup>
import { CONST } from '@/const';
import type { $AppMetaData } from "@/types/app.type";
import { onMounted, ref } from 'vue';
import { DragSortableList } from '../plugins/dragSortableList';
const refContainer = ref<HTMLDivElement>()
defineProps({
  appMetaData: {
      type: Object as () => $AppMetaData,
      required: true
    }
})

onMounted(() => {
  // TODO 拖动排序效果
  if(refContainer.value !== undefined){
    new DragSortableList(refContainer.value);
  }
})

function clickApp(id: string){
  const url = `${CONST.BASR_URL}/openApp?app_id=${id}`
  console.log("jmmOnclick")
  fetch(url)
}
</script>

<template>
  <div class="app" draggable="true"  ref="refContainer" @click="clickApp(appMetaData.id)">
    <div class="app-icon">
      <img class="img" :src="appMetaData.icon"/>
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
  /* margin: 0 auto; */
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

.img{
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
</style>

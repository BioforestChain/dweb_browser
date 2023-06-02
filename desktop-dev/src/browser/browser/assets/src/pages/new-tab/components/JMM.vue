<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import { DragSortableList } from '../plugins/dragSortableList';
const props = defineProps({
  src: String,
})

onMounted(() => {
  // TODO 拖动排序效果
  const container = document.querySelector(".jmm-container") as HTMLElement;
  new DragSortableList(container);
})
</script>

<template>
  <div class="app" draggable="true" >
    <div class="app-icon">
      <img class="img" :src="props.src"/>
    </div>
    <div class="app-name">JMM1</div>
  </div>
</template>
<style scoped>

.app {
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  margin: 12px;
  cursor: pointer;
}

.app-icon {
  width: 76px;
  height: 76px;
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
}
</style>

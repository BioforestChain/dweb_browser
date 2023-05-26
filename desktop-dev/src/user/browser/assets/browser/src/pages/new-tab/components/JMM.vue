<script lang="ts" setup>
import { onMounted, ref } from 'vue';
import { DragSortableList } from '../plugins/dragSortableList';


const apps = ref({});



onMounted(() => {
  // TODO 拖动排序效果
  const container = document.querySelector(".jmm-container") as HTMLElement;
  new DragSortableList(container);
})
</script>

<template>
  <div class="jmm-container">
    <div class="app" draggable="true" >
      <div class="app-icon"></div>
      <div class="app-name">JMM1</div>
    </div>
  </div>
</template>
<style scoped>
.jmm-container {
  align-items: center;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-start;
  position: absolute;
  left: 0;
  top: 0;
}

.app {
  display: flex;
  flex-direction: column;
  margin: 12px;
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
.app-name {
  font-size: 14px;
  width: 76px;
  font-weight: bold;
  color: #333;
  margin-top: 10px;
  text-align: center;
}
</style>

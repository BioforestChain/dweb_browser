<script lang="ts" setup>
import { onMounted, onUnmounted, provide, ref } from "vue";
import { gridMeshKey } from "./inject";

const props = defineProps({
  columnTemplateSize: {
    type: Number,
    default: 90,
  },
  rowTemplateSize: {
    type: Number,
    default: 120,
  },
});

const columns = ref(1);
const rows = ref(1);

provide("gridTemplateSize", props);
provide(gridMeshKey, {
  columns,
  rows,
});

const $panel = ref<HTMLDivElement>();
let resizeOb: undefined | ResizeObserver;
onMounted(() => {
  resizeOb = new ResizeObserver((entries) => {
    console.log(entries[0].contentRect);
    const { width, height } = entries[0].contentRect;
    columns.value = Math.floor(width / props.columnTemplateSize) || 1;
    rows.value = Math.floor(height / props.rowTemplateSize) || 1;
  });
  resizeOb.observe($panel.value!);
});
onUnmounted(() => {
  if (resizeOb !== undefined) {
    if ($panel.value) {
      resizeOb.unobserve($panel.value);
    }
    resizeOb.disconnect();
  }
});
</script>
<template>
  <div class="tile-panel" ref="$panel">
    <slot></slot>
  </div>
</template>

<style scoped>
.tile-panel {
  width: 100%;
  height: 100%;
  grid-area: view;
  z-index: 1;
  align-self: start;
  display: grid;
  grid-template-columns: repeat(auto-fill, calc(1px * v-bind("props.columnTemplateSize")));
  grid-template-rows: repeat(auto-fill, calc(1px * v-bind("props.rowTemplateSize")));
  justify-content: space-evenly;
  grid-auto-flow: row dense;
  align-items: center;
  justify-items: stretch;
}
</style>

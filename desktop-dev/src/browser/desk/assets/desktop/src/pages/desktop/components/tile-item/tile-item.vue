<script lang="ts" setup>
import { computed, inject } from "vue";
import { gridMeshKey } from "../tile-panel/inject.ts";
import { validatorPosition, validatorTileSize } from "./validator.ts";
const gridMesh = inject(gridMeshKey)!;
console.log("gridMesh", gridMesh);
const props = defineProps({
  x: {
    type: Number,
    validator: validatorPosition,
  },
  y: {
    type: Number,
    validator: validatorPosition,
  },
  width: {
    type: [String, Number],
    default: 1,
    validator: validatorTileSize,
  },
  height: {
    type: [String, Number],
    default: 1,
    validator: validatorTileSize,
  },
});

const gridRow = computed(() => {
  const { height } = props;
  if (typeof height === "number") {
    return height;
  }
  return Math.ceil((parseFloat(height) * gridMesh.rows.value) / 100) || 1;
});
const gridColumn = computed(() => {
  const { width } = props;
  if (typeof width === "number") {
    return width;
  }
  return Math.ceil((parseFloat(width) * gridMesh.columns.value) / 100) || 1;
});
</script>
<template>
  <div class="tile-item">
    <slot></slot>
  </div>
</template>

<style scoped lang="scss">
.tile-item {
  grid-row: span v-bind("gridRow");
  grid-column: span v-bind("gridColumn");

  height: 100%;
  width: 100%;
  // 垂直flex，模拟 block
  display: flex;
  flex-direction: column;
  // 默认水平居中
  place-items: center;
  > :deep(*) {
    flex: 1;
    width: 100%;
  }
}
</style>

<script lang="ts" setup>
import { computed, inject } from "vue";
import { gridMeshKey } from "./inject";
import { validatorTileSize } from "./validator";
const gridMesh = inject(gridMeshKey)!;
console.log("gridMesh", gridMesh);
const props = defineProps({
  row: {
    type: [String, Number],
    default: 1,
    validator: validatorTileSize,
  },
  column: {
    type: [String, Number],
    default: 1,
    validator: validatorTileSize,
  },
});

const gridRow = computed(() => {
  const { row } = props;
  if (typeof row === "number") {
    return row;
  }
  return Math.round((parseFloat(row) * gridMesh.rows.value) / 100) || 1;
});
const gridColumn = computed(() => {
  const { column } = props;
  if (typeof column === "number") {
    return column;
  }
  return Math.round((parseFloat(column) * gridMesh.columns.value) / 100) || 1;
});
</script>
<template>
  <div class="tile-item">
    <slot></slot>
  </div>
</template>

<style scoped>
.tile-item {
  grid-row: span v-bind("gridRow");
  grid-column: span v-bind("gridColumn");
}
</style>

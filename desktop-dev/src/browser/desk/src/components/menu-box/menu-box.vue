<script setup lang="ts">
import { vOnLongPress } from "@vueuse/components";
defineOptions({
  inheritAttrs: true,
});
const emit = defineEmits<{
  (event: "menu"): void;
}>();
let preDispatch = 0;
async function dispatchMenuEvent(e: Event) {
  /// 1s 内只能触发一次
  if (e.timeStamp - preDispatch < 1000) {
    return;
  }
  preDispatch = e.timeStamp;
  emit("menu");
}
</script>
<template>
  <div @contextmenu="dispatchMenuEvent" v-on-long-press.prevent="dispatchMenuEvent">
    <slot></slot>
  </div>
</template>

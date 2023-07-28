<script setup lang="ts">
import { computed, watchEffect } from "vue";
import IconSquircleBox from "../icon-squircle-box/index.vue";
import { $AppIconInfo } from "./types.ts";

const props = defineProps({
  size: {
    type: String,
    default: "3em",
  },
  icon: {
    type: Object as () => $AppIconInfo,
    required: true,
  },
});

const var_src = computed(() => (props.icon.src ? `url(${JSON.stringify(props.icon.src)})` : "none"));

watchEffect(() => {
  console.log("var_src:", var_src.value, props.icon);
});
</script>

<template>
  <div class="app-icon">
    <IconSquircleBox class="bg backdrop-ios-glass" />
    <div
      class="fg"
      :class="{
        markable: icon.markable,
        monochrome: icon.monochrome,
      }"
    ></div>
  </div>
</template>
<style scoped lang="scss">
.app-icon {
  width: v-bind("size");
  height: v-bind("size");
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: 1fr;
  grid-template-areas: "view";
  place-items: center;

  .bg {
    grid-area: view;
    color: rgba(255, 255, 255, 0.2);
    border-radius: 16%; // 高斯模糊的圆角
    :deep(svg) {
      width: 100%;
      height: 100%;
      stroke: rgb(0 0 0 / 50%);
      stroke-width: 1px;
      stroke-linejoin: round;
    }
    z-index: 0;
  }
  .fg {
    z-index: 1;
    grid-area: view;
    background-image: v-bind(var_src);
    background-size: contain;
    background-position: center;
    --size: 87%; /// Math.SQRT2 * 0.618 对角线黄金分割
    width: var(--size);
    height: var(--size);
    &.markable {
      --size: 100%;
    }
    &.monochrome {
      background-color: v-bind("icon.monocolor");
      mask-image: v-bind(var_src);
      mask-repeat: no-repeat;
      mask-position: center;
      mask-size: contain;
      background-image: none;
    }
  }
}
</style>

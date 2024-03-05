<script lang="ts" setup>
import squircle_svg_raw from "./squircle.svg?raw";
</script>
<template>
  <span class="squircle-box" v-html="squircle_svg_raw"></span>
</template>
<style scoped lang="scss">
.squircle-box {
  height: 100%;
  width: 100%;
  display: inline-block;
  > :deep(svg) {
    width: 100%;
    height: 100%;
    stroke: rgb(0 0 0 / 50%);
    stroke-width: 1px;
    stroke-linejoin: round;
  }
}
</style>

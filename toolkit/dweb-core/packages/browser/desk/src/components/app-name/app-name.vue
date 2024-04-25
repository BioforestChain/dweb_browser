<script setup lang="ts">
defineOptions({
  inheritAttrs: true,
});
</script>
<template>
  <div class="app-name backdrop-ios-glass ios-ani">
    <slot></slot>
  </div>
</template>
<style lang="scss">
.app-name {
  width: 76px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 12px;
  margin-top: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;

  color: rgb(0 0 0 / 80%);
  -webkit-text-stroke: rgb(255 255 255 / 15%);
  -webkit-text-stroke-width: 0.55px;

  text-align: center;
  text-wrap: balance;
  border-radius: 0.5em; // 高斯模糊的圆角
  padding: 0.1em 0.25em;
  box-sizing: content-box;
}
</style>

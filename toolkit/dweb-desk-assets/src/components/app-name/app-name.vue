<script setup lang="ts">
defineOptions({
  inheritAttrs: true,
});
</script>
<template>
  <div class="app-name ios-ani">
    <slot></slot>
  </div>
</template>
<style lang="scss">
.app-name {
  width: 76px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 10px;
  margin-top: 6px;
  margin-bottom: 4px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-word;

  color: #fff;
  text-shadow:
    0 0 4px rgb(0 0 0 / 60%),
    0 0 1px rgb(0 0 0);

  text-align: center;
  text-wrap: balance;
  border-radius: 0.5em; // 高斯模糊的圆角
  padding: 0.1em 0.25em;
  box-sizing: content-box;
}
</style>

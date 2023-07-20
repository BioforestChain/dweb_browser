<template>
  <router-view />
</template>

<script lang="ts" setup>
//
</script>
<style>
/* 隐藏滚动条 */
html {
  -ms-overflow-style: none;
  overflow: -moz-scrollbars-none;
}
html::-webkit-scrollbar {
  width: 0px;
}
/* 隐藏滚动条结束 */
body {
  background-color: #f2f2f2;
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
}
</style>

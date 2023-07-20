<template>
  <router-view />
</template>

<script lang="ts" setup></script>
<style>
/* 隐藏滚动条 */
html {
  -ms-overflow-style: none;
  overflow: -moz-scrollbars-none;
}
:root,
.v-application {
  background: transparent !important;
  opacity: 1;
}
html::-webkit-scrollbar {
  width: 0px;
}
/* 隐藏滚动条结束 */
.v-tooltip > .v-overlay__content {
  background: rgba(237, 236, 236, 0.7) !important;
  border-radius: 10px !important;
}
</style>

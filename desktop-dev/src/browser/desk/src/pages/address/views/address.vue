<script lang="ts" setup>
import btnGroups from '../components/btn-groups.vue';
import search from "../components/search.vue"
</script>
<template>
  <div :class="$style.container">
    <btnGroups></btnGroups>
    <search></search>
  </div>
</template>
<style module>
.container{
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  padding: 3px 16px;
  width: 100%;
  height: var(--address-height);
  border-bottom: 1px solid #eee;
  
}
</style>
<style>
  :root{
    --address-height: 38px;
  }
</style>

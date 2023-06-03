<template>
  <div :class="$style.container">
    <input :class="$style.input" type="text"/>
  </div>
</template>
<script lang="ts" setup>
</script>
<style module>
.container{
  flex-grow: 100;
  flex-shrink: 100;
  width: 100%;
  height: 100%;
  border-radius: calc(var(--address-height) / 2);
  background: #eee;
}
 
.input{
  box-sizing: border-box;
  padding: 0px 20px;
  width: 100%;
  height: 100%;
  outline: none;
  line-height: var(--address-height);
  font-size: 12px;
}
</style>

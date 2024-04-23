<template>
  <div :class="$style.container">
    <input 
      :class="$style.input" 
      type="text"
      ref="input"
      @keydown="keydown"
      value="https://www.bfmeta.info/"
    />
  </div>
</template>
<script lang="ts" setup>
import { ref } from "vue";
const input = ref<HTMLInputElement>();
async function keydown(e: KeyboardEvent){
  if(e.code === "Enter"){
    const url = `http://browser.dweb/update/content?url=${input.value?.value}`
    fetch(url)
  }
}

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

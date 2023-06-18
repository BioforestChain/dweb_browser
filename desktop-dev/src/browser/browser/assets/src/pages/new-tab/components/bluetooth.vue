<script lang="ts" setup>
import { reactive } from 'vue';

const state: {
  isOpen: boolean,
  devices: $Device[]
} = reactive({
  isOpen: false,
  devices: []
})

function btnOnClick(){
  console.log("点击了打开 or 关闭 bluetooth")
  console.log(location)

}

function deviceSelected(device: $Device){
  console.log('点击了选择', device)
}

interface $Device{
  deviceName: string;
  deviceId: string;
}
 

</script>
<template>
  <div :class="$style.container">
    <button :class="$style.btn" @click="btnOnClick">{{ state.isOpen ? "open" : "close" }} bluetooth</button>
    <ul>
      <li 
        v-for="device in state.devices" 
        :key="device.deviceId"
        @click="() => deviceSelected(device)"
      >
        {{ device.deviceName}}
      </li>
    </ul>
  </div>
</template>
<style module>
.container{
  position: absolute;
  z-index: 999;
  left: 0px; 
  top: 200px;
}

.btn{
  padding: 6px 10px;
  cursor: pointer;
  border: 1px solid #333;
  border-radius: 5px;
}
</style>

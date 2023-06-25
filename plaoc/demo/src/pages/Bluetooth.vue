<script setup lang="ts">
import { onMounted, ref, reactive } from "vue";
import { HTMLBluetoothElement } from "../plugin";
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";

export interface $Device{
  deviceId: string, 
  deviceName: string
}

const state: {
  isOpen: boolean,
  deviceList: $Device[],
  deviceConnectedId: string;
  deviceConnecingId: string;
} = reactive({
  isOpen: false,
  deviceList: [],
  deviceConnectedId: "",
  deviceConnecingId: "",
}) 
let bluetooth: HTMLBluetoothElement;
let bluetoothDevice;

const $bluetooth = ref<HTMLBluetoothElement>();
const $logPanel = ref<typeof LogPanel>();


onMounted(async () => {
  console = toConsole($logPanel);
  bluetooth = $bluetooth.value!;

  // 测试数据
  ;(() => {
    
    allDeviceUpdate([
      {deviceId: "1", deviceName: "name-1"},
      {deviceId: "2", deviceName: "name-2"},
      {deviceId: "3", deviceName: "name-3"},
    ])
     
    deviceConnectedIdUpdate("1")
    deviceConnecingIdUpdate("2")

  })();
})

async function toggleOpen(){
  if(state.isOpen){
    bluetooth.requestDeviceCancel()
  }else{
    bluetooth.requestDevice()
  }
  state.isOpen = !state.isOpen
  // bluetoothDevice = await bluetooth.toggle(state.isOpen)
  // console.log("bluetoothDevice: ", bluetoothDevice)
}

function allDeviceUpdate(list: $Device[]){
  state.deviceList = list
}

function deviceConnecingIdUpdate(deviceId: string){
  state.deviceConnecingId = deviceId;
}

function deviceConnectedIdUpdate(deviceId: string){
  state.deviceConnectedId = deviceId;
  
}

function deviceConnect(device: $Device){
  deviceConnecingIdUpdate(device.deviceId);
  console.log('点击了列表')
}


</script>
<template>
  <dweb-bluetooth ref="$bluetooth"></dweb-bluetooth>
  <div class="card glass">
    <figure class="icon">
      <div class="swap-on">🧬</div>
    </figure>
    <article class="card-body">
      <h2 class="card-title">蓝牙</h2>
      <input class="toggle" type="checkbox" id="statusbar-overlay" v-model="state.isOpen"  @click="toggleOpen"/>
    </article>
    <!-- <article class="card-body">
      <h2 class="card-title">我的蓝牙设备</h2>
      <v-list lines="one" bg-color="#ffffff11">
        <v-list-item
          v-for="item in state.deviceList"
          :key="item.deviceId"
          @click="() => deviceConnect(item)"
        >
          <template v-slot:prepend>
            <span :class="{[$style.device_name_active]: item.deviceId === state.deviceConnectedId}">{{ item.deviceName }}</span>
          </template>
          <template v-slot:append>
            <span v-if="item.deviceId !== state.deviceConnecingId && item.deviceId !== state.deviceConnectedId">未连接</span>
            <span 
              v-if="item.deviceId === state.deviceConnecingId"
              :class="$style.loading"  
            >
              <svg 
                t="1687322802748" 
                viewBox="0 0 1024 1024" 
                version="1.1" 
                xmlns="http://www.w3.org/2000/svg" 
              >
                <path 
                  d="M834.7648 736.3584a5.632 5.632 0 1 0 11.264 0 5.632 5.632 0 0 0-11.264 0z m-124.16 128.1024a11.1616 11.1616 0 1 0 22.3744 0 11.1616 11.1616 0 0 0-22.3744 0z m-167.3216 65.8944a16.7936 16.7936 0 1 0 33.6384 0 16.7936 16.7936 0 0 0-33.6384 0zM363.1616 921.6a22.3744 22.3744 0 1 0 44.7488 0 22.3744 22.3744 0 0 0-44.7488 0z m-159.744-82.0224a28.0064 28.0064 0 1 0 55.9616 0 28.0064 28.0064 0 0 0-56.0128 0zM92.672 700.16a33.6384 33.6384 0 1 0 67.2256 0 33.6384 33.6384 0 0 0-67.2256 0zM51.2 528.9984a39.168 39.168 0 1 0 78.336 0 39.168 39.168 0 0 0-78.336 0z m34.1504-170.0864a44.8 44.8 0 1 0 89.6 0 44.8 44.8 0 0 0-89.6 0zM187.904 221.7984a50.432 50.432 0 1 0 100.864 0 50.432 50.432 0 0 0-100.864 0zM338.432 143.36a55.9616 55.9616 0 1 0 111.9232 0 55.9616 55.9616 0 0 0-111.9744 0z m169.0112-4.9152a61.5936 61.5936 0 1 0 123.2384 0 61.5936 61.5936 0 0 0-123.2384 0z m154.7776 69.632a67.1744 67.1744 0 1 0 134.3488 0 67.1744 67.1744 0 0 0-134.3488 0z m110.0288 130.816a72.8064 72.8064 0 1 0 145.5616 0 72.8064 72.8064 0 0 0-145.5616 0z m43.7248 169.472a78.3872 78.3872 0 1 0 156.8256 0 78.3872 78.3872 0 0 0-156.8256 0z" 
                  fill="currentColor" 
                >
                </path>
              </svg>
            </span>
            <span 
              v-if="item.deviceId === state.deviceConnectedId"
              :class="{[$style.label_connected] : true}"
            >已连接</span>
          </template>
        </v-list-item>
      </v-list>
    </article> -->
  </div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>
<style module>

.device_name_active{
  color: #1296db;
}
.loading{
  display: block;
  width: 26px;
  height: 26px;
  animation: rotate 1s infinite;
}

@keyframes rotate {
  0% { 
    transform: rotateZ(0deg)
  }
  100% { 
    transform: rotateZ(360deg);
  }
}

.label_connected{
  color: #1296db;
}

  
</style>
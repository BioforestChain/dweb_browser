<script setup lang="ts">
import { onMounted, ref, reactive } from "vue";
import { HTMLBluetoothElement } from "../plugin";
import LogPanel, { toConsole } from "../components/LogPanel.vue";
import type {
  BluetoothRemoteGATTServer,
  BluetoothRemoteGATTService,
  BluetoothRemoteGATTCharacteristic,
  BluetoothRemoteGATTDescriptor,
} from "../../../src/client/components/bluetooth/index";

export interface $Device {
  deviceId: string;
  deviceName: string;
}

const state: {
  isOpen: boolean;
  deviceList: $Device[];
  deviceConnectedId: string;
  deviceConnecingId: string;
  bluetoothRemoteGATTServer?: BluetoothRemoteGATTServer;
  uuid: string;
  bluetoothRemoteGATTService?: BluetoothRemoteGATTService;
  bluetoothCharacteristicUUID: string;
  waritBluetoothRemoteGATTCharacteristicValue?: string;
  bluetoothRemoteGATTCharacteristic?: BluetoothRemoteGATTCharacteristic;
  bluetoothDescriptorUUID: string;
  bluetoothRemoteGATTDescriptor?: BluetoothRemoteGATTDescriptor;
  writeCharacteristicDescriptorValue?: string;
} = reactive({
  isOpen: false,
  deviceList: [],
  deviceConnectedId: "",
  deviceConnecingId: "",
  bluetoothRemoteGATTServer: undefined,
  // 测试 device HUAWEI WATCH FIT-090 提供的 uuid
  uuid: "00003802-0000-1000-8000-00805f9b34fb",
  bluetoothRemoteGATTService: undefined,
  bluetoothCharacteristicUUID: "00004a02-0000-1000-8000-00805f9b34fb",
  waritBluetoothRemoteGATTCharacteristicValue: "写入特性的值",
  bluetoothRemoteGATTCharacteristic: undefined,
  bluetoothDescriptorUUID: "00002902-0000-1000-8000-00805f9b34fb",
  bluetoothRemoteGATTDescriptor: undefined,
  writeCharacteristicDescriptorValue: "写入特征描述符的内容",
});
let bluetooth: HTMLBluetoothElement;
let bluetoothDevice;
let _bluetoothRemoteGATTServer: BluetoothRemoteGATTServer | undefined;

const $bluetooth = ref<HTMLBluetoothElement>();
const $logPanel = ref<typeof LogPanel>();

onMounted(async () => {
  // console = toConsole($logPanel);
  bluetooth = $bluetooth.value!;
});

async function toggleOpen() {
  state.isOpen ? close() : open();
}

async function open() {
  const res = await bluetooth.open();
  if (res.success) {
    console.log("open bluetooth.std.dweb success");
    state.isOpen = true;
  } else {
    console.error("open bluetooth.std.dweb fail", res.error);
  }
}

async function close() {
  const res = await bluetooth.close();
  if (res.success === true) {
    console.log("close bluetooth.std.dweb success");
    state.isOpen = false;
    state.bluetoothRemoteGATTServer = undefined;
  } else {
    console.log("close bluetooth.std.dweb fail");
  }
}

async function requestAndConnectDevice() {
  const res = await bluetooth.requestAndConnectDevice();
  if (res.success === true && res.data !== undefined) {
    state.bluetoothRemoteGATTServer = res.data;
    console.log("连接蓝牙设备 成功", state.bluetoothRemoteGATTServer);
    state.bluetoothRemoteGATTServer.device.addEventListener("gattserverdisconnected", (e: Event) => {
      console.log("gattserverdisconnected", e);
    });

    state.bluetoothRemoteGATTServer.device.addEventListener("advertisementreceived", (e: Event) => {
      console.log("advertisementreceived", e);
    });
  } else {
    state.bluetoothRemoteGATTServer = undefined;
    console.error("连接蓝牙设备失败 ", res.error);
  }
}

async function watchAdvertisements() {
  if (state.bluetoothRemoteGATTServer === undefined) {
    throw new Error(`state.bluetoothRemoteGATTServer === undefined`);
  }
  const res = await state.bluetoothRemoteGATTServer?.device.watchAdvertisements();
  if (res.success) {
    console.log("watch Advertisements success");
  } else {
    console.error("watch advertisments fail", res.error);
  }
}

async function disconnect() {
  if (state.bluetoothRemoteGATTServer === undefined) {
    console.error("state.bluetoothRemoteGATTServer === undefined");
    return;
  }
  const bluetoothRemoteGATTServer = await state.bluetoothRemoteGATTServer.disconnect();
  // 根据返回的 bluetoothRemoteGATTServer.connected 的值判断 请求是否成功
  if (bluetoothRemoteGATTServer.connected === true) {
    console.log("disconnect sucess");
  } else {
    console.log("disconnect fail");
  }
}

async function connect() {
  if (state.bluetoothRemoteGATTServer === undefined) {
    console.error("state.bluetoothRemoteGATTServer === undefined");
    return;
  }
  const bluetoothRemoteGATTServer = await state.bluetoothRemoteGATTServer.connect();
  // 根据返回的 bluetoothRemoteGATTServer.connected 的值判断 请求是否成功
  if (bluetoothRemoteGATTServer.connected === true) {
    console.log("connect sucess");
  } else {
    console.log("connect fail");
  }
}

async function getPrimaryService(uuid: string) {
  const res = await state.bluetoothRemoteGATTServer?.getPrimaryService(state.uuid);
  if (res === undefined) {
    console.error("getPrimaryService res", undefined);
    state.bluetoothRemoteGATTService = undefined;
    return;
  }

  if (res.success) {
    state.bluetoothRemoteGATTService = res.data;
    console.log("getPrimaryService res: ", res);
  } else {
    state.bluetoothRemoteGATTService = undefined;
    console.error("getPrimaryService res: ", res);
  }
}

async function getCharacteristic() {
  if (state.bluetoothRemoteGATTService === undefined) {
    console.error("state.bluetoothRemoteGATTService === undefined");
    return;
  }
  const res = await state.bluetoothRemoteGATTService?.getCharacteristic(state.bluetoothCharacteristicUUID);
  if (res.success === true) {
    state.bluetoothRemoteGATTCharacteristic = res.data;
    state.bluetoothRemoteGATTCharacteristic?.addEventListener("characteristicvaluechanged", (arg: unknown) => {
      console.log("characteristicvaluechanged", arg);
    });
    console.log("getCharacteristic success ", res);
  } else {
    console.log("getCharacteristic fail: ", res);
  }
}

async function readCharacteristicValue() {
  if (state.bluetoothRemoteGATTCharacteristic === undefined) {
    console.error(`state.bluetoothRemoteGATTCharacteristic === undefined`);
    return;
  }
  const res = await state.bluetoothRemoteGATTCharacteristic.readValue();
  if (res.success) {
    console.log("获取特征的值 成功", res.data);
  } else {
    console.error("获取特征的值失败", res.error);
  }
}

async function waritBluetoothRemoteGATTCharacteristicValue() {
  if (state.bluetoothRemoteGATTCharacteristic === undefined) {
    console.error(`state.bluetoothRemoteGATTCharacteristic === undefined`);
    return;
  }
  const res = await state.bluetoothRemoteGATTCharacteristic.writeValue(
    new TextEncoder().encode(state.writeCharacteristicDescriptorValue)
  );
  if (res.success) {
    console.log("写入特征的值 成功", res.data);
  } else {
    console.error("写入特征的值失败", res.error);
  }
}

async function readCharacteristicDescriptor() {
  if (state.bluetoothRemoteGATTCharacteristic === undefined) {
    console.error(`state.bluetoothRemoteGATTCharacteristic === undefined`);
    return;
  }
  // 从这里开始 设置 state
  const res = await state.bluetoothRemoteGATTCharacteristic.getDescriptor(state.bluetoothDescriptorUUID);
  if (res.success) {
    state.bluetoothRemoteGATTDescriptor = res.data;
    console.log("readCharacteristicDescriptor success ", res.data);
  } else {
    state.bluetoothRemoteGATTDescriptor = undefined;
    console.error("readCharacteristicDescriptor fail ", res.error);
  }
}

async function readCharacteristicDescriptorValue() {
  if (state.bluetoothRemoteGATTDescriptor === undefined) {
    console.error(`state.bluetoothRemoteGATTDescriptor === undefined`);
    return;
  }
  const res = await state.bluetoothRemoteGATTDescriptor?.readValue();
  if (res.success) {
    console.log("readCharacteristicDescriptorValue success", res.data);
  } else {
    console.error(`readCharacteristicDescriptorValue fail`, res.data);
  }
}

async function writeCharacteristicDescriptorValue() {
  if (state.bluetoothRemoteGATTDescriptor === undefined) {
    console.error(`state.bluetoothRemoteGATTDescriptor === undefined`);
    return;
  }
  const res = await state.bluetoothRemoteGATTDescriptor?.writeValue(
    new TextEncoder().encode(state.writeCharacteristicDescriptorValue)
  );

  if (res.success) {
    console.log("writeCharacteristicDescriptorValue success", res);
  } else {
    console.error("writeCharacteristicDescriptorValue fail", res);
  }
}

function allDeviceUpdate(list: $Device[]) {
  state.deviceList = list;
}

function deviceConnecingIdUpdate(deviceId: string) {
  state.deviceConnecingId = deviceId;
}

function deviceConnectedIdUpdate(deviceId: string) {
  state.deviceConnectedId = deviceId;
}

// function deviceConnect(device: $Device){
//   deviceConnecingIdUpdate(device.deviceId);
//   console.log('点击了列表')
// }
</script>
<template>
  <dweb-bluetooth ref="$bluetooth"></dweb-bluetooth>
  <div class="card glass">
    <figure class="icon">
      <div class="swap-on">🧬</div>
    </figure>
    <article class="card-body">
      <h2 class="card-title">蓝牙</h2>
      <input class="toggle" type="checkbox" id="statusbar-overlay" v-model="state.isOpen" @click="toggleOpen" />
    </article>
    <article class="card-body" v-if="state.isOpen">
      <!-- <h2 class="card-title">查询设备</h2> -->
      <v-btn color="indigo-darken-3" @click="requestAndConnectDevice">查询连接蓝牙设备 </v-btn>
    </article>

    <article
      class="card-body"
      v-if="state.bluetoothRemoteGATTServer !== undefined && state.bluetoothRemoteGATTServer.connected"
    >
      <!-- <h2 class="card-title">查询设备</h2> -->
      <v-btn color="indigo-darken-3" @click="watchAdvertisements">watch advertisements </v-btn>
    </article>

    <article
      class="card-body"
      v-if="state.bluetoothRemoteGATTServer !== undefined && state.bluetoothRemoteGATTServer.connected"
    >
      <h2 class="card-title">断开 {{ state.bluetoothRemoteGATTServer.device.name }} 蓝牙</h2>
      <v-btn color="indigo-darken-3" @click="disconnect">disconnect </v-btn>
    </article>
    <article
      class="card-body"
      v-if="state.bluetoothRemoteGATTServer !== undefined && !state.bluetoothRemoteGATTServer.connected"
    >
      <h2 class="card-title">连接 {{ state.bluetoothRemoteGATTServer.device.name }} 蓝牙</h2>
      <v-btn color="indigo-darken-3" @click="connect">connect </v-btn>
    </article>

    <article
      class="card-body"
      v-if="state.bluetoothRemoteGATTServer !== undefined && state.bluetoothRemoteGATTServer.connected"
    >
      <h2 class="card-title">获取主服务</h2>
      <v-input v-model="state.uuid">{{ state.uuid }}</v-input>
      <v-btn color="indigo-darken-3" @click="getPrimaryService">getPrimaryService </v-btn>
    </article>
    <article class="card-body" v-if="state.bluetoothRemoteGATTService !== undefined">
      <h2 class="card-title">获取特性</h2>
      <v-input v-model="state.uuid">{{ state.bluetoothCharacteristicUUID }}</v-input>
      <v-btn color="indigo-darken-3" @click="getCharacteristic">getCharacteristic </v-btn>
    </article>
    <article class="card-body" v-if="state.bluetoothRemoteGATTCharacteristic !== undefined">
      <h2 class="card-title">读取特性的值</h2>
      <v-btn color="indigo-darken-3" @click="readCharacteristicValue">read Characteristic Value </v-btn>
    </article>
    <article class="card-body" v-if="state.bluetoothRemoteGATTCharacteristic !== undefined">
      <h2 class="card-title">写入特性的值</h2>

      <v-text-field label="属性描述符" v-model="state.waritBluetoothRemoteGATTCharacteristicValue"></v-text-field>
      <v-btn color="indigo-darken-3" @click="waritBluetoothRemoteGATTCharacteristicValue"
        >write characteristic Value
      </v-btn>
    </article>
    <article class="card-body" v-if="state.bluetoothRemoteGATTCharacteristic !== undefined">
      <h2 class="card-title">获取特性的描述</h2>
      <v-input v-model="state.bluetoothDescriptorUUID">{{ state.bluetoothCharacteristicUUID }}</v-input>
      <v-btn color="indigo-darken-3" @click="readCharacteristicDescriptor">getDescriptor </v-btn>
    </article>

    <article class="card-body" v-if="state.bluetoothRemoteGATTDescriptor !== undefined">
      <h2 class="card-title">读取特性描述述符的值</h2>
      <v-btn color="indigo-darken-3" @click="readCharacteristicDescriptorValue">read Descriptor Value </v-btn>
    </article>
    <article class="card-body" v-if="state.bluetoothRemoteGATTDescriptor !== undefined">
      <h2 class="card-title">写入特性描述符</h2>
      <!-- <v-input v-model="state.writeCharacteristicDescriptorValue"></v-input> -->
      <v-text-field label="属性描述符" v-model="state.writeCharacteristicDescriptorValue"></v-text-field>
      <v-btn color="indigo-darken-3" @click="writeCharacteristicDescriptorValue">write Descriptor Value </v-btn>
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
.device_name_active {
  color: #1296db;
}
.loading {
  display: block;
  width: 26px;
  height: 26px;
  animation: rotate 1s infinite;
}

@keyframes rotate {
  0% {
    transform: rotateZ(0deg);
  }
  100% {
    transform: rotateZ(360deg);
  }
}

.label_connected {
  color: #1296db;
}
</style>

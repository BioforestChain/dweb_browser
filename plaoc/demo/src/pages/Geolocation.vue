<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";
import type { $GeolocationContoller, HTMLGeolocationElement } from "../plugin";
import { geolocationPlugin } from "../plugin";

const $geolocationElement = ref<HTMLGeolocationElement>();
const $logPanel = ref<typeof LogPanel>();

let console: Console;
let geolocation: HTMLGeolocationElement;

onMounted(async () => {
  geolocation = $geolocationElement.value!;
  console = toConsole($logPanel);
});

async function getLocation() {
  const res = await geolocation.getLocation();
  console.log("Location", res);
}
let contoller: $GeolocationContoller;
// 创建控制器
async function createLocation() {
  contoller = await geolocationPlugin.createLocation();
  onLocation();
}
function stop() {
  contoller.stop();
}
function onLocation() {
  contoller.listen((res) => {
    console.log("location", res.state.message);
    const coords = res.coords;
    console.log(`经度：${coords.longitude}纬度：${coords.latitude}海拔：${coords.altitude}`);
  });
}
</script>
<template>
  <dweb-geolocation ref="$geolocationElement"></dweb-geolocation>
  <div class="card glass">
    <figure class="icon">
      <div class="swap-on">🧬</div>
    </figure>
    <article class="card-body">
      <h2 class="card-title">Location</h2>
      <v-btn color="indigo-darken-3" @click="getLocation">单次获取地理位置</v-btn>
    </article>
    <article class="card-body">
      <h2 class="card-title">Location channel</h2>
      <v-btn color="indigo-darken-3" @click="createLocation">创建监听</v-btn>
      <v-btn color="indigo-darken-3" @click="stop">停止监听</v-btn>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

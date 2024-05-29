<script setup lang="ts">
import type { $GeolocationController, HTMLGeolocationElement } from "@plaoc/plugins";
import { geolocationPlugin } from "@plaoc/plugins";
import { onMounted, ref } from "vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";

const $geolocationElement = ref<HTMLGeolocationElement>();
const $logPanel = ref<typeof LogPanel>();

let console: Console;
let geolocation: HTMLGeolocationElement;
let controller: $GeolocationController | undefined;

onMounted(async () => {
  geolocation = $geolocationElement.value!;
  console = toConsole($logPanel);
});

// 获取一次
async function getLocation() {
  const res = await geolocation.getLocation();
  console.log("Location", res);
}

// 创建控制器
async function createLocation() {
  if (controller !== undefined) {
    console.info("already listening.");
  } else {
    controller = await geolocationPlugin.createLocation();
    controller.listen((res) => {
      console.log("location", res.state.message);
      const coords = res.coords;
      console.log(`经度：${coords.longitude}纬度：${coords.latitude}海拔：${coords.altitude}`);
    });
  }
}
function stop() {
  if (controller !== undefined) {
    controller.stop();
    controller = undefined;
    console.log("location stoped");
  }
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

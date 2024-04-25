<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { toConsole, } from "../components/LogPanel.vue";
import type { HTMLDeviceElement } from "../plugin";
import { isDweb } from "../plugin";


const title = "device";
const $deviceElement = ref<HTMLDeviceElement>();
const $logPanel = ref<typeof LogPanel>();

let console: Console;
let device: HTMLDeviceElement;

onMounted(async () => {
  device = $deviceElement.value!;
  console = toConsole($logPanel);
});

async function getUUID() {
  const res = await device.getUUID();
  console.log("uuid", res.uuid);
}

function isDwebBrowser() {
  console.log("isDwebBrowser=>",isDweb())
}

</script>
<template>
  <dweb-device ref="$deviceElement"></dweb-device>
  <div class="card glass">
    <figure class="icon">
      <div class="swap-on">🧬</div>
    </figure>
    <article class="card-body">
      <h2 class="card-title">UUID</h2>
      <v-btn color="indigo-darken-3" @click="getUUID">查询 UUID</v-btn>
    </article>
    <article class="card-body">
      <h2 class="card-title">是否是dweb</h2>
      <v-btn color="indigo-darken-3" @click="isDwebBrowser">是否在dweb环境下</v-btn>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

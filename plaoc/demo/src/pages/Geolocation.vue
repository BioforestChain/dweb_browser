<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";
import type { HTMLGeolocationElement } from "../plugin";

const title = "Geolocation";
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
</script>
<template>
  <dweb-geolocation ref="$geolocationElement"></dweb-geolocation>
  <div class="card glass">
    <figure class="icon">
      <div class="swap-on">🧬</div>
    </figure>
    <article class="card-body">
      <h2 class="card-title">Location</h2>
      <v-btn color="indigo-darken-3" @click="getLocation">获取地理位置</v-btn>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

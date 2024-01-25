<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";

const $logPanel = ref<typeof LogPanel>();
let console: Console;

onMounted(async () => {
  console = toConsole($logPanel);
});
// 获取图片位置
</script>

<template>
  <dweb-biometrics ref="$biometricsPlugin"></dweb-biometrics>
  <div class="card glass">
    <figure class="icon">
      <div class="swap-on">📸</div>
    </figure>
    <article class="card-body">
      <h2 class="card-title">获取图片</h2>
      <!-- <FieldLabel label="cameraSource:">
        <select name="camera-source" id="camera-source" v-model="">
          <option value="PROMPT">PROMPT</option>
          <option value="CAMERA">CAMERA</option>
          <option value="PHOTOS">PHOTOS</option>
        </select>
      </FieldLabel>
      <FieldLabel label="cameraResultType:">
        <select name="camera-result-type" id="ccamera-result-type" v-model="cameraResultType">
          <option value="Uri">Uri</option>
          <option value="Base64">Base64</option>
        </select>
      </FieldLabel>
      <div>
        <div class="text-caption">压缩率</div>
        <v-slider v-model="quality" thumb-label="always"></v-slider>
      </div>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="getPhoto">getPhoto</button>
      </div>
      <img :src="webPathImage" /> -->
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

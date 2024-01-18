<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
import { CameraResultType, CameraSource, cameraPlugin } from "../plugin";
const $logPanel = ref<typeof LogPanel>();
let console: Console;

onMounted(async () => {
  console = toConsole($logPanel);
});
// 获取图片位置
const cameraSource = ref<CameraSource>(CameraSource.Prompt);
//返回结果
const cameraResultType = ref<CameraResultType>(CameraResultType.Base64);
// 压缩率
const quality = ref<number>(0);
// webPath结果
const webPathImage = ref<string | undefined>();
// plugin调用方法
const getPhoto = defineLogAction(
  async () => {
    console.log("输入：", cameraSource.value, cameraResultType.value, quality.value);
    const result = await cameraPlugin.getPhoto({
      source: cameraSource.value,
      resultType: cameraResultType.value,
      quality: quality.value,
    });
    console.log("base64String:", result.base64String);
    console.log("format:", result.format);
    console.log("path:", result.path);
    console.log("saved:", result.saved);
    webPathImage.value = result.base64String;
    return result;
  },
  { name: "getPhoto", args: [], logPanel: $logPanel }
);
</script>

<template>
  <dweb-biometrics ref="$biometricsPlugin"></dweb-biometrics>
  <div class="card glass">
    <figure class="icon">
      <div class="swap-on">📸</div>
    </figure>
    <article class="card-body">
      <h2 class="card-title">获取图片</h2>
      <FieldLabel label="cameraSource:">
        <select name="camera-source" id="camera-source" v-model="cameraSource">
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
      <img :src="webPathImage" />
    </article>
    <!-- <article class="card-body">
      <h2 class="card-title">生物识别</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="fingerprint">fingerprint</button>
      </div>
    </article> -->
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

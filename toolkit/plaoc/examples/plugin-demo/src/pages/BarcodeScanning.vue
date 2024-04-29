<script setup lang="ts">
import { Ref, onMounted, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";
import { HTMLDwebBarcodeScanningElement, barcodeScannerPlugin } from "../plugin";

const title = "Scanner";

const $logPanel = ref<typeof LogPanel>();
const $scannerComponent = ref<HTMLDwebBarcodeScanningElement>();

let console: Console;
let barcodeScanner: HTMLDwebBarcodeScanningElement;
onMounted(async () => {
  console = toConsole($logPanel);
  barcodeScanner = $scannerComponent.value!;
});

const imgFile: Ref<File | undefined> = ref();
// 文件选择
const onFileChanged = async ($event: Event) => {
  const target = $event.target as HTMLInputElement;
  if (target && target.files?.[0]) {
    const img = target.files[0];
    console.info("选中图片，请点击下面的扫描 ==> ", img.name, img.type, img.size);
    imgFile.value = img;
  }
};

const postScanner = async () => {
  if (imgFile.value == undefined) {
    alert("请先选择图片再识别");
    return;
  }
  console.log("图片识别结果=>", await barcodeScannerPlugin.process(imgFile.value));
};

//  components 组件
const stopScanning = async () => {
  barcodeScanner.stopScanning();
};

const camaraScanner = async () => {
  console.log("相机扫描结果=>", await barcodeScanner.startScanning());
};
</script>

<template>
  <dweb-barcode-scanning ref="$scannerComponent"></dweb-barcode-scanning>
  <div class="card glass">
    <figure class="icon">
      <h2>{{ title }}</h2>
    </figure>
    <article class="card-body">
      <h2 class="card-title">Scanner</h2>
      <FieldLabel label="选择文件扫码">
        <input type="file" @change="onFileChanged($event)" accept="image/*" />
      </FieldLabel>
      <button class="inline-block rounded-full btn btn-accent" @click="postScanner">scanner</button>
    </article>

    <article class="card-body">
      <h2 class="card-title">扫描模式</h2>
      <div class="justify-end card-actions">
        <button class="inline-block rounded-full btn btn-accent" @click="camaraScanner">摄像头识别</button>
        <button class="inline-block rounded-full btn btn-accent" @click="stopScanning">stop</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

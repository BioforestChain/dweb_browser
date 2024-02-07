<script setup lang="ts">
import { onMounted, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
import { HTMLDwebBarcodeScanningElement, ScannerProcesser, barcodeScannerPlugin } from "../plugin";

const title = "Scanner";

const $logPanel = ref<typeof LogPanel>();
const $barcodeScannerPlugin = ref<HTMLDwebBarcodeScanningElement>();

let console: Console;
let scanner = barcodeScannerPlugin;
let barcodeScanner: HTMLDwebBarcodeScanningElement;
let scannerServer: ScannerProcesser;
onMounted(async () => {
  console = toConsole($logPanel);
  barcodeScanner = $barcodeScannerPlugin.value!;
  scannerServer = await scanner.createProcesser();
});

const result = ref();

const onFileChanged = defineLogAction(
  async ($event: Event) => {
    const target = $event.target as HTMLInputElement;
    if (target && target.files?.[0]) {
      const img = target.files[0];
      console.info("photo ==> ", img.name, img.type, img.size);
      const res = await scannerServer.process(img);
      res.forEach((value) => console.log(value.data));
      result.value = res.length;
    }
  },
  { name: "process", args: [result], logPanel: $logPanel }
);

const onStop = defineLogAction(
  async () => {
    barcodeScanner.stopScanning();
  },
  { name: "onStop", args: [], logPanel: $logPanel }
);

const takePhoto = defineLogAction(
  async () => {
    result.value = await barcodeScanner.startScanning();
  },
  { name: "takePhoto", args: [result], logPanel: $logPanel }
);
</script>

<template>
  <dweb-barcode-scanning ref="$barcodeScannerPlugin"></dweb-barcode-scanning>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/vibrate.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Scanner</h2>
      <FieldLabel label="Vibrate Pattern:">
        <input type="file" @change="onFileChanged($event)" accept="image/*" capture />
      </FieldLabel>
      <button class="inline-block rounded-full btn btn-accent" @click="takePhoto">scanner</button>
      <button class="inline-block rounded-full btn btn-accent" @click="onStop">stop</button>
      <!-- <button class="inline-block rounded-full btn btn-accent" @click="getSupportedformats">getSupportedFormats</button> -->
    </article>

    <!-- <article class="card-body">
      <h2 class="card-title">get Photo</h2>
      <select class="w-full max-w-xs select" v-model="cameraSource">
        <option value="PROMPT">PROMPT</option>
        <option value="CAMERA">CAMERA</option>
        <option value="PHOTOS">PHOTOS</option>
      </select>
      <div class="justify-end card-actions">
        <button class="inline-block rounded-full btn btn-accent" @click="getPhoto">getPhoto</button>
      </div>
    </article> -->
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

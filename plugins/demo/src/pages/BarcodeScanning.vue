<script setup lang="ts">
import { onMounted, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import { barcodeScannerPlugin, HTMLDwebBarcodeScanningElement } from '@bfex/plugin';
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";
import { CameraSource } from "@bfex/plugin/types/components/camera/camera.type";

const title = "Scanner";

const $logPanel = ref<typeof LogPanel>();
const $barcodeScannerPlugin = ref<HTMLDwebBarcodeScanningElement>();

let console: Console;
let scanner = barcodeScannerPlugin;
let barcodeScanner: HTMLDwebBarcodeScanningElement;
onMounted(() => {
  console = toConsole($logPanel);
  barcodeScanner = $barcodeScannerPlugin.value!;
});

const result = ref()

const onFileChanged = defineLogAction(async ($event: Event) => {
  const target = $event.target as HTMLInputElement;
  if (target && target.files?.[0]) {
    const img = target.files[0]
    console.info("photo ==> ", img.name, img.type, img.size)
    result.value = await scanner.process(img).then(res => res.text())
  }
}, { name: "process", args: [result], logPanel: $logPanel })

const onStop = defineLogAction(async () => {
  await scanner.stop()
}, { name: "onStop", args: [], logPanel: $logPanel })

const taskPhoto = defineLogAction(async () => {
  result.value = await barcodeScanner.startScanning()
}, { name: "taskPhoto", args: [result], logPanel: $logPanel })

const cameraSource = ref<CameraSource>("PHOTOS" as never)

const getPhoto = defineLogAction(async () => {
  result.value = await barcodeScanner.getPhoto({ source: cameraSource.value })
}, { name: "getPhoto", args: [result], logPanel: $logPanel })

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
        <input type="file" @change="onFileChanged($event)" accept="image/*" capture>
      </FieldLabel>
      <button class="inline-block rounded-full btn btn-accent" @click="taskPhoto">scanner</button>
      <button class="inline-block rounded-full btn btn-accent" @click="onStop">stop</button>
    </article>

    <article class="card-body">
      <h2 class="card-title">get Photo</h2>
      <select class="w-full max-w-xs select" v-model="cameraSource">
        <option value="PROMPT">PROMPT</option>
        <option value="CAMERA">CAMERA</option>
        <option value="PHOTOS">PHOTOS</option>
      </select>
      <div class="justify-end card-actions">
        <button class="inline-block rounded-full btn btn-accent" @click="getPhoto">getPhoto</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

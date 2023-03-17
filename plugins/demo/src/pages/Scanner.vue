<script setup lang="ts">
import { onMounted, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import { barcodeScannerPlugin, HTMLDwebBarcodeScanningElement } from '@bfex/plugin';
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";

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


const onFileChanged = defineLogAction(async ($event: Event) => {
  const target = $event.target as HTMLInputElement;
  if (target && target.files?.[0]) {
    const img = target.files[0]
    console.info("photo ==> ", img.name, img.type, img.size)
    const result = await scanner.process(img).then(res => res.text())
    console.info("photo process", result)
  }
}, { name: "process", args: [], logPanel: $logPanel })

const onStop = async () => {
  await scanner.stop()
}

const taskPhoto = async () => {
  const result = await barcodeScanner.startScan()
  console.info("taskPhoto:", result)
}

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
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

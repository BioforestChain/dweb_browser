<script setup lang="ts">
import { onMounted, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import { barcodeScannerPlugin } from "@bfex/plugin"
import LogPanel, { toConsole } from "../components/LogPanel.vue";

const title = "Scanner";

const $logPanel = ref<typeof LogPanel>();

let console: Console;
let scanner = barcodeScannerPlugin;
onMounted(() => {
  console = toConsole($logPanel);
});


const onFileChanged = async ($event: Event) => {
  const target = $event.target as HTMLInputElement;
  if (target && target.files) {
    const img = target.files[0]
    console.info("photo ==> ", img.name, img.type, img.size)
    const result = await scanner.process(img).then(res => res.text())
    console.info("photo process", result)
  }
}

const onStop = async () => {
  await scanner.stop()
}
</script>

<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/vibrate.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Scanner</h2>
      <FieldLabel label="Vibrate Pattern:">
        <input type="file" @change="onFileChanged($event)" accept="image/*" capture>
      </FieldLabel>
      <button class="inline-block rounded-full btn btn-accent">process</button>
      <button class="inline-block rounded-full btn btn-accent" @click="onStop">stop</button>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

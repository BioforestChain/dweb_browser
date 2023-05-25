<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { biometricsPlugin, HTMLDwebBiometricsElement } from "../plugin"
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";
const $logPanel = ref<typeof LogPanel>();
let console: Console;

const $biometricsPlugin = ref<HTMLDwebBiometricsElement>();

let biometrics: HTMLDwebBiometricsElement;

onMounted(async () => {
  console = toConsole($logPanel);
  biometrics = $biometricsPlugin.value!;
})

// plugin调用方法
const fingerprint = defineLogAction(async () => {
  return await biometricsPlugin.biometrics()
}, { name: "fingerprint", args: [], logPanel: $logPanel })

const check = defineLogAction(async () => {
  return await biometricsPlugin.check()
}, { name: "check", args: [], logPanel: $logPanel })

// webComponent 的调用方法
const fingerprintWb = defineLogAction(async () => {
  return await biometrics.biometrics()
}, { name: "fingerprint", args: [], logPanel: $logPanel })

const checkWb = defineLogAction(async () => {
  return await biometrics.check()
}, { name: "check", args: [], logPanel: $logPanel })

const title = "BiometricsManager";
</script>

<template>
  <dweb-biometrics ref="$biometricsPlugin"></dweb-biometrics>
  <div class="card glass">
    <figure class="icon">
      <div class="swap-on">🧬</div>
    </figure>
    <article class="card-body">
      <h2 class="card-title">检测设备是否可以生物识别</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="check">check</button>
      </div>
    </article>
    <article class="card-body">
      <h2 class="card-title">生物识别</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="fingerprint">fingerprint</button>
      </div>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>


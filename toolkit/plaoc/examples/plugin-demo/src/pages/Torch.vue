<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";
import type { HTMLDwebTorchElement } from "../plugin";
const title = "Toast";

const $logPanel = ref<typeof LogPanel>();
const $torchPlugin = ref<HTMLDwebTorchElement>();

let console: Console;
let toast: HTMLDwebTorchElement;
onMounted(() => {
  console = toConsole($logPanel);
  toast = $torchPlugin.value!;
});


const toggleTorch = defineLogAction(
  async () => {
    return toast.toggleTorch();
  },
  { name: "toggleTorch", args: [], logPanel: $logPanel }
);

const getState = defineLogAction(
  async () => {
    const result = await toast.getTorchState();
    console.info("torch state", result)
  },
  { name: "getState", args: [], logPanel: $logPanel }
);

</script>
<template>
  <dweb-torch ref="$torchPlugin"></dweb-torch>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/toast.svg" :alt="title" />
    </figure>

    <article class="card-body">
      <h2 class="card-title">Torch</h2>

      <div class="justify-end card-actions">
        <button class="inline-block rounded-full btn btn-accent" @click="toggleTorch">toggle</button>
        <button class="inline-block rounded-full btn btn-accent" @click="getState">state</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

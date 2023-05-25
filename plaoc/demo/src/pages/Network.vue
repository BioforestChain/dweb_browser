<script setup lang="ts">
import { onMounted, ref } from "vue";
import { networkPlugin, HTMLDwebNetworkElement } from '../plugin';
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";

const title = "Scanner";

const $logPanel = ref<typeof LogPanel>();
const $networkPlugin = ref<HTMLDwebNetworkElement>();

let console: Console;
let network: HTMLDwebNetworkElement;
onMounted(() => {
  console = toConsole($logPanel);
  network = $networkPlugin.value!;
});

const getStatus = defineLogAction(async () => {
  const result = await network.getStatus()
  console.log("getStatus=> ", result)
}, { name: "getStatus", args: [], logPanel: $logPanel })

const onLine = defineLogAction(async () => {
  const result = await networkPlugin.onLine()
  console.log("onLine=> ", result)
}, { name: "onLine", args: [], logPanel: $logPanel })


</script>

<template>
  <dweb-network ref="$networkPlugin"></dweb-network>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/vibrate.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">查看网络是否在线</h2>
      <button class="inline-block rounded-full btn btn-accent" @click="onLine">onLine</button>
    </article>
    <article class="card-body">
      <h2 class="card-title">查看网络状态</h2>
      <button class="inline-block rounded-full btn btn-accent" @click="getStatus">getStatus(android only)</button>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

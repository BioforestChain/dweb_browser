<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { dwebServiceWorker as sw } from "@bfex/plugin"
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";
const $logPanel = ref<typeof LogPanel>();


let console: Console;



onMounted(async () => {
  console = toConsole($logPanel);
  sw.addEventListener("updatefound", (event) => {
    console.log("Dweb Service Worker update found!", event);
  })

  sw.addEventListener("fetch", (event) => {
    console.log("Dweb Service Worker fetch!", event);
  })

  sw.addEventListener("onFetch", (event) => {
    console.log("Dweb Service Worker onFetch!", event);
  })


  const updateContoller = sw.update

  updateContoller.addEventListener("start", (event) => {
    console.log("Dweb Service Worker updateContoller start =>", event);
  })
  updateContoller.addEventListener("end", (event) => {
    console.log("Dweb Service Worker updateContoller end =>", event);
  })
  updateContoller.addEventListener("progress", (event) => {
    console.log("Dweb Service Worker updateContoller progress =>", event);
  })
  updateContoller.addEventListener("cancel", (event) => {
    console.log("Dweb Service Worker updateContoller cancel =>", event);
  })

})


const close = defineLogAction(async () => {
  return await sw.close()
}, { name: "close", args: [], logPanel: $logPanel })

const restart = defineLogAction(async () => {
  return await sw.restart()
}, { name: "restart", args: [], logPanel: $logPanel })


const pause = defineLogAction(async () => {
  return await sw.updateContoller.pause()
}, { name: "pause", args: [], logPanel: $logPanel })

const resume = defineLogAction(async () => {
  return await sw.updateContoller.resume()
}, { name: "resume", args: [], logPanel: $logPanel })

const cancel = defineLogAction(async () => {
  return await sw.updateContoller.cancel()
}, { name: "cancel", args: [], logPanel: $logPanel })


const title = "Dweb Service Worker";
</script>

<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/splashscreen.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Dweb Service Worker close/restart</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="close">close</button>
        <button class="inline-block rounded-full btn btn-accent" @click="restart">restart</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">update Contoller 暂停/重下/取消</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="pause">pause</button>
        <button class="inline-block rounded-full btn btn-accent" @click="resume">resume</button>
        <button class="inline-block rounded-full btn btn-accent" @click="cancel">cancel</button>
      </div>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>


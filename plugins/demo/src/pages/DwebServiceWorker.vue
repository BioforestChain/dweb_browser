<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { dwebServiceWorker as sw } from "@bfex/plugin"
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";
const $logPanel = ref<typeof LogPanel>();
let console: Console;

const progress = ref(0)

onMounted(async () => {
  console = toConsole($logPanel);
  sw.addEventListener("updatefound", (event) => {
    console.log("Dweb Service Worker update found!", event);
  })

  sw.addEventListener("fetch", async (event) => {
    console.log("Dweb Service Worker fetch!", event.clientId);
    const response = await fetch(event.request)
    console.log("Dweb Service Worker fetch response=>", response)
    return event.respondWith(response)
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
  updateContoller.addEventListener("progress", (progressRate) => {
    progress.value = parseFloat(progressRate)
    console.log("Dweb Service Worker updateContoller progress =>", progressRate, parseFloat(progressRate));
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

const download = defineLogAction(async () => {
  return await sw.updateContoller.download("https://shop.plaoc.com/bfs-metadata.json")
}, { name: "cancel", args: [], logPanel: $logPanel })

const title = "Dweb Service Worker";
</script>

<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/splashscreen.svg" :alt="title" />
    </figure>

    <article class="card-body">
      <h2 class="card-title">下载测试</h2>
      <div class="justify-end card-actions">
        <button class="inline-block rounded-full btn btn-accent" @click="download">下载新版本</button>
      </div>
      <div>
        <progress class="w-56 progress progress-accent" :value="progress" max="100"></progress>
        <div class="stat">
          <div class="stat-figure text-secondary">
            <div class="avatar online">
              <div class="w-16 rounded-full">
                <img src="https://www.bfmeta.org/imgs/logo_1000.webp" />
              </div>
            </div>
          </div>
          <div class="stat-value">{{ progress }}%</div>
          <div class="stat-title">download Task runing</div>
          <div class="stat-desc text-secondary">{{ 100 - progress }} tasks remaining</div>
        </div>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">APP 关闭、重启</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="close">close</button>
        <button class="inline-block rounded-full btn btn-accent" @click="restart">restart</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">下载控制器： 暂停/重下/取消</h2>
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


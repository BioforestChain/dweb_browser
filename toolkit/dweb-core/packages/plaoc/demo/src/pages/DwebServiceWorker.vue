<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
import * as plaoc from "../plugin";
import { dwebServiceWorker, updateControllerPlugin } from "../plugin";

const $logPanel = ref<typeof LogPanel>();
let console: Console;

Object.assign(globalThis, { dwebServiceWorker, plaoc });

// const progress = ref(0);

onMounted(async () => {
  console = toConsole($logPanel);
  // app暂停触发事件（这个时候后台还会运行，前端界面被关闭）
  dwebServiceWorker.addEventListener("pause", (event) => {
    console.log("app pause", event);
  });
  // app恢复触发事件
  dwebServiceWorker.addEventListener("resume", (event) => {
    console.log("app resume", event);
  });
  dwebServiceWorker.addEventListener("shortcut", (event) => {
    console.log("shortcut", event.data);
    plaoc.toastPlugin.show({ text: event.data });
  });
});

const close = defineLogAction(
  async () => {
    return await dwebServiceWorker.close();
  },
  { name: "close", args: [], logPanel: $logPanel }
);

const restart = defineLogAction(
  async () => {
    return await dwebServiceWorker.restart();
  },
  { name: "restart", args: [], logPanel: $logPanel }
);

const pause = defineLogAction(
  async () => {
    // return await updateControllerPlugin.pause();
  },
  { name: "pause", args: [], logPanel: $logPanel }
);

const resume = defineLogAction(
  async () => {
    // return await updateControllerPlugin.resume();
  },
  { name: "resume", args: [], logPanel: $logPanel }
);

const cancel = defineLogAction(
  async () => {
    // return await updateControllerPlugin.cancel();
  },
  { name: "cancel", args: [], logPanel: $logPanel }
);

const download = defineLogAction(
  async () => {
    return await updateControllerPlugin.download("http://172.30.95.93:8096/metadata.json");
  },
  { name: "download", args: [], logPanel: $logPanel }
);

const message = ref("这里显示收到的消息");

// 向desktop.dweb.waterbang.top.dweb 发送消息
const sayHi = async () => {
  const url = new URL("/say/hi", document.baseURI);
  url.searchParams.set("message", "今晚吃螃🦀️蟹吗？");
  const response = await dwebServiceWorker.externalFetch(`plaoc.html.demo.dweb`, url, {
    method: "POST",
    body: new Blob([`{"xxx":"哈哈哈"}`], { type: "application/json" }),
  });
  message.value = await response.text();
  console.log("sayHi return => ", message.value);
};
dwebServiceWorker.addEventListener("fetch", async (event) => {
  console.log("Dweb Service Worker fetch!", event);
  console.log("xxxx=>", await event.getRemoteManifest());
  const url = new URL(event.request.url);
  if (url.pathname.endsWith("/say/hi")) {
    const hiMessage = url.searchParams.get("message");
    console.log(`收到:${hiMessage}`);
    if (hiMessage) {
      message.value = hiMessage;
    }
    // 发送消息回去
    return event.respondWith(`吃，再来两斤二锅头。`);
  }

  return event.respondWith("Not match any routes");
});

const title = "Dweb Service Worker";
</script>

<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/splashscreen.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">APP之间通信</h2>
      <div class="card-actions">
        <input type="text" v-model="message" />
      </div>
      <div class="card-actions">
        <button class="inline-block rounded-full btn btn-accent" @click="sayHi">say hi</button>
      </div>
    </article>
    <article class="card-body">
      <h2 class="card-title">下载测试</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="download">下载新版本</button>
      </div>
      <!-- <div>
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
      </div> -->
    </article>

    <article class="card-body">
      <h2 class="card-title">APP 关闭、重启</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="close">close</button>
        <button class="inline-block rounded-full btn btn-accent" @click="restart">restart</button>
      </div>
    </article>

    <!-- <article class="card-body">
      <h2 class="card-title">下载控制器： 暂停/重下/取消</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="pause">pause</button>
        <button class="inline-block rounded-full btn btn-accent" @click="resume">resume</button>
        <button class="inline-block rounded-full btn btn-accent" @click="cancel">cancel</button>
      </div>
    </article> -->
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

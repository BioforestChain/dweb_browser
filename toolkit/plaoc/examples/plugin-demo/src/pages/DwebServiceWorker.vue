<script setup lang="ts">
import * as plaoc from "@plaoc/plugins";
import { dwebServiceWorker } from "@plaoc/plugins";
import { onMounted, ref } from "vue";
import LogPanel from "../components/LogPanel.vue";

Object.assign(globalThis, { dwebServiceWorker, plaoc });

onMounted(async () => {
  dwebServiceWorker.addEventListener("shortcut", (event) => {
    console.log("shortcut", event.data);
    plaoc.toastPlugin.show({ text: event.data });
  });
});

const close = async () => {
  return await dwebServiceWorker.close();
};

const restart = async () => {
  return await dwebServiceWorker.restart();
};

const message = ref("这里显示收到的消息");
const input = ref("这里写发送的消息");

// 向desktop.dweb.waterbang.top.dweb 发送消息
const sayHi = async () => {
  const response = await dwebServiceWorker.fetch(`file://app.example.com.dweb/say/hi?message=${input.value}`, {
    search: {
      哈哈哈: "xx",
    },
    method: "POST",
    body: new Blob([`{"xxx":${input.value}}`], { type: "application/json" }),
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
  return event.respondWith(`Not match any routes:${url.pathname}`);
});

const loading = ref(false);
const updateApp = async () => {
  window.open(`dweb://install?url=http://172.30.95.93:8000/metadata.json`);
};

const clearCache = async () => {
  dwebServiceWorker.clearCache();
};

const title = "Dweb Service Worker";
</script>

<template>
  <div class="card glass">
    <h2>{{ title }}</h2>
    <article class="card-body">
      <h2 class="card-title">APP之间通信</h2>
      <div class="card-actions">
        <input type="text" v-model="input" />
      </div>
      <div>{{ message }}</div>
      <div class="card-actions">
        <button class="inline-block rounded-full btn btn-accent" @click="sayHi">say hi</button>
      </div>
    </article>
    <article class="card-body">
      <h2 class="card-title">APP 关闭、重启</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="close">close</button>
        <button class="inline-block rounded-full btn btn-accent" @click="restart">restart</button>
        <v-btn class="inline-block rounded-full btn btn-accent" :loading="loading" @click="updateApp">查看升级</v-btn>
      </div>
    </article>
    <article class="card-body">
      <h2 class="card-title">清空数据</h2>
      <v-btn color="indigo-darken-3" @click="clearCache">clearCache</v-btn>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

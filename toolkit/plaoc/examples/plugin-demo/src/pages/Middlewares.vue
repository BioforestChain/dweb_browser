<script setup lang="ts">
import { BasePlugin } from "@plaoc/plugins";
import { onMounted, ref } from "vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";

const title = "中间件测试";

const $logPanel = ref<typeof LogPanel>();

let console: Console;
let socket: WebSocket;
onMounted(() => {
  console = toConsole($logPanel);
});

const message = ref("这里显示收到的消息");
const input = ref("这里写发送的消息");
const createSocket = () => {
  const url = new URL(BasePlugin.api_url.replace(/^http/, "ws"));
  url.pathname = `/websocket`;
  socket = new WebSocket(url);
  socket.addEventListener("message", (event) => {
    const msg = event.data;
    message.value = msg;
  });
};
const sendMessage = () => {
  socket?.send(input.value);
};
const closeSocket = () => {
  socket?.close();
};
</script>

<template>
  <dweb-network ref="$networkPlugin"></dweb-network>
  <div class="card glass p-3">
    <h2 class="text-2xl font-bold text-black">{{ title }}</h2>
    <article class="card-body">
      <h2 class="card-title">测试socket</h2>
      <div class="card-actions">
        <input type="text" v-model="input" />
      </div>
      <div>{{ message }}</div>
      <div class="card-actions">
        <button class="inline-block btn btn-accent" @click="createSocket">创建socket</button>
        <button class="inline-block btn btn-accent" @click="closeSocket">关闭socket</button>
        <button class="inline-block btn btn-accent" @click="sendMessage">发送消息</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

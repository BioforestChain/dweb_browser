<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";
import type { HTMLWebviewElement, WebViewItem } from "../plugin";

const state: {
  openUrl: string;
} = reactive({
  openUrl: location.href,
});
const title = "webview";
const $logPanel = ref<typeof LogPanel>();
const $webviewPlugin = ref<HTMLWebviewElement>();
let console: Console;
let webview: HTMLWebviewElement;
const webviewItem = ref<WebViewItem>();

onMounted(() => {
  console = toConsole($logPanel);
  webview = $webviewPlugin.value!;
});

async function open() {
  const res = await webview.open(state.openUrl);
  console.log("open", res);
  webviewItem.value = res;
}

async function close() {
  if(webviewItem.value === null)
  {
    console.log("close", "webview not opened");
    return;
  }
  
  const res = await webview.close(webviewItem.value!.webview_id);
  console.log("close", res);
}

async function activate() {
  const res = await webview.activate();
  console.log("activate", res);
}

async function closeWindow() {
  const res = await webview.closeWindow();
  console.log("closeWindow", res);
}
</script>
<template>
  <dweb-webview ref="$webviewPlugin"></dweb-webview>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/toast.svg" :alt="title" />
    </figure>

    <article class="card-body">
      <h2 class="card-title">open</h2>
      <v-text-field label="属性描述符" v-model="state.openUrl"></v-text-field>
      <v-btn color="indigo-darken-3" @click="open">open</v-btn>
    </article>

    <article class="card-body">
      <h2 class="card-title">close</h2>
      <v-btn color="indigo-darken-3" @click="close">close</v-btn>
    </article>

    <article class="card-body">
      <h2 class="card-title">activate</h2>
      <v-btn color="indigo-darken-3" @click="activate">activate</v-btn>
    </article>

    <article class="card-body">
      <h2 class="card-title">close window</h2>
      <v-btn color="indigo-darken-3" @click="closeWindow">close window</v-btn>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

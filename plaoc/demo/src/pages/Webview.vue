<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";
import type { HTMLMWebviewElement, WebViewItem } from "../plugin";

const state: {
  openUrl: string;
} = reactive({
  openUrl: "https://dweb.waterbang.top",
});
const title = "mwebview";
const $logPanel = ref<typeof LogPanel>();
const $mwebviewPlugin = ref<HTMLMWebviewElement>();
let console: Console;
let mwebview: HTMLMWebviewElement;
const webviewItem = ref<WebViewItem>();

onMounted(() => {
  console = toConsole($logPanel);
  mwebview = $mwebviewPlugin.value!;
});

async function open() {
  const res = await mwebview.open(state.openUrl);
  console.log("open", res);
  webviewItem.value = res;
}

async function close() {
  if(webviewItem.value === null)
  {
    console.log("close", "webview not opened");
    return;
  }
  
  const res = await mwebview.close(webviewItem.value!.webview_id);
  console.log("close", res);
}

async function activate() {
  const res = await mwebview.activate();
  console.log("activate", res);
}

async function closeWindow() {
  const res = await mwebview.closeApp();
  console.log("closeWindow", res);
}
</script>
<template>
  <dweb-mwebview ref="$mwebviewPlugin"></dweb-mwebview>
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

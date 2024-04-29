<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";
import { clipboardPlugin } from "@plaoc/plugins";

const title = "剪切板测试(Clipboard)";
const $logPanel = ref<typeof LogPanel>();
const writeData = ref("我是写入的数据");
let console: Console;

onMounted(async () => {
  console = toConsole($logPanel);
});

const read = async () => {
  const data = await clipboardPlugin.read();
  console.log("读取到=>", data.type, data.value);
};
const write = () => {
  clipboardPlugin.write({ string: writeData.value });
  console.log("写入到剪切板=>", writeData.value);
};
</script>
<template>
  <div class="card glass">
    <figure class="icon">
      <h2>{{ title }}</h2>
    </figure>

    <article class="card-body">
      <h2 class="card-title">写入剪切板</h2>
      <FieldLabel label="write:">
        <input type="text" v-model="writeData" />
        <button class="inline-block rounded-full btn btn-accent" @click="write">read</button>
      </FieldLabel>
    </article>
    <article class="card-body">
      <h2 class="card-title">读取剪切板</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="read">read</button>
      </div>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
import { ShareOptions, type HTMLDwebShareElement } from "../plugin";

const title = "Share";

const $logPanel = ref<typeof LogPanel>();
const $sharePlugin = ref<HTMLDwebShareElement>();

let console: Console;
let share: HTMLDwebShareElement;

const shareData = reactive({
  title: "分享标题🍉",
  text: "分享文字分享文字",
  url: "https://gpt.waterbang.top",
  files: null as FileList | null,
});

onMounted(() => {
  console = toConsole($logPanel);
  share = $sharePlugin.value!;
});

const shareHandle = defineLogAction(
  async () => {
    const result = await share.share(shareData as unknown as ShareOptions);
    console.info("shareHandle=>", result);
  },
  { name: "shareHandle", args: [], logPanel: $logPanel }
);

const fileChange = ($event: Event) => {
  const target = $event.target as HTMLInputElement;
  if (target && target.files?.[0]) {
    console.log("event", $event);
    console.log("target.files=>", target.files[0]);
    shareData.files = target.files;
  }
};
</script>
<template>
  <dweb-share ref="$sharePlugin"></dweb-share>
  <div class="card glass">
    <h2>{{ title }}</h2>
    <article class="card-body">
      <h2 class="card-title">Share</h2>
      <FieldLabel label="title:">
        <input type="text" v-model="shareData.title" />
      </FieldLabel>
      <FieldLabel label="text:">
        <input type="text" v-model="shareData.text" />
      </FieldLabel>
      <FieldLabel label="url:">
        <input type="url" v-model="shareData.url" />
      </FieldLabel>
      <FieldLabel label="files:">
        <input type="file" @change="fileChange($event)" />
      </FieldLabel>

      <div class="text-xs mockup-code min-w-max">
        <code>这是传输的json配置</code>
      </div>

      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="shareHandle">Share</button>
      </div>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

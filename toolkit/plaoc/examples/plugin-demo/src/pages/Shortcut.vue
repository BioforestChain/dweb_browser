<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";
import { ShortcutOption, shortcutPlugin } from "../plugin";

const $logPanel = ref<typeof LogPanel>();
let console: Console;

onMounted(async () => {
  console = toConsole($logPanel);
});

const shortcut = reactive({
  title: "新年快乐",
  data: "这是一条发到ipcEvent的消息🧨",
  icon: null,
} as ShortcutOption);

const registry = async () => {
  const res = await shortcutPlugin.registry(shortcut);
  console.log("registry=>", res);
};

const onFileChanged = async ($event: Event) => {
  const target = $event.target as HTMLInputElement;
  if (target && target.files?.[0]) {
    const img = target.files[0];
    console.info("photo ==> ", img.name, img.type, img.size);
    shortcut.icon = new Uint8Array(await img.arrayBuffer());
  }
};
</script>

<template>
  <dweb-biometrics ref="$biometricsPlugin"></dweb-biometrics>
  <div class="card glass">
    <figure class="icon">
      <div class="swap-on">🔗</div>
    </figure>
    <article class="card-body">
      <h2 class="card-title">设置短连接</h2>
      <v-text-field label="显示标题" v-model="shortcut.title"></v-text-field>
      <v-text-field label="传递的数据" v-model="shortcut.data"></v-text-field>
      <v-file-input
        label="File input"
        variant="solo"
        ref="$inputFile"
        type="file"
        accept="image/*"
        @change="onFileChanged($event)"
      ></v-file-input>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="registry">注册短链接</button>
      </div>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

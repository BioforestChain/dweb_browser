<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";

const title = "InputFile";

const $logPanel = ref<typeof LogPanel>();
const $inputFile = ref<HTMLInputElement>();

let console: Console;
let inputFile: HTMLInputElement;

const fileData = reactive({
  capture: false,
  accept: [] as string[],
  multiple: false,
  items: ["*/*", "image/*", "video/*", "audio/*"],
});

onMounted(() => {
  console = toConsole($logPanel);
  inputFile = $inputFile.value!;
});

const resetInput = defineLogAction(
  async () => {
    inputFile.value = "";
  },
  { name: "shareHandle", args: [], logPanel: $logPanel }
);
</script>
<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/share.svg" :alt="title" />
    </figure>

    <article class="card-body">
      <h2 class="card-title">InputFile</h2>
      <div v-if="fileData.capture">
        <v-file-input
          label="File input"
          variant="solo"
          ref="$inputFile"
          type="file"
          capture
          :accept="fileData.accept"
          :multiple="fileData.multiple"
        ></v-file-input>
      </div>
      <div v-else>
        <v-file-input
          label="File input"
          variant="solo"
          ref="$inputFile"
          type="file"
          :accept="fileData.accept"
          :multiple="fileData.multiple"
        ></v-file-input>
      </div>
      <v-select variant="solo" label="accept" :items="fileData.items" v-model="fileData.accept"></v-select>
      <v-switch v-model="fileData.multiple" label="multiple"></v-switch>
      <v-switch v-model="fileData.capture" color="indigo-darken-3" label="capture"></v-switch>
      <div class="text-xs mockup-code min-w-max">
        <code>这是传输的json配置</code>
      </div>

      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="resetInput">Reset</button>
      </div>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

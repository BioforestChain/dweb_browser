<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";

const title = "InputFile";

const $logPanel = ref<typeof LogPanel>();
const $inputFile = ref<HTMLInputElement>();

let console: Console;
let inputFile: HTMLInputElement;

const fileData = reactive({
  capture: false as boolean | "user" | "environment",
  accept: [] as string[],
  multiple: false,
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
      <FieldLabel>
        <input
          ref="$inputFile"
          type="file"
          :capture="fileData.capture"
          :accept="fileData.accept.join(',')"
          :multiple="fileData.multiple"
        />
      </FieldLabel>

      <FieldLabel label="accept:">
        <div class="flex flex-row">
          <input class="flex-2" type="text" v-model="fileData.accept" />
          <select class="flex-1" multiple v-model="fileData.accept">
            <option value="*/*">any</option>
            <option value="image/*">image</option>
            <option value="video/*">video</option>
            <option value="audio/*">audio</option>
          </select>
        </div>
      </FieldLabel>
      <FieldLabel label="multiple:">
        <input type="checkbox" v-model="fileData.multiple" />
      </FieldLabel>
      <FieldLabel label="capture:">
        <select v-model="fileData.capture">
          <option :value="false">false</option>
          <option :value="true">true</option>
          <option value="user">user</option>
          <option value="environment">environment</option>
        </select>
      </FieldLabel>

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

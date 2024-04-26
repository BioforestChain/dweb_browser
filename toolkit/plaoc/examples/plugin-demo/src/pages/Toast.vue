<script setup lang="ts">
import { onMounted, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";
import type { HTMLDwebToastElement, ToastDuration } from "../plugin";
const title = "Toast";

const $logPanel = ref<typeof LogPanel>();
const $toastPlugin = ref<HTMLDwebToastElement>();

let console: Console;
let toast: HTMLDwebToastElement;
onMounted(() => {
  console = toConsole($logPanel);
  toast = $toastPlugin.value!;
});

// export default {};
const toast_message = ref("我是toast🍓");
const toast_duration = ref<ToastDuration>("short");
const toast_position = ref<"top" | "center" | "bottom">("top")
const showToast = defineLogAction(
  async () => {
    return toast.show({ text: toast_message.value, duration: toast_duration.value, position: toast_position.value });
  },
  { name: "showToast", args: [toast_message, toast_duration], logPanel: $logPanel }
);
</script>
<template>
  <dweb-toast ref="$toastPlugin"></dweb-toast>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/toast.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Show Toast</h2>
      <FieldLabel label="Toast Message:">
        <input type="text" id="toast-message" v-model="toast_message" />
      </FieldLabel>
      <FieldLabel label="Toast Duration:">
        <select name="toast-duration" id="toast-duration" v-model="toast_duration">
          <option value="long">long</option>
          <option value="short">short</option>
        </select>
      </FieldLabel>
      <FieldLabel label="Toast Position:">
        <select name="toast-duration" id="toast-posiiton" v-model="toast_position">
          <option value="top">top</option>
          <option value="bottom">bottom</option>
        </select>
      </FieldLabel>
      <div class="justify-end card-actions">
        <button class="inline-block rounded-full btn btn-accent" id="toast-show" @click="showToast()">Show</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

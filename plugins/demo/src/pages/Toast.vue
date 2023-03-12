<script setup lang="ts">
import { onMounted, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";
import type { Tduration, ToastPlugin } from "@bfex/plugin";
const title = "Toast";

const $logPanel = ref<typeof LogPanel>();
const $toastPlugin = ref<ToastPlugin>();

let console: Console;
let toast: ToastPlugin;
onMounted(() => {
  console = toConsole($logPanel);
  toast = $toastPlugin.value!;
});

// export default {};
const toast_message = ref("我是toast🍓");
const toast_duration = ref<Tduration>("short");
const showToast = async () => {
  console.info("show toast:", toast_message.value, toast_duration.value);
  const result = await toast.show({ text: toast_message.value, duration: toast_duration.value });
};
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
      <div class="card-actions justify-end">
        <button class="btn btn-accent inline-block rounded-full" id="toast-show" @click="showToast()">Show</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

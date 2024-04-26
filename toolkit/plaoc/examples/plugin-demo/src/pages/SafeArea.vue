<script setup lang="ts">
import { onMounted, ref } from "vue";
import { HTMLDwebSafeAreaElement, $SafeAreaState } from "../plugin";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
const title = "Safe Area";

const $logPanel = ref<typeof LogPanel>();
const $safeArea = ref<HTMLDwebSafeAreaElement>();

let console: Console;
let safeArea: HTMLDwebSafeAreaElement;
onMounted(async () => {
  console = toConsole($logPanel);
  safeArea = $safeArea.value!;
  onSafeAreaChange(await safeArea.getState(), "init");
});
const onSafeAreaChange = (info: $SafeAreaState, type: string) => {
  overlay.value = info.overlay;
  info.insets.effect({ css_var_name: "safe-area" });
  console.log(type, info);
};

const overlay = ref<boolean>(undefined as never);
const setOverlay = defineLogAction(
  async () => {
    await safeArea.setOverlay(overlay.value);
  },
  { name: "setOverlay", args: [], logPanel: $logPanel }
);
const getOverlay = defineLogAction(
  async () => {
    await safeArea.getOverlay();
  },
  { name: "getOverlay", args: [], logPanel: $logPanel }
);
</script>

<template>
  <dweb-safe-area ref="$safeArea" @statechange="onSafeAreaChange($event.detail, 'change')"></dweb-safe-area>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/safearea.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Safe Area Overlay</h2>
      <input class="toggle" type="checkbox" v-model="overlay" />
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" :disabled="null == overlay" @click="setOverlay">
          Set
        </button>
        <button class="inline-block rounded-full btn btn-accent" @click="getOverlay">Get</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>
<style>
:root {
  border-top: max(var(--safe-area-inset-top, 0px), 1px);
  border-left: max(var(--safe-area-inset-left, 0px), 1px);
  border-right: max(var(--safe-area-inset-right, 0px), 1px);
  border-bottom: max(var(--safe-area-inset-bottom, 0px), 1px);
  border-style: solid;
  border-color: red;
}
</style>

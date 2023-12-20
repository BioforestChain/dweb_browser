<script setup lang="ts">
import { onMounted, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
import { $VirtualKeyboardState, HTMLDwebVirtualKeyboardElement } from "../plugin";
const title = "Virtual Keyboard";

const $logPanel = ref<typeof LogPanel>();
const $virtualKeyboard = ref<HTMLDwebVirtualKeyboardElement>();

let console: Console;
let virtualKeyboard: HTMLDwebVirtualKeyboardElement;
onMounted(async () => {
  console = toConsole($logPanel);
  virtualKeyboard = $virtualKeyboard.value!;
  onVirtualKeyboardChange(await virtualKeyboard.getState(), "init");
});
const onVirtualKeyboardChange = (info: $VirtualKeyboardState, type: string) => {
  overlay.value = info.overlay;
  info.insets.effect({ css_var_name: "keyboard" });
  console.log(type, info);
};

const overlay = ref<boolean>(undefined as never);
const setOverlay = defineLogAction(
  async () => {
    await virtualKeyboard.setOverlay(overlay.value);
  },
  { name: "setOverlay", args: [], logPanel: $logPanel }
);
const getOverlay = defineLogAction(
  async () => {
    return await virtualKeyboard.getOverlay();
  },
  { name: "getOverlay", args: [], logPanel: $logPanel }
);
</script>

<template>
  <dweb-virtual-keyboard
    ref="$virtualKeyboard"
    @statechange="onVirtualKeyboardChange($event.detail, 'change')"
  ></dweb-virtual-keyboard>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/safearea.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Virtual Keyboard Overlay</h2>
      <input class="toggle" type="checkbox" v-model="overlay" />
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" :disabled="null == overlay" @click="setOverlay">
          Set
        </button>
        <button class="inline-block rounded-full btn btn-accent" @click="getOverlay">Get</button>
      </div>
      <FieldLabel label="Vibrate Pattern:">
        <input type="text" />
      </FieldLabel>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>
<style>
:root {
  border-top: max(var(--keyboard-inset-top, 0px), 1px);
  border-left: max(var(--keyboard-inset-left, 0px), 1px);
  border-right: max(var(--keyboard-inset-right, 0px), 1px);
  border-bottom: max(var(--keyboard-inset-bottom, 0px), 1px);
  border-style: solid;
  border-color: green;
}
</style>

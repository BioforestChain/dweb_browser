<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";
import { StatusBarPlugin, STATUS_BAR_STYLE, $StatusBarState } from "@bfex/plugin";

const title = "StatusBar";

const $logPanel = ref<typeof LogPanel>();
const $statusbarPlugin = ref<StatusBarPlugin>();

let console: Console;
let statusbar: StatusBarPlugin;
onMounted(async () => {
  console = toConsole($logPanel);
  statusbar = $statusbarPlugin.value!;
  onStatusBarChange(await statusbar.getState());
});

const onStatusBarChange = (info: $StatusBarState) => {
  color.value = info.color;
  style.value = info.style;
  overlay.value = info.overlay;
  visible.value = info.visible;
};

const color = ref<string>(null as never);
const setColor = defineLogAction(
  async () => {
    await statusbar.setColor(color.value);
  },
  { name: "setColor", args: [color], logPanel: $logPanel }
);
const getColor = defineLogAction(
  async () => {
    color.value = await statusbar.getColor();
  },
  { name: "getColor", args: [color], logPanel: $logPanel }
);

const style = ref<STATUS_BAR_STYLE>(null as never);
const setStyle = defineLogAction(
  async () => {
    await statusbar.setStyle(style.value);
  },
  { name: "setStyle", args: [style], logPanel: $logPanel }
);
const getStyle = defineLogAction(
  async () => {
    style.value = await statusbar.getStyle();
  },
  { name: "getStyle", args: [style], logPanel: $logPanel }
);

const overlay = ref<boolean>(null as never);
const setOverlay = defineLogAction(() => statusbar.setOverlay(overlay.value), {
  name: "setOverlay",
  args: [overlay],
  logPanel: $logPanel,
});
const getOverlay = defineLogAction(
  async () => {
    overlay.value = await statusbar.getOverlay();
  },
  {
    name: "getOverlay",
    args: [overlay],
    logPanel: $logPanel,
  }
);
const visible = ref<boolean>(null as never);
const setVisible = defineLogAction(() => statusbar.setVisible(visible.value), {
  name: "setVisible",
  args: [visible],
  logPanel: $logPanel,
});
const getVisible = defineLogAction(
  async () => {
    visible.value = await statusbar.getVisible();
  },
  {
    name: "getOverlay",
    args: [visible],
    logPanel: $logPanel,
  }
);
</script>
<template>
  <dweb-status-bar ref="$statusbarPlugin" @stateChange="onStatusBarChange($event.detail)"></dweb-status-bar>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/statusbar.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Status Bar Background Color</h2>
      <v-color-picker v-model="color" :modes="['rgba']"></v-color-picker>

      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" :disabled="null == color" @click="setColor">
          Set
        </button>
        <button class="inline-block rounded-full btn btn-accent" @click="getColor">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Status Bar Style</h2>
      <select class="w-full max-w-xs select" name="statusbar-style" id="statusbar-style" v-model="style">
        <option value="DEFAULT">Default</option>
        <option value="DARK">Dark</option>
        <option value="LIGHT">Light</option>
      </select>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" :disabled="null == style" @click="setStyle">
          Set
        </button>
        <button class="inline-block rounded-full btn btn-accent" @click="getStyle">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Status Bar Overlays WebView</h2>
      <input class="toggle" type="checkbox" id="statusbar-overlay" v-model="overlay" />

      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" :disabled="null == overlay" @click="setOverlay">
          Set
        </button>
        <button class="inline-block rounded-full btn btn-accent" @click="getOverlay">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Status Bar Visible</h2>
      <input class="toggle" type="checkbox" id="statusbar-overlay" v-model="visible" />

      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" :disabled="null == visible" @click="setVisible">
          Set
        </button>
        <button class="inline-block rounded-full btn btn-accent" @click="getVisible">Get</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

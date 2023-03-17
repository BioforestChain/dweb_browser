<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";
import { NavigationBarPlugin, NAVIGATION_BAR_STYLE, $NavigationBarState } from "@bfex/plugin";

const title = "NavigationBar";

const $logPanel = ref<typeof LogPanel>();
const $navigationbarPlugin = ref<NavigationBarPlugin>();

let console: Console;
let navigationbar: NavigationBarPlugin;
onMounted(async () => {
  console = toConsole($logPanel);
  navigationbar = $navigationbarPlugin.value!;
  onNavigationBarChange(await navigationbar.getState());
});

const onNavigationBarChange = (info: $NavigationBarState) => {
  color.value = info.color;
  style.value = info.style;
  overlay.value = info.overlay;
  visible.value = info.visible;
};

const color = ref<string>(null as never);
const setColor = defineLogAction(
  async () => {
    await navigationbar.setColor(color.value);
  },
  { name: "setColor", args: [color], logPanel: $logPanel }
);
const getColor = defineLogAction(
  async () => {
    color.value = await navigationbar.getColor();
  },
  { name: "getColor", args: [color], logPanel: $logPanel }
);

const style = ref<NAVIGATION_BAR_STYLE>(null as never);
const setStyle = defineLogAction(
  async () => {
    await navigationbar.setStyle(style.value);
  },
  { name: "setStyle", args: [style], logPanel: $logPanel }
);
const getStyle = defineLogAction(
  async () => {
    style.value = await navigationbar.getStyle();
  },
  { name: "getStyle", args: [style], logPanel: $logPanel }
);

const overlay = ref<boolean>(null as never);
const setOverlay = defineLogAction(() => navigationbar.setOverlay(overlay.value), {
  name: "setOverlay",
  args: [overlay],
  logPanel: $logPanel,
});
const getOverlay = defineLogAction(
  async () => {
    overlay.value = await navigationbar.getOverlay();
  },
  {
    name: "getOverlay",
    args: [overlay],
    logPanel: $logPanel,
  }
);
const visible = ref<boolean>(null as never);
const setVisible = defineLogAction(() => navigationbar.setVisible(visible.value), {
  name: "setVisible",
  args: [visible],
  logPanel: $logPanel,
});
const getVisible = defineLogAction(
  async () => {
    visible.value = await navigationbar.getVisible();
  },
  {
    name: "getOverlay",
    args: [visible],
    logPanel: $logPanel,
  }
);
</script>
<template>
  <dweb-navigation-bar ref="$navigationbarPlugin" @change="onNavigationBarChange($event.detail)"></dweb-navigation-bar>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/navigationbar.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Navigation Bar Background Color</h2>
      <v-color-picker v-model="color" :modes="['rgba']"></v-color-picker>

      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" :disabled="null == color" @click="setColor">
          Set
        </button>
        <button class="inline-block rounded-full btn btn-accent" @click="getColor">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Navigation Bar Style</h2>
      <select class="w-full max-w-xs select" name="navigationbar-style" id="navigationbar-style" v-model="style">
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
      <h2 class="card-title">Navigation Bar Overlays WebView</h2>
      <input class="toggle" type="checkbox" id="navigationbar-overlay" v-model="overlay" />

      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" :disabled="null == overlay" @click="setOverlay">
          Set
        </button>
        <button class="inline-block rounded-full btn btn-accent" @click="getOverlay">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Navigation Bar Visible</h2>
      <input class="toggle" type="checkbox" id="navigationbar-overlay" v-model="visible" />

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

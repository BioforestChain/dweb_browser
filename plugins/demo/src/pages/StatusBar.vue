<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";
import { StatusbarPlugin, StatusbarStyle, StatusbarInfo } from "@bfex/plugin";

const title = "StatusBar";

// import { } from "@bfex/plugin"

// export default {};
// const elSetStatusbarStyle = document.querySelector("#statusbar-setStyle");
// const elSelectStatusbarStyle = document.querySelector("#statusbar-style");
// elSetStatusbarStyle.addEventListener("click", () => {
//   const value = elSelectStatusbarStyle.value;
//   const pluginsStatusbar = window.Capacitor.Plugins.StatusBar;
//   console.log("pluginsStatusbar: ", pluginsStatusbar);
//   pluginsStatusbar.setStyle({ style: "LIGHT" }).then(async (res) => {
//     if (res.status === 200) {
//       console.log("res: ", res);
//       console.log("设置bstatusar ok res.body===", await res.text());
//       return;
//     }
//     console.error("设置 statusbar style 失败", await res.text());
//   });
// });

const $logPanel = ref<typeof LogPanel>();
const $statusbarPlugin = ref<StatusbarPlugin>();

let console: Console;
let statusbar: StatusbarPlugin;
onMounted(async () => {
  console = toConsole($logPanel);
  statusbar = $statusbarPlugin.value!;
  onStatusBarChange(await statusbar.getInfo());
});

const onStatusBarChange = (info: StatusbarInfo) => {
  color.value = info.color;
  style.value = info.style;
  overlay.value = info.overlay;
  visible.value = info.visible;
};

const color = ref<string>(null as never);
const setBackgroundColor = defineLogAction(
  async () => {
    await statusbar.setBackgroundColor({ color: color.value });
  },
  { name: "setBackgroundColor", args: [color], logPanel: $logPanel }
);
const getBackgroundColor = defineLogAction(
  async () => {
    color.value = await statusbar.getBackgroundColor();
  },
  { name: "getBackgroundColor", args: [color], logPanel: $logPanel }
);

const style = ref<StatusbarStyle>(null as never);
const setStyle = defineLogAction(
  async () => {
    await statusbar.setStyle({ style: style.value });
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
const setOverlay = defineLogAction(() => statusbar.setOverlaysWebView({ overlay: overlay.value }), {
  name: "setOverlay",
  args: [overlay],
  logPanel: $logPanel,
});
const getOverlay = defineLogAction(
  async () => {
    overlay.value = await statusbar.getOverlaysWebView();
  },
  {
    name: "getOverlay",
    args: [overlay],
    logPanel: $logPanel,
  }
);
const visible = ref<boolean>(null as never);
const setVisible = defineLogAction(() => statusbar.setVisible({ visible: visible.value }), {
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
  <dweb-status-bar ref="$statusbarPlugin" @change="onStatusBarChange(event.detail)"></dweb-status-bar>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/statusbar.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Status Bar Background Color</h2>
      <v-color-picker v-model="color" :modes="['rgba']"></v-color-picker>

      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" :disabled="null == color" @click="setBackgroundColor">
          Set
        </button>
        <button class="inline-block rounded-full btn btn-accent" @click="getBackgroundColor">Get</button>
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

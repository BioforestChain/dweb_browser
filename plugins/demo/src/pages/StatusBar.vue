<script setup lang="ts">
import { onMounted, ref } from "vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";
import { StatusbarPlugin, StatusbarStyle } from "@bfex/plugin";
import { defineAction } from "../helpers/logHelper";

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
onMounted(() => {
  console = toConsole($logPanel);
  statusbar = $statusbarPlugin.value!;
});

const color = ref<string>(null as never);
const setBackgroundColor = defineAction(
  async () => {
    await statusbar.setBackgroundColor({ color: color.value });
  },
  { name: "setBackgroundColor", args: [color], logPanel: $logPanel }
);
const getBackgroundColor = defineAction(
  async () => {
    color.value = await statusbar.getBackgroundColor();
  },
  { name: "getBackgroundColor", args: [color], logPanel: $logPanel }
);

const style = ref<StatusbarStyle>(null as never);
const setStyle = defineAction(
  async () => {
    await statusbar.setStyle({ style: style.value });
  },
  { name: "setStyle", args: [style], logPanel: $logPanel }
);
const getStyle = defineAction(
  async () => {
    style.value = await statusbar.getStyle();
  },
  { name: "getStyle", args: [style], logPanel: $logPanel }
);

const overlay = ref<boolean>(null as never);
const setOverlay = defineAction(() => statusbar.setOverlaysWebView({ overlay: overlay.value }), {
  name: "setOverlay",
  args: [overlay],
  logPanel: $logPanel,
});
const getOverlay = defineAction(
  async () => {
    overlay.value = await statusbar.getOverlaysWebView();
  },
  {
    name: "getOverlay",
    args: [overlay],
    logPanel: $logPanel,
  }
);
</script>
<template>
  <dweb-statusbar ref="$statusbarPlugin"></dweb-statusbar>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/statusbar.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Status Bar Background Color</h2>
      <input class="color" type="color" v-model="color" />

      <div class="card-actions justify-end btn-group">
        <button class="rounded-full btn btn-accent inline-block" :disabled="null == color" @click="setBackgroundColor">
          Set
        </button>
        <button class="rounded-full btn btn-accent inline-block" @click="getBackgroundColor">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Status Bar Style</h2>
      <select class="max-w-xs w-full select" name="statusbar-style" id="statusbar-style" v-model="style">
        <option value="DEFAULT">Default</option>
        <option value="DARK">Dark</option>
        <option value="LIGHT">Light</option>
      </select>
      <div class="card-actions justify-end btn-group">
        <button class="rounded-full btn btn-accent inline-block" :disabled="null == style" @click="setStyle">
          Set
        </button>
        <button class="rounded-full btn btn-accent inline-block" @click="getStyle">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Status Bar Overlays WebView</h2>
      <input class="toggle" type="checkbox" id="statusbar-overlay" v-model="overlay" />

      <div class="card-actions justify-end btn-group">
        <button class="rounded-full btn btn-accent inline-block" :disabled="null == overlay" @click="setOverlay">
          Set
        </button>
        <button class="rounded-full btn btn-accent inline-block" @click="getOverlay">Get</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

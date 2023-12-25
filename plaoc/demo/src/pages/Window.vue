<script setup lang="ts">
import { UnwrapRef, onMounted, reactive, ref } from "vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
import { $WindowState, $WindowStyleColor, HTMLDwebWindowElement, windowPlugin } from "../plugin";

const title = "window";

const $logPanel = ref<typeof LogPanel>();
const $window = ref<HTMLDwebWindowElement>();

let console: Console;
let statusBar: HTMLDwebWindowElement;
onMounted(async () => {
  console = toConsole($logPanel);
  statusBar = $window.value!;
  onStatusBarChange(await statusBar.getState(), "init");
});
function defineRef<T>(
  name: string,
  getter: () => Promise<UnwrapRef<T>>,
  setter: (value: UnwrapRef<T>) => Promise<unknown>
) {
  const refIns = ref<T>(null as never);
  const setAction = defineLogAction(() => setter(refIns.value), {
    name: name + "::set",
    args: [refIns],
    logPanel: $logPanel,
  });
  const getAction = defineLogAction(
    async () => {
      refIns.value = await getter();
    },
    { name: name + "::get", args: [refIns], logPanel: $logPanel }
  );
  return [refIns, setAction, getAction] as const;
}

const onStatusBarChange = (info: $WindowState, type: string) => {
  topBarContentColor.value = info.topBarContentColor;
  topBarBackgroundColor.value = info.topBarBackgroundColor;
  topBarOverlay.value = info.topBarOverlay;
  console.log(type, info);
};

const [topBarContentColor, setTopBarContentColor, getTopBarContentColor] = defineRef<$WindowStyleColor>(
  "topBarContentColor",
  async () => (await statusBar.getState()).topBarContentColor,
  (topBarContentColor) => statusBar.setStyle({ topBarContentColor })
);

const [topBarBackgroundColor, setTopBarBackgroundColor, getTopBarBackgroundColor] = defineRef<$WindowStyleColor>(
  "topBarBackgroundColor",
  async () => (await statusBar.getState()).topBarBackgroundColor,
  (topBarBackgroundColor) => statusBar.setStyle({ topBarBackgroundColor })
);

const [topBarOverlay, setTopBarOverlay, getTopBarOverlay] = defineRef<boolean>(
  "topBarOverlay",
  async () => (await statusBar.getState()).topBarOverlay,
  (topBarOverlay) => statusBar.setStyle({ topBarOverlay })
);

const getDisplay = async () => {
  const data = await statusBar.getDisplay();
  console.log("getDisplay=>", data);
};

const state: {
  openUrl: string;
} = reactive({
  openUrl: "https://dwebdapp.com",
});
async function open() {
  const res = windowPlugin.openInBrowser(state.openUrl);
  console.log("open", res);
}

const focus = () => {
  return statusBar.focusWindow();
};
const blur = () => {
  return statusBar.blurWindow();
};
const maximize = () => {
  return statusBar.maximize();
};
const unMaximize = () => {
  return statusBar.unMaximize();
};
const visible = () => {
  return statusBar.visible();
};
const close = () => {
  return statusBar.close();
};
</script>
<template>
  <dweb-window ref="$window" @statechange="onStatusBarChange($event.detail, 'change')"></dweb-window>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/statusbar.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">open</h2>
      <v-text-field label="属性描述符" v-model="state.openUrl"></v-text-field>
      <v-btn color="indigo-darken-3" @click="open">open</v-btn>
    </article>
    <article class="card-body">
      <h2 class="card-title">设置窗口状态</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="focus">聚焦</button>
        <button class="inline-block rounded-full btn btn-accent" @click="blur">模糊</button>
      </div>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="maximize">最大化</button>
        <button class="inline-block rounded-full btn btn-accent" @click="unMaximize">最小化</button>
      </div>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="visible">隐藏</button>
        <button class="inline-block rounded-full btn btn-accent" @click="close">关闭窗口</button>
      </div>
    </article>
    <article class="card-body">
      <h2 class="card-title">Window Top Bar Content Color</h2>
      <v-color-picker v-model="topBarContentColor" :modes="['hex']"></v-color-picker>

      <div class="justify-end card-actions btn-group">
        <button
          class="inline-block rounded-full btn btn-accent"
          :disabled="null == topBarContentColor"
          @click="setTopBarContentColor"
        >
          Set
        </button>
        <button class="inline-block rounded-full btn btn-accent" @click="getTopBarContentColor">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Window Top Bar Background Color</h2>
      <v-color-picker v-model="topBarBackgroundColor" :modes="['hex']"></v-color-picker>

      <div class="justify-end card-actions btn-group">
        <button
          class="inline-block rounded-full btn btn-accent"
          :disabled="null == topBarBackgroundColor"
          @click="setTopBarBackgroundColor"
        >
          Set
        </button>
        <button class="inline-block rounded-full btn btn-accent" @click="getTopBarBackgroundColor">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Window Top Bar Overlay</h2>
      <input class="toggle" type="checkbox" id="statusbar-overlay" v-model="topBarOverlay" />

      <div class="justify-end card-actions btn-group">
        <button
          class="inline-block rounded-full btn btn-accent"
          :disabled="null == topBarOverlay"
          @click="setTopBarOverlay"
        >
          Set
        </button>
        <button class="inline-block rounded-full btn btn-accent" @click="getTopBarOverlay">Get</button>
        <button class="inline-block rounded-full btn btn-accent" @click="getDisplay">getDisplay</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

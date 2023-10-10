<script setup lang="ts">
import { UnwrapRef, onMounted, ref } from "vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
import { $WindowState, $WindowStyleColor, HTMLDwebWindowElement } from "../plugin";

const title = "StatusBar";

const $logPanel = ref<typeof LogPanel>();
const $window = ref<HTMLDwebWindowElement>();

let console: Console;
let windowPlugin: HTMLDwebWindowElement;
onMounted(async () => {
  console = toConsole($logPanel);
  windowPlugin = $window.value!;
  onStatusBarChange(await windowPlugin.getState(), "init");
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
  async () => (await windowPlugin.getState()).topBarContentColor,
  (topBarContentColor) => windowPlugin.setStyle({ topBarContentColor })
);

const [topBarBackgroundColor, setTopBarBackgroundColor, getTopBarBackgroundColor] = defineRef<$WindowStyleColor>(
  "topBarBackgroundColor",
  async () => (await windowPlugin.getState()).topBarBackgroundColor,
  (topBarBackgroundColor) => windowPlugin.setStyle({ topBarBackgroundColor })
);

const [topBarOverlay, setTopBarOverlay, getTopBarOverlay] = defineRef<boolean>(
  "topBarOverlay",
  async () => (await windowPlugin.getState()).topBarOverlay,
  (topBarOverlay) => windowPlugin.setStyle({ topBarOverlay })
);

const focus = () => {
  return windowPlugin.focusWindow();
};
const blur = () => {
  return windowPlugin.blurWindow();
};
const maximize = () => {
  return windowPlugin.maximize();
};
const unMaximize = () => {
  return windowPlugin.unMaximize();
};
const visible = () => {
  return windowPlugin.visible();
};
const close = () => {
  return windowPlugin.close();
};
</script>
<template>
  <dweb-window ref="$window" @statechange="onStatusBarChange($event.detail, 'change')"></dweb-window>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/statusbar.svg" :alt="title" />
    </figure>
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
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

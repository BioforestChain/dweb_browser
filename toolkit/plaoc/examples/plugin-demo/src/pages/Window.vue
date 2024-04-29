<script setup lang="ts">
import { UnwrapRef, onMounted, reactive, ref } from "vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
import { $WindowState, $WindowStyleColor, HTMLDwebWindowElement } from "../plugin";

const title = "window";

const $logPanel = ref<typeof LogPanel>();
const $window = ref<HTMLDwebWindowElement>();

let console: Console;
let windowComponent: HTMLDwebWindowElement;
onMounted(async () => {
  console = toConsole($logPanel);
  windowComponent = $window.value!;
  onStatusBarChange(await windowComponent.getState(), "init");
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
  async () => (await windowComponent.getState()).topBarContentColor,
  (topBarContentColor) => windowComponent.setStyle({ topBarContentColor })
);

const [topBarBackgroundColor, setTopBarBackgroundColor, getTopBarBackgroundColor] = defineRef<$WindowStyleColor>(
  "topBarBackgroundColor",
  async () => (await windowComponent.getState()).topBarBackgroundColor,
  (topBarBackgroundColor) => windowComponent.setStyle({ topBarBackgroundColor })
);

const [topBarOverlay, setTopBarOverlay, getTopBarOverlay] = defineRef<boolean>(
  "topBarOverlay",
  async () => (await windowComponent.getState()).topBarOverlay,
  (topBarOverlay) => windowComponent.setStyle({ topBarOverlay })
);

const getDisplay = async () => {
  const data = await windowComponent.getDisplay();
  console.log("getDisplay=>", data);
};

const state: {
  openUrl: string;
} = reactive({
  openUrl: "https://dwebdapp.com",
});
async function open() {
  const res = window.open(`dweb://openinbrowser?url=${state.openUrl}`);
  console.log("open", res);
}

const focus = () => {
  return windowComponent.focusWindow();
};
const blur = () => {
  return windowComponent.blurWindow();
};
const maximize = () => {
  return windowComponent.maximize();
};
const unMaximize = () => {
  return windowComponent.unMaximize();
};
const visible = () => {
  return windowComponent.visible();
};
const getDisplayInfo = async () => {
  const info = await windowComponent.getDisplay();
  console.log("当前屏幕信息：", JSON.stringify(info));
};
const close = () => {
  return windowComponent.close();
};

const boardData = reactive({
  width: 500,
  height: 750,
  resizeable: false,
});

//#region board
const setBounds = () => {
  windowComponent.setBounds(boardData.resizeable, boardData.width, boardData.height);
};
//#endregion
</script>
<template>
  <dweb-window ref="$window" @statechange="onStatusBarChange($event.detail, 'change')"></dweb-window>
  <div class="card glass">
    <h2>{{ title }}</h2>
    <article class="card-body">
      <h2 class="card-title">open</h2>
      <v-text-field label="属性描述符" v-model="state.openUrl"></v-text-field>
      <v-btn color="indigo-darken-3" @click="open">open</v-btn>
    </article>
    <article class="card-body">
      <h2 class="card-title">设置窗口大小</h2>
      <v-switch v-model="boardData.resizeable" color="indigo-darken-3" label="是否可以resize"></v-switch>
      <v-text-field label="宽" type="number" v-model="boardData.width"></v-text-field>
      <v-text-field label="高" type="number" v-model="boardData.height"></v-text-field>
      <v-btn color="indigo-darken-3" @click="setBounds">设置</v-btn>
    </article>
    <article class="card-body">
      <h2 class="card-title">设置窗口状态</h2>
      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" @click="focus">聚焦</button>
        <button class="inline-block rounded-full btn btn-accent" @click="blur">模糊</button>
        <button class="inline-block rounded-full btn btn-accent" @click="maximize">最大化</button>
        <button class="inline-block rounded-full btn btn-accent" @click="unMaximize">最小化</button>
        <button class="inline-block rounded-full btn btn-accent" @click="visible">隐藏</button>
        <button class="inline-block rounded-full btn btn-accent" @click="close">关闭窗口</button>
        <button class="inline-block rounded-full btn btn-accent" @click="getDisplayInfo">获取当前窗口信息</button>
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

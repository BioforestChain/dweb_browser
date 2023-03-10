<script setup lang="ts">
import { onMounted, ref, watch } from "vue";
import LogPanel, { toConsole } from "../components/LogPanel.vue";
import { Navigatorbar, NavigationBarPluginEvents } from "@bfex/plugin"
import { throttle } from "../helper/throttle";

const title = "Navigation Bar";

const $logPanel = ref<typeof LogPanel>();
const $NavigatorbarPlugin = ref<Navigatorbar>();


let console: Console;
let navigator: Navigatorbar;
onMounted(() => {
  console = toConsole($logPanel);
  navigator = $NavigatorbarPlugin.value!;
  listen_change()
  listen_hide()
  listen_show()
});

const color = ref("#000000")
const visiable = ref(false)
const transparency = ref(false)
const overlay = ref(false)
// 防止颜色刷新过快
// watch(color, async (newColor, oldColor) => {
//   console.info(newColor, oldColor)
//   throttle(function () {
//     oldColor = newColor
//   }, 500)
// })

const setColor = async () => {
  await navigator.setColor({ color: color.value })
  console.info("set Color", color.value)
}

const getColor = async () => {
  const res = await navigator.getColor()
  console.info("get Color", res.color)
}

const getVisible = async () => {
  const res = await navigator.getVisible().then(res => res.text())
  console.info("get visiable", res)
}

const setVisible = async () => {
  if (visiable.value === true) {
    await navigator.show()
  } else {
    await navigator.hide()
  }
  console.info("set visiable", visiable.value)
}

const setTransparency = async () => {
  await navigator.setTransparency({ isTransparent: transparency.value })
  console.info("set transparency", transparency.value)
}

const getTransparency = async () => {
  const res = await navigator.getTransparency().then(res => res.text())
  console.info("get transparency", res)
}

const setOverlay = async () => {
  await navigator.setOverlay({ isOverlay: overlay.value })
  console.info("set overlay", overlay.value)
}

const getOverlay = async () => {
  const res = await navigator.getOverlay().then(res => res.text())
  console.info("get overlay", res)
}

const listen_show = async () => {
  navigator.addListener(NavigationBarPluginEvents.SHOW, (res) => {
    console.info("listen_show", res)
  })
}

const listen_hide = async () => {
  navigator.addListener(NavigationBarPluginEvents.HIDE, (res) => {
    console.info("listen_hide", res)
  })
}
const listen_change = async () => {
  navigator.addListener(NavigationBarPluginEvents.COLOR_CHANGE, (res) => {
    console.info("listen_change", res)
  })
}
</script>

<template>
  <dweb-navigator ref="$NavigatorbarPlugin"></dweb-navigator>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/navigationbar.svg" :alt="title" />
    </figure>

    <article class="card-body">
      <h2 class="card-title">Navigation Bar Color</h2>
      <input class="color" type="color" v-model="color" />
      <div class="justify-end card-actions btn-group">
        <button class="rounded-full btn btn-accent" @click="setColor">Set</button>
        <button class="rounded-full btn btn-accent" @click="getColor">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Navigation Bar visible</h2>
      <input class="toggle toggle-accent" type="checkbox" v-model="visiable" />
      <div class="justify-end card-actions btn-group">
        <button class="rounded-full btn btn-accent" @click="setVisible">Set</button>
        <button class="rounded-full btn btn-accent" @click="getVisible">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Navigation Bar Transparency WebView</h2>
      <input class="toggle toggle-accent" type="checkbox" v-model="transparency" />
      <div class="justify-end card-actions btn-group">
        <button class="rounded-full inline-block btn btn-accent" @click="setTransparency">Set</button>
        <button class="rounded-full inline-block btn btn-accent" @click="getTransparency">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Navigation Bar Overlay WebView</h2>
      <input class="toggle toggle-accent" type="checkbox" v-model="overlay" />
      <div class="justify-end card-actions btn-group">
        <button class="rounded-full inline-block btn btn-accent" @click="setOverlay">Set</button>
        <button class="rounded-full inline-block btn btn-accent" @click="getOverlay">Get</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

<template>
  <div :class="$style.container">
    <div
      :class="{
        [$style.icon]: true,
        [$style.icon_back]: true,
        [$style.icon_active]: state.goBackIsActive,
      }"
      @click="back"
      @mouseenter="goBackOnMouseentery"
      @mouseleave="goBackOnMouseleave"
    >
      <svg
        t="1685784528771"
        :class="$style.icon_svg"
        viewBox="0 0 1024 1024"
        version="1.1"
        xmlns="http://www.w3.org/2000/svg"
        p-id="5666"
      >
        <path
          d="M810.666667 469.333333H334.08l208.213333-208.213333c16.64-16.64 16.64-43.946667 0-60.586667a42.496 42.496 0 0 0-60.16 0l-281.173333 281.173334a42.496 42.496 0 0 0 0 60.16l281.173333 281.173333a42.496 42.496 0 1 0 60.16-60.16L334.08 554.666667H810.666667c23.466667 0 42.666667-19.2 42.666666-42.666667s-19.2-42.666667-42.666666-42.666667z"
          fill="currentColor"
          p-id="5667"
        ></path>
      </svg>
    </div>
    <div
      :class="{
        [$style.icon]: true,
        [$style.icon_forward]: true,
        [$style.icon_active]: state.goForwardIsActive,
      }"
      @click="forward"
      @mouseenter="goForwardMouseenter"
      @mouseleave="goForwardOnMouseLeave"
    >
      <svg
        t="1685784674107"
        :class="$style.icon_svg"
        viewBox="0 0 1024 1024"
        version="1.1"
        xmlns="http://www.w3.org/2000/svg"
        p-id="1277"
      >
        <path
          d="M213.333333 554.666667h476.586667l-208.213333 208.213333c-16.64 16.64-16.64 43.946667 0 60.586667 16.64 16.64 43.52 16.64 60.16 0l281.173333-281.173334a42.496 42.496 0 0 0 0-60.16l-280.746667-281.6a42.496 42.496 0 1 0-60.16 60.16L689.92 469.333333H213.333333c-23.466667 0-42.666667 19.2-42.666666 42.666667s19.2 42.666667 42.666666 42.666667z"
          fill="currentColor"
          p-id="1278"
        ></path>
      </svg>
    </div>
    <div
      :class="{
        [$style.icon]: true,
        [$style.icon_active]: state.refreshIsActive,
      }"
      @click="refresh"
      @mouseenter="refreshOnMouseenter"
      @mouseleave="refreshOnMouseLeave"
    >
      <svg
        t="1685784952385"
        :class="$style.icon_svg"
        viewBox="0 0 1024 1024"
        version="1.1"
        xmlns="http://www.w3.org/2000/svg"
        p-id="6791"
      >
        <path
          d="M778.154667 665.6H704a38.4 38.4 0 1 1 0-76.8h122.624a38.229333 38.229333 0 0 1 23.978667 0h6.997333a38.4 38.4 0 0 1 38.4 38.4v153.6a38.4 38.4 0 1 1-76.8 0v-38.272a384.128 384.128 0 0 1-682.410667-148.522667 38.4 38.4 0 0 1 75.050667-16.341333A307.328 307.328 0 0 0 778.154667 665.6zM245.888 358.4H320a38.4 38.4 0 0 1 0 76.8H166.4a38.4 38.4 0 0 1-38.4-38.4V243.2a38.4 38.4 0 0 1 76.8 0v38.357333a384.128 384.128 0 0 1 681.898667 146.090667 38.4 38.4 0 1 1-74.922667 16.810667A307.328 307.328 0 0 0 245.888 358.4z"
          fill="currentColor"
          p-id="6792"
        ></path>
      </svg>
    </div>
  </div>
</template>
<script lang="ts" setup>
import { nativeFetch } from "src/provider/fetch.ts";
import { reactive } from "vue";
const state = reactive({
  goBackIsActive: false,
  goForwardIsActive: false,
  refreshIsActive: false,
});

async function goBackOnMouseentery() {
  const res = await nativeFetch("/can-go-back");
  const value = await res.json();
  console.log("value: ", value);
  state.goBackIsActive = value.value;
}

function goBackOnMouseleave() {
  state.goBackIsActive = false;
}

async function goForwardMouseenter() {
  const res = await nativeFetch("/can-go-forward");
  const value = await res.json();
  state.goForwardIsActive = value.value;
}

function goForwardOnMouseLeave() {
  state.goForwardIsActive = false;
}

function refreshOnMouseenter() {
  state.refreshIsActive = true;
}

function refreshOnMouseLeave() {
  state.refreshIsActive = false;
}

async function back() {
  nativeFetch("/go-back");
}

async function forward() {
  nativeFetch("/go-forward");
}

async function refresh() {
  nativeFetch("/refresh");
}
</script>
<style module>
.container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-grow: 0;
  flex-shrink: 0;
  padding-right: 20px;
  width: 120px;
  height: 100%;
}

.icon {
  display: flex;
  justify-content: center;
  align-items: center;
  width: auto;
  height: 100%;
  color: #cccccc;
}

.icon_active {
  color: #666;
  cursor: pointer;
}

.icon_svg {
  width: 22px;
  height: 22px;
}
</style>

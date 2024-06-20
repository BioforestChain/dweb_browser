<script setup lang="ts">
import { computed, ref } from "vue";
import squircle_svg_url from "../icon-squircle-box/squircle.svg";
import { $AppIconInfo } from "./types.ts";
import { get, set, createStore } from "idb-keyval";
import { computedAsync } from "@vueuse/core";
import { withLock } from "../../provider/lock.ts";
import { nativeFetch } from "../../provider/fetch.ts";

const iconStore = createStore("desk", "icon");
type $IconRow = { blob: Blob; updateTime: number };

const props = defineProps({
  size: {
    type: String,
    default: "100%",
  },
  bgColor: {
    type: String,
    default: "rgba(255, 255, 255, 0.2)",
  },
  bgImage: {
    type: String,
  },
  bgDisableTranslucent: {
    type: Boolean,
    default: false,
  },
  icon: {
    type: Object as () => $AppIconInfo,
    required: true,
  },
});

const mono_css = computed(() => props.icon.monoimage ?? props.icon.monocolor ?? "none");

const setIconRow = async (url: string) => {
  try {
    const iconBlob = await nativeFetch("/proxy", { search: { url }, responseType: "blob" });
    const iconRow: $IconRow = {
      blob: iconBlob,
      updateTime: Date.now(),
    };
    await set(url, iconRow, iconStore);
    return iconRow;
  } catch (err) {
    console.warn("fail to fetch error", err);
  }
};

const DAY = 24 * 60 * 60 * 1000;
const icon_blob_ref = ref<Blob>();
const icon_css_map = new Map<string, string>();
const icon_css = computedAsync<string | undefined>(async () => {
  let cache_icon_css = icon_css_map.get(props.icon.src);

  if (cache_icon_css) {
    return cache_icon_css;
  }

  const blob = await withLock(props.icon.src, async () => {
    if (false === props.icon.src.startsWith("https://")) {
      return;
    }
    let iconRow = await get<$IconRow>(props.icon.src, iconStore);
    if (iconRow === undefined) {
      iconRow = await setIconRow(props.icon.src);
      icon_blob_ref.value = iconRow?.blob;
    } else {
      icon_blob_ref.value = iconRow.blob;
      if (Date.now() - iconRow.updateTime > DAY) {
        void setIconRow(props.icon.src).then((it) => {
          icon_blob_ref.value = it?.blob;
        });
      }
    }
    return icon_blob_ref.value;
  });
  if (blob) {
    cache_icon_css = `url(${URL.createObjectURL(blob)})`;
    icon_css_map.set(props.icon.src, cache_icon_css);
    return cache_icon_css;
  } else {
    return `url(${props.icon.src})`;
  }
}, undefined);

const squircle_css = `url("${squircle_svg_url}")`;
const bg_image = computed(() => {
  if (props.bgImage) {
    return props.bgImage;
  } else {
    return `linear-gradient(0deg, rgb(255 255 255 / 70%), rgb(255 255 255 / 45%))`;
  }
});
const bg_color = computed(() => props.bgColor);
</script>

<template>
  <div class="app-icon z-grid">
    <div
      class="bg z-view"
      :class="{
        'backdrop-ios-glass': !bgDisableTranslucent,
      }"
    >
      <div class="squircle"></div>
    </div>
    <div
      class="fg z-view"
      :class="{
        maskable: icon.maskable,
        monochrome: icon.monochrome,
      }"
    ></div>
    <div class="z-view slot">
      <slot></slot>
    </div>
  </div>
</template>
<style scoped lang="scss">
.app-icon {
  width: v-bind(size);
  height: v-bind(size);
  border-radius: 16%;
  overflow: hidden;
  .bg {
    z-index: 0;
    .squircle {
      width: 100%;
      height: 100%;
      background-color: v-bind(bg_color);
      background-image: v-bind(bg_image);
      background-size: contain;
      background-position: center;
      background-repeat: no-repeat;
      mask-image: v-bind(squircle_css);
      mask-repeat: no-repeat;
      mask-position: center;
      mask-size: cover;
    }

    border-radius: 16%; // 高斯模糊的圆角
    width: 100%;
    height: 100%;

    stroke: rgb(0 0 0 / 50%);
    stroke-width: 1px;
    stroke-linejoin: round;
    z-index: 0;
  }
  .fg {
    z-index: 1;
    background-image: v-bind(icon_css);
    background-size: contain;
    background-position: center;
    --size: 87%; /// Math.SQRT2 * 0.618 对角线黄金分割
    width: var(--size);
    height: var(--size);
    &.maskable {
      --size: 100%;
    }
    &.monochrome {
      background: v-bind(mono_css);
      mask-image: v-bind(icon_css);
      mask-repeat: no-repeat;
      mask-position: center;
      mask-size: contain;
    }
  }
  .slot {
    width: v-bind(size);
    height: v-bind(size);
    z-index: 2;
    overflow: hidden;
  }
}
</style>

<script setup lang="ts">
import { computed } from "vue";
import squircle_svg_url from "../icon-squircle-box/squircle.svg";
import { $AppIconInfo } from "./types.ts";

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
const icon_css = computed(() => (props.icon.src ? `url(${JSON.stringify(props.icon.src)})` : "none"));
const squircle_css = `url(${squircle_svg_url})`;
const bg_image = computed(() => {
  if (props.bgImage) {
    return props.bgImage;
  } else {
    return `linear-gradient(to bottom, ${props.bgColor}, ${props.bgColor})`;
  }
});

// watchEffect(() => {
//   console.log("var_src:", icon_css.value, props.icon);
// });
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
        markable: icon.markable,
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

  .bg {
    z-index: 0;
    .squircle {
      width: 100%;
      height: 100%;
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
    &.markable {
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

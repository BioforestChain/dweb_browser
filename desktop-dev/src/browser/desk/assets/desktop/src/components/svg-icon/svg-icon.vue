<script setup lang="ts">
import { computed } from "vue";

const props = defineProps({
  size: {
    type: String,
    default: "100%",
  },
  src: {
    type: [String],
    required: true,
  },
  alt: String,
});
const css_url = computed(() => `url(${props.src})`);
</script>
<template>
  <span class="icon" :title="props.alt"></span>
</template>

<style scoped lang="scss">
.icon {
  width: v-bind(size);
  height: v-bind(size);
  background-image: v-bind(css_url);
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  display: inline-block;
  > :deep(svg) {
    width: 100%;
    height: 100%;
  }
}
</style>

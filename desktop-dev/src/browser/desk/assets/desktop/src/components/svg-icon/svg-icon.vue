<script setup lang="ts">
import { ref, watchEffect } from "vue";

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
const svgRaw = ref("");
watchEffect(async () => {
  if (!props.src) {
    return (svgRaw.value = "");
  }
  try {
    svgRaw.value = await (await fetch(props.src)).text();
  } catch {
    svgRaw.value = "";
  }
});
</script>
<template>
  <span class="icon" v-html="svgRaw" :data-src="props.src" :title="props.alt"></span>
</template>

<style scoped lang="scss">
.icon {
  width: v-bind(size);
  height: v-bind(size);
  display: inline-block;
  > :deep(svg) {
    width: 100%;
    height: 100%;
  }
}
</style>

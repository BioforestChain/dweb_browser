<script setup lang="ts">
import { ref, watchEffect } from "vue";

const props = defineProps({
  src: {
    type: String,
    required: true,
  },
  alt: String,
});
const svgRaw = ref("");
watchEffect(async () => {
  if (!props.src) {
    svgRaw.value = "";
  } else {
    try {
      svgRaw.value = await (await fetch(props.src)).text();
    } catch {
      svgRaw.value = "";
    }
  }
});
</script>
<template>
  <span class="icon" v-html="svgRaw" :data-src="props.src" :title="props.alt"></span>
</template>

<style scoped lang="scss">
.icon {
  width: 100%;
  height: 100%;
  display: inline-block;
  > :deep(svg) {
    width: 100%;
    height: 100%;
  }
}
</style>

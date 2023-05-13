<script setup lang="ts">
import { ref } from "vue";

// const label = ref("");
const input = ref("");
const props = defineProps({ label: String });
// defineExpose({
//   label,
// });

// defineProps({});
</script>
<template>
  <div class="form-control w-full max-w-xs">
    <label class="label">
      <span class="label-text">{{ props.label }}</span>
    </label>
    <slot></slot>
    <!-- <input type="text" class="input input-bordered input-accent max-w-xs" id="toast-message" v-model="input" /> -->
  </div>
</template>
<style scoped>
.form-control :deep(input[type="text"]),
.form-control :deep(input[type="url"]) ,
.form-control :deep(input[type="number"]) {
  @apply input input-bordered input-accent max-w-xs;
}
.form-control :deep(input[type="file"]) {
  @apply file-input file-input-bordered file-input-accent max-w-xs;
}
.form-control :deep(input[type="checkbox"]) {
  @apply toggle;
}
.form-control :deep(select) {
  @apply select input-bordered input-accent max-w-xs;
}
</style>

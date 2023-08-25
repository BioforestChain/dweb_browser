<script setup lang="ts">
import { ref } from 'vue';

const props = defineProps<{
  description: string;
}>()

const show = ref(false)

</script>

<template>
<div class="detail">
  <div class="title">描述</div>
  <div :class="show ?'':'content'" @click="show = !show">
    {{ props.description }}
  </div>
</div>

</template>

<style scoped>
.detail {
  display: flex;
  flex-direction: column;
  padding: 20px;
  cursor: pointer;
}
.title {
  font-size: 20px;
  font-weight: 400;
  margin-bottom: 10px;
}
.content {
  overflow : hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
</style>

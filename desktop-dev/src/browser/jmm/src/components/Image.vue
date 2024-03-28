<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { Carousel, Navigation, Slide } from 'vue3-carousel';
import 'vue3-carousel/dist/carousel.css';
const props = defineProps<{
  images: string[];
}>()
const perviewEle = ref<HTMLDivElement>();
let resizeOb: ResizeObserver | undefined;
// 显示的数量
const showCount = ref(1)
onMounted(() => {
  if (perviewEle.value) {
    resizeOb = new ResizeObserver(async (entries) => {
      for (const entry of entries) {
        const { width } = entry.contentRect;
        showCount.value = Math.floor(width/260 * 100) / 100
      }
    });
    resizeOb.observe(perviewEle.value);
  }
});
</script>

<template>
  <div class="preview-container" ref="perviewEle">
    <Carousel :items-to-show="showCount" :wrap-around="true">
      <Slide v-for="slide in props.images" :key="slide">
        <img class="img-box" :src="slide" alt="">
      </Slide>
      <template #addons>
        <Navigation />
      </template>
    </Carousel>
  </div>
</template>

<style scoped>
.preview-container {
  margin: 1em 0;
}
.img-box {
  flex: none;
  margin-left: 10px;
  width: 260px;
  border-radius: 20px;
  scroll-snap-align: center;
}

.img-box:first-of-type {
  margin-left: 0px;
}
</style>

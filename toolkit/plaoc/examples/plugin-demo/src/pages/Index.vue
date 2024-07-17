<script setup lang="ts">
import { routes } from "@/routes";
import { useRouter } from "vue-router";
const router = useRouter();
const go = (path: string) => {
  router.push(path);
};

const items = routes;
</script>
<template>
  <v-card class="mx-auto pa-2 my-card" variant="tonal" max-width="800" title="导航 🧭">
    <v-list class="my-list">
      <v-list-item
        v-for="(item, i) in items"
        :key="i"
        :value="item"
        color="primary"
        rounded="shaped"
        class="bg-[#03A9F4] my-list-item"
        @click="go(item.path)"
      >
        <template v-slot:prepend>
          <v-icon :icon="item.icon"></v-icon>
        </template>
        <v-list-item-title v-text="item.title"></v-list-item-title>
      </v-list-item>
    </v-list>
  </v-card>
</template>
<style>
.my-card {
  /* background: linear-gradient(256deg, #00a6ff2f, #00ffe175) !important; */
}
.my-list {
  /* background: unset;
  display: flex;
  flex-wrap: wrap;
  gap: 0.5em; */
  background: unset !important;
  column-count: 1;
}
@media (max-width: 500px) {
  .my-list {
    column-count: 1;
  }
}
@media (min-width: 500px) {
  .my-list {
    column-count: 2;
  }
}
@media (min-width: 800px) {
  .my-list {
    column-count: 3;
  }
}
.my-list-item {
}
</style>

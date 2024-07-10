<script setup lang="ts">
import { routes } from "@/routes";
import { useRouter } from "vue-router";
const router = useRouter();
const go = (path: string) => {
  router.push(path);
};

const items = routes
</script>
<template>
  <v-card class="mx-auto pa-2" max-width="300">
    <v-list>
      <v-list-subheader>导航 🧭</v-list-subheader>

      <v-list-item
        v-for="(item, i) in items"
        :key="i"
        :value="item"
        color="primary"
        rounded="shaped"
        class="bg-[#03A9F4]"
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

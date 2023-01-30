<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import Protal from "./components/protal.vue"
</script>

<template>
  <h1 class="title">BFM</h1>

  <Protal></Protal>

  <RouterView />
</template>

<style scoped>
.title {
  display: flex;
  justify-content: center;
}

header {
  line-height: 1.5;
  max-height: 100vh;
}

.logo {
  display: block;
  margin: 0 auto 2rem;
}


@media (min-width: 1024px) {
  header {
    display: flex;
    place-items: center;
    padding-right: calc(var(--section-gap) / 2);
  }

  .logo {
    margin: 0 2rem 0 0;
  }

}
</style>

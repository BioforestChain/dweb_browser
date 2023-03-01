<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'
import Protal from "./components/protal.vue"
import Clipboard from "./components/clipboard.vue";
</script>

<template>
  <h1 class="title">BFM</h1>

  <div class="content">
    <div class="item">
      <Protal></Protal>
    </div>
    <div class="item">
      <Clipboard></Clipboard>
    </div>
  </div>

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

.content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;

}

.item {
  margin: 10px;
}

.item::after {
  display: flex;
  justify-content: center;
  margin-top: 14px;
  content: "---";
}
</style>

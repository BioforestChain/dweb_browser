<script setup lang="ts">
const title = "Safe Area";
</script>

<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/safearea.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Get Safe Area Insets</h2>
      <div class="card-actions justify-end btn-group">
        <button class="btn btn-accent rounded-full" id="safearea-getSafeAreaInsets">Get</button>
      </div>
    </article>
  </div>
</template>

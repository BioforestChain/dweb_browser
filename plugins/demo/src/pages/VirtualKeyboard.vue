<script setup lang="ts">
import FieldLabel from "../components/FieldLabel.vue";
const title = "Keyboard";
</script>
<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/keyboard.svg" :alt="title" />
    </figure>

    <article class="card-body">
      <h2 class="card-title">Keyboard Show/Hide</h2>
      <div class="card-actions justify-end btn-group">
        <button class="btn btn-accent inline-block rounded-full" id="keyboard-show">Show</button>
        <button class="btn btn-accent inline-block rounded-full" id="keyboard-hide">Hide</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

<script setup lang="ts">
import FieldLabel from "../components/FieldLabel.vue";

const title = "Splash Screen";
</script>
<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/splashscreen.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Splash Screen Show/Hide</h2>
      <FieldLabel label="Auto Hidden After:">
        <label class="input-group">
          <input type="number" placeholder="1000" />
          <span>ms</span>
        </label>
      </FieldLabel>
      <div class="card-actions justify-end btn-group">
        <button class="btn btn-accent rounded-full" id="splashscreen-show">Show</button>
      </div>
    </article>
  </div>
</template>

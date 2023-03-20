<script setup lang="ts">
import FieldLabel from "../components/FieldLabel.vue";
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";
const title = "Haptics";
</script>

<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/vibrate.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Haptics Vibrate</h2>
      <FieldLabel label="Vibrate Pattern:">
        <input type="text" id="haptics-vibrate-pattern" placeholder="1,20,1,30" />
      </FieldLabel>
      <button class="inline-block rounded-full btn btn-accent" id="haptics-vibrate">Vibrate</button>
    </article>
  </div>
</template>

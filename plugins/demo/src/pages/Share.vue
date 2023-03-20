<script setup lang="ts">
import FieldLabel from "../components/FieldLabel.vue";
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";

const title = "Share";
</script>
<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/share.svg" :alt="title" />
    </figure>

    <article class="card-body">
      <h2 class="card-title">Share</h2>
      <FieldLabel label="title:">
        <input type="text" />
      </FieldLabel>
      <FieldLabel label="text:">
        <input type="text" />
      </FieldLabel>
      <FieldLabel label="url:">
        <input type="url" />
      </FieldLabel>
      <FieldLabel label="files:">
        <input type="file" />
      </FieldLabel>

      <div class="text-xs mockup-code min-w-max">
        <code>这是传输的json配置</code>
      </div>

      <div class="justify-end card-actions btn-group">
        <button class="inline-block rounded-full btn btn-accent" id="share-share">Share</button>
      </div>
    </article>
  </div>
</template>

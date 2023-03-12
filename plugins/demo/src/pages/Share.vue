<script setup lang="ts">
import FieldLabel from "../components/FieldLabel.vue";

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

      <div class="mockup-code text-xs min-w-max">
        <code>这是传输的json配置</code>
      </div>

      <div class="card-actions justify-end btn-group">
        <button class="btn btn-accent inline-block rounded-full" id="share-share">Share</button>
      </div>
    </article>
  </div>
</template>

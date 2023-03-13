<script setup lang="ts">
import FieldLabel from "../components/FieldLabel.vue";

const title = "Navigation Bar";

// const elBtnNavigatorbarGet = document.querySelector("#navigationbar-getNavigationBarColor");
// elBtnNavigatorbarGet.addEventListener("click", () => {
//   const _navigatorbar = window.Capacitor.Plugins.NavigationBar;
//   console.log("_navigatorbar: ", _navigatorbar);
// });
</script>

<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/navigationbar.svg" :alt="title" />
    </figure>

    <article class="card-body">
      <h2 class="card-title">Navigation Bar Color</h2>
      <input class="color" type="color" id="navigationbar-color" />
      <div class="card-actions justify-end btn-group">
        <button class="btn btn-accent rounded-full" id="navigationbar-setNavigationBarColor">Set</button>
        <button class="btn btn-accent rounded-full" id="navigationbar-getNavigationBarColor">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Navigation Bar Overlays WebView</h2>
      <input class="toggle toggle-accent" type="checkbox" id="navigationbar-overlay" />
      <div class="card-actions justify-end btn-group">
        <button class="btn btn-accent inline-block rounded-full" id="navigationbar-setOverlaysWebView">Set</button>
        <button class="btn btn-accent inline-block rounded-full" id="navigationbar-getOverlaysWebView">Get</button>
      </div>
    </article>
  </div>

  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

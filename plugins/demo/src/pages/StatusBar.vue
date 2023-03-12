<script setup lang="ts">
const title = "StatusBar";
// export default {};
// const elSetStatusbarStyle = document.querySelector("#statusbar-setStyle");
// const elSelectStatusbarStyle = document.querySelector("#statusbar-style");
// elSetStatusbarStyle.addEventListener("click", () => {
//   const value = elSelectStatusbarStyle.value;
//   const pluginsStatusbar = window.Capacitor.Plugins.StatusBar;
//   console.log("pluginsStatusbar: ", pluginsStatusbar);
//   pluginsStatusbar.setStyle({ style: "LIGHT" }).then(async (res) => {
//     if (res.status === 200) {
//       console.log("res: ", res);
//       console.log("设置bstatusar ok res.body===", await res.text());
//       return;
//     }
//     console.error("设置 statusbar style 失败", await res.text());
//   });
// });
</script>
<template>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/statusbar.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Status Bar Background Color</h2>
      <input class="color" type="color" id="statusbar-background-color" />

      <div class="card-actions justify-end btn-group">
        <button class="btn btn-accent inline-block rounded-full" id="statusbar-setBackgroundColor">Set</button>
        <button class="btn btn-accent inline-block rounded-full" id="statusbar-getBackgroundColor">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Status Bar Style</h2>
      <select class="select w-full max-w-xs" name="statusbar-style" id="statusbar-style">
        <option value="Default">Default</option>
        <option value="Dark">Dark</option>
        <option value="Light">Light</option>
      </select>
      <div class="card-actions justify-end btn-group">
        <button class="btn btn-accent inline-block rounded-full" id="statusbar-setStyle">Set</button>
        <button class="btn btn-accent inline-block rounded-full" id="statusbar-getStyle">Get</button>
      </div>
    </article>

    <article class="card-body">
      <h2 class="card-title">Status Bar Overlays WebView</h2>
      <input class="toggle" type="checkbox" id="statusbar-overlay" />

      <div class="card-actions justify-end btn-group">
        <button class="btn btn-accent inline-block rounded-full" id="statusbar-setOverlaysWebView">Set</button>
        <button class="btn btn-accent inline-block rounded-full" id="statusbar-getOverlaysWebView">Get</button>
      </div>
    </article>

    <div class="card-body">
      <div class="mockup-code text-xs min-w-max">
        <pre id="statusbar-observer-log"><code>xxxx</code></pre>
      </div>
    </div>
  </div>
</template>

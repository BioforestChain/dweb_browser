<script setup lang="ts">
import { onMounted, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import LogPanel, { toConsole, defineLogAction } from "../components/LogPanel.vue";
import { HTMLDwebSplashScreenElement } from "../plugin";
const $logPanel = ref<typeof LogPanel>();

const $splashScreen = ref<HTMLDwebSplashScreenElement>();

let console: Console;
let splashScreen: HTMLDwebSplashScreenElement;


onMounted(async () => {
  console = toConsole($logPanel);
  splashScreen = $splashScreen.value!;
});

const autoHidden = ref(1000)
const show = defineLogAction(async () => {
  const result = await (await splashScreen.show({ showDuration: autoHidden.value }))
  console.info("splash screen:", result)
}, { name: "show", args: [autoHidden], logPanel: $logPanel })

const title = "Splash Screen";
</script>
<template>
  <dweb-splash-screen ref="$splashScreen"></dweb-splash-screen>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/splashscreen.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">Splash Screen Show/Hide</h2>
      <FieldLabel label="Auto Hidden After:">
        <label class="input-group">
          <input type="number" placeholder="1000" v-model="autoHidden" />
          <span>ms</span>
        </label>
      </FieldLabel>
      <div class="justify-end card-actions btn-group">
        <button class="rounded-full btn btn-accent" @click="show">Show</button>
      </div>
    </article>
  </div>
</template>

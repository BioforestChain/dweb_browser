<script setup lang="ts">
import { onMounted, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
import type { HTMLDwebHapticsElement, ImpactStyle, NotificationType } from '../plugin';
const title = "Haptics";

const $logPanel = ref<typeof LogPanel>();
const $hapticsPlugin = ref<HTMLDwebHapticsElement>();

let console: Console;
let haptics: HTMLDwebHapticsElement;

const vibrate = ref([1000, 2000, 500, 1000, 1000, 2000, 500, 3000])

onMounted(() => {
  console = toConsole($logPanel);
  haptics = $hapticsPlugin.value!;
});


const impactStyle = ref<ImpactStyle>("HEAVY" as never)
const impactLight = defineLogAction(
  async () => {
    haptics.impactLight({ style: impactStyle.value })
  },
  { name: "impactLight", args: [], logPanel: $logPanel }
)

const notificationStyle = ref<NotificationType>("SUCCESS" as never)
const notification = defineLogAction(
  async () => {
    haptics.notification({ type: notificationStyle.value })
  },
  { name: "notification", args: [], logPanel: $logPanel }
)

const hapticsVibrate = defineLogAction(
  async () => {
    haptics.vibrate({ duration: vibrate.value })
  },
  { name: "hapticsVibrate", args: [vibrate], logPanel: $logPanel }
);
</script>

<template>
  <dweb-haptics ref="$hapticsPlugin"></dweb-haptics>
  <div class="card glass">
    <figure class="icon">
      <img src="../../assets/vibrate.svg" :alt="title" />
    </figure>
    <article class="card-body">
      <h2 class="card-title">触碰轻质量物体</h2>
      <FieldLabel label="Haptics Duration:">
        <select name="haptics-duration" class="haptics-duration" v-model="impactStyle">
          <option value="HEAVY">HEAVY</option>
          <option value="MEDIUM">MEDIUM</option>
          <option value="LIGHT">LIGHT</option>
        </select>
      </FieldLabel>
      <div class="justify-end card-actions">
        <button class="inline-block rounded-full btn btn-accent" @touchstart="impactLight">impactLight</button>
      </div>
    </article>
    <article class="card-body">
      <h2 class="card-title">振动通知</h2>
      <FieldLabel label="Haptics Duration:">
        <select name="haptics-duration" class="haptics-duration" v-model="notificationStyle">
          <option value="SUCCESS">SUCCESS</option>
          <option value="WARNING">WARNING</option>
          <option value="ERROR">ERROR</option>
        </select>
      </FieldLabel>
      <div class="justify-end card-actions">
        <button class="inline-block rounded-full btn btn-accent" @touchstart="notification">notification</button>
      </div>
    </article>
    <article class="card-body">
      <h2 class="card-title">单击手势的反馈振动</h2>
      <button class="inline-block rounded-full btn btn-accent" @touchstart="haptics.vibrateClick">vibrateClick</button>
    </article>
    <article class="card-body">
      <h2 class="card-title">禁用手势的反馈振动</h2>
      <button class="inline-block rounded-full btn btn-accent" @touchstart="haptics.vibrateDisabled">Disabled</button>
    </article>
    <article class="card-body">
      <h2 class="card-title">双击手势的反馈振动</h2>
      <button class="inline-block rounded-full btn btn-accent" @touchstart="haptics.vibrateDoubleClick">DoubleClick</button>
    </article>
    <article class="card-body">
      <h2 class="card-title">重击手势的反馈振动, 比如菜单键/惨案/3Dtouch</h2>
      <button class="inline-block rounded-full btn btn-accent" @touchstart="haptics.vibrateHeavyClick">HeavyClick</button>
    </article>
    <article class="card-body">
      <h2 class="card-title">滴答</h2>
      <button class="inline-block rounded-full btn btn-accent" @touchstart="haptics.vibrateTick">vibrateTick</button>
    </article>
    <article class="card-body">
      <h2 class="card-title">Haptics Vibrate</h2>
      <FieldLabel label="Vibrate Pattern:">
        <input type="text" id="haptics-vibrate-pattern" placeholder="1,20,1,30" v-model="vibrate" />
      </FieldLabel>
      <button class="inline-block rounded-full btn btn-accent" @touchstart="hapticsVibrate">Vibrate</button>
    </article>
  </div>
</template>

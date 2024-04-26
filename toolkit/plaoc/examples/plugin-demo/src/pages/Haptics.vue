<script setup lang="ts">
import { onMounted, ref } from "vue";
import FieldLabel from "../components/FieldLabel.vue";
import LogPanel, { defineLogAction, toConsole } from "../components/LogPanel.vue";
import type { HTMLDwebHapticsElement, ImpactStyle, NotificationType } from "../plugin";
import { isTouchDevice } from "../helpers/device.ts";
const title = "Haptics";

const $logPanel = ref<typeof LogPanel>();
const $hapticsPlugin = ref<HTMLDwebHapticsElement>();

let console: Console;
let haptics: HTMLDwebHapticsElement;

const vibrate = ref([1000, 2000, 500, 1000, 1000, 2000, 500, 3000]);

onMounted(() => {
  console = toConsole($logPanel);
  haptics = $hapticsPlugin.value!;
});

const impactStyle = ref<ImpactStyle>("HEAVY" as never);
const impactLight = defineLogAction(
  async () => {
    return (await haptics.impactLight({ style: impactStyle.value })).text();
  },
  { name: "impactLight", args: [], logPanel: $logPanel }
);

const impactLightClick = () => {
  if (isTouchDevice()) return;
  impactLight();
};

const notificationStyle = ref<NotificationType>("SUCCESS" as never);
const notification = defineLogAction(
  async () => {
    return (await haptics.notification({ type: notificationStyle.value })).text();
  },
  { name: "notification", args: [], logPanel: $logPanel }
);

const notificationClick = () => {
  if (isTouchDevice()) return;
  notification();
};

const hapticsVibrate = defineLogAction(
  async () => {
    return (await haptics.vibrate({ duration: vibrate.value })).text();
  },
  { name: "hapticsVibrate", args: [vibrate], logPanel: $logPanel }
);

const hapticsVibrateByClick = () => {
  if (isTouchDevice()) return;
  hapticsVibrate();
};

const hapticsVibrateClick = defineLogAction(
  async () => {
    return (await haptics.vibrateClick()).text();
  },
  { name: "vibrateClick", args: [], logPanel: $logPanel }
);

const hapticsVibrateClickByClickEvent = () => {
  if (isTouchDevice()) return;
  hapticsVibrateClick();
};

const hapticsVibrateDisabled = defineLogAction(
  async () => {
    return (await haptics.vibrateDisabled()).text();
  },
  { name: "Disabled", args: [], logPanel: $logPanel }
);

const disabledClick = () => {
  if (isTouchDevice()) return;
  hapticsVibrateDisabled();
};

const hapticsVibrateDoubleClick = defineLogAction(
  async () => {
    return (await haptics.vibrateDoubleClick()).text();
  },
  { name: "doubleClick", args: [], logPanel: $logPanel }
);

const doubleClick = () => {
  if (isTouchDevice()) return;
  hapticsVibrateDoubleClick();
};

const hapticsVibrateHeavyClick = defineLogAction(
  async () => {
    return (await haptics.vibrateHeavyClick()).text();
  },
  { name: "heavyClick", args: [], logPanel: $logPanel }
);

const heavyClick = () => {
  if (isTouchDevice()) return;
  hapticsVibrateHeavyClick();
};

const hapticsVibrateTick = defineLogAction(
  async () => {
    return (await haptics.vibrateTick()).text();
  },
  { name: "heavyClick", args: [], logPanel: $logPanel }
);

const vibrateTickClick = () => {
  if (isTouchDevice()) return;
  hapticsVibrateTick();
};
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
        <button class="inline-block rounded-full btn btn-accent" @touchstart="impactLight" @click="impactLightClick">
          impactLight
        </button>
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
        <button class="inline-block rounded-full btn btn-accent" @touchstart="notification" @click="notificationClick">
          notification
        </button>
      </div>
    </article>
    <article class="card-body">
      <h2 class="card-title">单击手势的反馈振动</h2>
      <button
        class="inline-block rounded-full btn btn-accent"
        @touchstart="hapticsVibrateClick"
        @click="hapticsVibrateClickByClickEvent"
      >
        vibrateClick
      </button>
    </article>
    <article class="card-body">
      <h2 class="card-title">禁用手势的反馈振动</h2>
      <button
        class="inline-block rounded-full btn btn-accent"
        @touchstart="hapticsVibrateDisabled"
        @click="disabledClick"
      >
        Disabled
      </button>
    </article>
    <article class="card-body">
      <h2 class="card-title">双击手势的反馈振动</h2>
      <button
        class="inline-block rounded-full btn btn-accent"
        @touchstart="hapticsVibrateDoubleClick"
        @click="doubleClick"
      >
        DoubleClick
      </button>
    </article>
    <article class="card-body">
      <h2 class="card-title">重击手势的反馈振动, 比如菜单键/惨案/3Dtouch</h2>
      <button
        class="inline-block rounded-full btn btn-accent"
        @touchstart="hapticsVibrateHeavyClick"
        @click="heavyClick"
      >
        HeavyClick
      </button>
    </article>
    <article class="card-body">
      <h2 class="card-title">滴答</h2>
      <button
        class="inline-block rounded-full btn btn-accent"
        @touchstart="haptics.vibrateTick"
        @click="vibrateTickClick"
      >
        vibrateTick
      </button>
    </article>
    <article class="card-body">
      <h2 class="card-title">Haptics Vibrate</h2>
      <FieldLabel label="Vibrate Pattern:">
        <input type="text" id="haptics-vibrate-pattern" placeholder="1,20,1,30" v-model="vibrate" />
      </FieldLabel>
      <button
        class="inline-block rounded-full btn btn-accent"
        @touchstart="hapticsVibrate"
        @click="hapticsVibrateByClick"
      >
        Vibrate
      </button>
    </article>
  </div>
  <div class="divider">LOG</div>
  <LogPanel ref="$logPanel"></LogPanel>
</template>

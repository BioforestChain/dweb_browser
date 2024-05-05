<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
const router = useRouter();
const go = (path: string) => {
  router.push(path);
};

const items = ref([
  { text: "window", icon: "mdi-window-restore", router: "./window" },
  { text: "inputfile", icon: "mdi-file-upload-outline", router: "/inputfile" },
  // { text: "camera", icon: "mdi-camera-outline", router: "/camera" },

  { text: "statusbar", icon: "mdi-signal-cellular-outline", router: "/statusbar" },
  { text: "navigationbar", icon: "mdi-menu", router: "./navigationbar" },
  { text: "safearea", icon: "mdi-security", router: "/safearea" },
  { text: "virtualkeyboard", icon: "mdi-keyboard", router: "/virtualkeyboard" },
  { text: "toast", icon: "mdi-message-alert-outline", router: "/toast" },

  { text: "share", icon: "mdi-share", router: "./share" },
  // { text: "splashscreen", icon: "mdi-account", router: "/splashscreen" },
  { text: "barcodescanning", icon: "mdi-qrcode-scan", router: "/barcodescanning" },
  { text: "torch", icon: "mdi-flashlight", router: "/torch" },

  { text: "geolocation", icon: "mdi-map-marker-outline", router: "./geolocation" },
  { text: "haptics", icon: "mdi-vibrate", router: "/haptics" },
  { text: "closewatcher", icon: "mdi-eye-off", router: "/closewatcher" },
  { text: "serviceworker", icon: "mdi-server-network", router: "/serviceworker" },

  { text: "media", icon: "mdi-multimedia", router: "./media" },
  { text: "biometrics", icon: "mdi-fingerprint", router: "/biometrics" },
  { text: "network", icon: "mdi-wan", router: "/network" },
  // { text: "bluetooth", icon: "mdi-flag", router: "/bluetooth" },

  { text: "device", icon: "mdi-devices", router: "./device" },
  { text: "shortcut", icon: "mdi-link-box-variant-outline", router: "/shortcut" },
  { text: "motionSensors", icon: "mdi-motion-sensor", router: "/motionSensors" },
  { text: "clipboard", icon: "mdi-clipboard-multiple-outline", router: "/clipboard" },
]);
</script>
<template>
  <v-card class="mx-auto pa-2" max-width="300">
    <v-list>
      <v-list-subheader>导航 🧭</v-list-subheader>

      <v-list-item
        v-for="(item, i) in items"
        :key="i"
        :value="item"
        color="primary"
        rounded="shaped"
        class="bg-[#03A9F4]"
        @click="go(item.router)"
      >
        <template v-slot:prepend>
          <v-icon :icon="item.icon"></v-icon>
        </template>
        <v-list-item-title v-text="item.text"></v-list-item-title>
      </v-list-item>
    </v-list>
  </v-card>
</template>

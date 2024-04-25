<script setup lang="ts">
import { ref } from "vue";
import { useRouter } from "vue-router";
const router = useRouter();
const go = (path: string) => {
  router.push(path);
};

const items = ref([
  { text: "window", icon: "mdi-clock", router: "./window" },
  { text: "inputfile", icon: "mdi-account", router: "/inputfile" },
  // { text: "camera", icon: "mdi-flag", router: "/camera" },

  { text: "statusbar", icon: "mdi-flag", router: "/statusbar" },
  { text: "navigationbar", icon: "mdi-clock", router: "./navigationbar" },
  { text: "safearea", icon: "mdi-account", router: "/safearea" },
  { text: "virtualkeyboard", icon: "mdi-flag", router: "/virtualkeyboard" },
  { text: "toast", icon: "mdi-flag", router: "/toast" },

  { text: "share", icon: "mdi-clock", router: "./share" },
  // { text: "splashscreen", icon: "mdi-account", router: "/splashscreen" },
  { text: "barcodescanning", icon: "mdi-flag", router: "/barcodescanning" },
  { text: "torch", icon: "mdi-flag", router: "/torch" },

  { text: "geolocation", icon: "mdi-clock", router: "./geolocation" },
  { text: "haptics", icon: "mdi-account", router: "/haptics" },
  { text: "closewatcher", icon: "mdi-flag", router: "/closewatcher" },
  { text: "serviceworker", icon: "mdi-flag", router: "/serviceworker" },

  { text: "media", icon: "mdi-clock", router: "./media" },
  { text: "biometrics", icon: "mdi-account", router: "/biometrics" },
  { text: "network", icon: "mdi-flag", router: "/network" },
  // { text: "bluetooth", icon: "mdi-flag", router: "/bluetooth" },

  { text: "device", icon: "mdi-clock", router: "./device" },
  { text: "shortcut", icon: "mdi-account", router: "/shortcut" },
  { text: "motionSensors", icon: "mdi-account", router: "/motionSensors" },
  { text: "clipboard", icon: "mdi-account", router: "/clipboard" },
]);
</script>
<template>
  <v-card class="mx-auto pa-2" max-width="300">
    <v-list>
      <v-list-subheader>导航🧭</v-list-subheader>

      <v-list-item
        v-for="(item, i) in items"
        :key="i"
        :value="item"
        color="primary"
        rounded="shaped"
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

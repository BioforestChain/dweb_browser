import type { RouteRecordRaw } from "vue-router";

export const routes = [
  { title: "Status Bar", path: "/statusbar", component: () => import("./pages/StatusBar.vue") },
  { title: "Navigation Bar", path: "/navigationbar", component: () => import("./pages/NavigationBar.vue") },
  { title: "Safe Area", path: "/safearea", component: () => import("./pages/SafeArea.vue") },
  { title: "Virtual Keyboard", path: "/virtualkeyboard", component: () => import("./pages/VirtualKeyboard.vue") },

  { title: "Toast", path: "/toast", component: () => import("./pages/Toast.vue") },
  { title: "Share", path: "/share", component: () => import("./pages/Share.vue") },
  { title: "Splash Screen", path: "/splashscreen", component: () => import("./pages/SplashScreen.vue") },
  { title: "Barcode Scanning", path: "/barcodescanning", component: () => import("./pages/BarcodeScanning.vue") },
  { title: "Torch", path: "/torch", component: () => import("./pages/Torch.vue") },

  { title: "Haptics", path: "/haptics", component: () => import("./pages/Haptics.vue") },
]; //satisfies RouteRecordRaw[];

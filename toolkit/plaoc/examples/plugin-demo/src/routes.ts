export const routes = [
  {
    title: "Home",
    icon: "mdi-home",
    path: "/",
    component: () => import("./pages/Index.vue"),
  },
  {
    title: "Window",
    icon: "mdi-window-restore",
    path: "/window",
    component: () => import("./pages/Window.vue"),
  },
  {
    title: "Input File",
    icon: "mdi-file-upload-outline",
    path: "/inputfile",
    component: () => import("./pages/InputFile.vue"),
  },
  {
    title: "Status Bar",
    icon: "mdi-signal-cellular-outline",
    path: "/statusbar",
    component: () => import("./pages/StatusBar.vue"),
  },
  {
    title: "Navigation Bar",
    icon: "mdi-menu",
    path: "/navigationbar",
    component: () => import("./pages/NavigationBar.vue"),
  },
  // { title: "Safe Area", path: "/safearea",  icon: "mdi-security", component: () => import("./pages/SafeArea.vue") },
  {
    title: "Virtual Keyboard",
    icon: "mdi-keyboard",
    path: "/virtualkeyboard",
    component: () => import("./pages/VirtualKeyboard.vue"),
  },

  { title: "Toast", icon: "mdi-message-alert-outline", path: "/toast", component: () => import("./pages/Toast.vue") },
  { title: "Share", icon: "mdi-share", path: "/share", component: () => import("./pages/Share.vue") },
  // { title: "Splash Screen", icon: "mdi-account",  path: "/splashscreen", component: () => import("./pages/SplashScreen.vue") },
  {
    title: "Barcode Scanning",
    icon: "mdi-qrcode-scan",
    path: "/barcodescanning",
    component: () => import("./pages/BarcodeScanning.vue"),
  },
  { title: "Torch", icon: "mdi-flashlight", path: "/torch", component: () => import("./pages/Torch.vue") },

  {
    title: "Geolocation",
    icon: "mdi-map-marker-outline",
    path: "/geolocation",
    component: () => import("./pages/Geolocation.vue"),
  },
  { title: "Haptics", icon: "mdi-vibrate", path: "/haptics", component: () => import("./pages/Haptics.vue") },

  {
    title: "Close Watcher",
    icon: "mdi-eye-off",
    path: "/closewatcher",
    component: () => import("./pages/CloseWatcher.vue"),
  },
  {
    title: "Dweb ServiceWorker",
    icon: "mdi-server-network",
    path: "/serviceworker",
    component: () => import("./pages/DwebServiceWorker.vue"),
  },
  { title: "Media", icon: "mdi-multimedia", path: "/media", component: () => import("./pages/Media.vue") },
  {
    title: "Biometrics",
    icon: "mdi-fingerprint",
    path: "/biometrics",
    component: () => import("./pages/Biometrics.vue"),
  },
  {
    title: "Keychain",
    icon: "mdi-key-chain",
    path: "/keychain",
    component: () => import("./pages/Keychain.vue"),
  },
  {
    title: "Network",
    icon: "mdi-wan",
    path: "/network",
    component: () => import("./pages/Network.vue"),
  },
  // { title: "bluetooth", path: "/bluetooth", component: () => import("./pages/Bluetooth.vue") },
  {
    title: "Device",
    icon: "mdi-devices",
    path: "/device",
    component: () => import("./pages/Device.vue"),
  },
  {
    title: "Shortcut",
    icon: "mdi-link-box-variant-outline",
    path: "/shortcut",
    component: () => import("./pages/Shortcut.vue"),
  },
  {
    title: "Motion Sensors",
    icon: "mdi-motion-sensor",
    path: "/motionSensors",
    component: () => import("./pages/MotionSensors.vue"),
  },
  {
    title: "Clipboard",
    icon: "mdi-clipboard-multiple-outline",
    path: "/clipboard",
    component: () => import("./pages/Clipboard.vue"),
  },
];
//satisfies (RouteRecordRaw & { title: string })[];

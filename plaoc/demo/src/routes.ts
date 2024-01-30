export const routes = [
  { title: "index", path: "/", component: () => import("./pages/Index.vue") },
  { title: "window", path: "/window", component: () => import("./pages/Window.vue") },
  { title: "Input File", path: "/inputfile", component: () => import("./pages/InputFile.vue") },
  { title: "Camera", path: "/camera", component: () => import("./pages/Camera.vue") },
  { title: "Status Bar", path: "/statusbar", component: () => import("./pages/StatusBar.vue") },
  { title: "Navigation Bar", path: "/navigationbar", component: () => import("./pages/NavigationBar.vue") },
  { title: "Safe Area", path: "/safearea", component: () => import("./pages/SafeArea.vue") },
  { title: "Virtual Keyboard", path: "/virtualkeyboard", component: () => import("./pages/VirtualKeyboard.vue") },

  { title: "Toast", path: "/toast", component: () => import("./pages/Toast.vue") },
  { title: "Share", path: "/share", component: () => import("./pages/Share.vue") },
  { title: "Splash Screen", path: "/splashscreen", component: () => import("./pages/SplashScreen.vue") },
  { title: "Barcode Scanning", path: "/barcodescanning", component: () => import("./pages/BarcodeScanning.vue") },
  { title: "Torch", path: "/torch", component: () => import("./pages/Torch.vue") },

  { title: "Geolocation", path: "/geolocation", component: () => import("./pages/Geolocation.vue") },
  { title: "Haptics", path: "/haptics", component: () => import("./pages/Haptics.vue") },

  { title: "Close Watcher", path: "/closewatcher", component: () => import("./pages/CloseWatcher.vue") },
  { title: "Dweb ServiceWorker", path: "/serviceworker", component: () => import("./pages/DwebServiceWorker.vue") },
  { title: "File System", path: "/filesystem", component: () => import("./pages/FileSystem.vue") },
  { title: "Biometrics", path: "/biometrics", component: () => import("./pages/Biometrics.vue") },
  { title: "Network", path: "/network", component: () => import("./pages/Network.vue") },

  { title: "bluetooth", path: "/bluetooth", component: () => import("./pages/Bluetooth.vue") },

  { title: "device", path: "/device", component: () => import("./pages/Device.vue") },
  { title: "shortcut", path: "/shortcut", component: () => import("./pages/Shortcut.vue") },
  { title: "motionSensors", path: "/motionSensors", component: () => import("./pages/MotionSensors.vue") },
];
//satisfies (RouteRecordRaw & { title: string })[];

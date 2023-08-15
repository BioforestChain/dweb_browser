import * as _Electron from "electron";
export * as micaElectron from "mica-electron";

Object.assign(globalThis, {
  Electron: _Electron,
});

// import { createRequire } from "node:module";
// import os from "node:os";
// class MicaBrowserWindow extends Electron.BrowserWindow {
//   setMicaEffect() {}
//   enableMargin() {}
//   setMicaAcrylicEffect() {}
//   setMicaTabbedEffect() {}
//   setAcrylic() {}
//   setRoundedCorner() {}
// }
// export let micaElectron = {
//   MicaBrowserWindow,
//   IS_WINDOWS_11: false,
// };
// if (os.platform() === "win32") {
//   const _micaElectron = createRequire(import.meta.url)("mica-electron");
//   micaElectron = _micaElectron;
// }
Object.assign(globalThis, {
  Electron: _Electron,
});

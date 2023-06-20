try {
  const { ipcRenderer } = require("electron");
  console.log(ipcRenderer);
  Object.assign(globalThis, {
    ipcRenderer,
  });
} catch {}
if ("ipcRenderer" in globalThis) {
  (async () => {
    const { exportApis } = await import("../../../../helper/openNativeWindow.preload");
    exportApis(globalThis);
  })();
}

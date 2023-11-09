if ("ipcRenderer" in self) {
  (async () => {
    const { exportApis } = await import("@dweb-browser/desktop/helper/openNativeWindow.preload.ts");
    exportApis(globalThis);
  })();
}

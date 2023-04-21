if ("ipcRenderer" in self) {
  (async () => {
    const { exportApis } = await import(
      "../../../helper/openNativeWindow.preload.mjs"
    );
    exportApis(globalThis);
  })();
}

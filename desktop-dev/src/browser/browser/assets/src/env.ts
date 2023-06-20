try {
  const { ipcRenderer } = require("electron");
  (async () => {
    const { exportApis } = await import(
      "../../../../helper/openNativeWindow.preload"
    );
    exportApis(globalThis);
  })();
} catch {}

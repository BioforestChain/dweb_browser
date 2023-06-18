import * as _Electron from "electron";
import Store from "npm:electron-store";
const config_store = new Store<ElectronConfig>({ name: "config" });
Object.assign(globalThis, {
  Electron: Object.assign(_Electron, {
    config: config_store,
  }),
});
declare global {
  namespace Electron {
    const config: typeof config_store;
  }
  interface ElectronConfig {}
}

import Store from "electron-store";
export const ElectronConfig = new Store<ElectronConfig>({ name: "config" });

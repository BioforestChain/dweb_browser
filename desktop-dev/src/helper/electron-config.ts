import Store from "electron-store";
export const ElectronConfig = new Store<ElectronConfig>({ name: "config" });
declare global {
  namespace Electron {
    const config: typeof ElectronConfig;
  }
  interface ElectronConfig {}
}
Object.assign(Electron, {
  config: ElectronConfig,
});

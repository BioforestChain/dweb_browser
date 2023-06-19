import Store from "npm:electron-store@8.1.0";
export const electronConfig = new Store<ElectronConfig>({ name: "config" });
declare global {
    interface ElectronConfig {
    //   [key: string]: unknown;
    }
//   namespace Electron {
//     const config: typeof ElectronConfig;
//   }
}
// Object.assign(Electron, {
//   config: ElectronConfig,
// });

import process from "node:process";

const isEnvSet = "ELECTRON_IS_DEV" in process.env;
const getFromEnv = Number.parseInt(process.env.ELECTRON_IS_DEV || "", 10) === 1;

export const isElectronDev = isEnvSet ? getFromEnv : !Electron.app.isPackaged;

export const tryDevUrl = async (originUrl: string, devUrl: string) => {
  if (isElectronDev) {
    try {
      const res = await fetch(devUrl);
      if (res.status == 200) {
        originUrl = devUrl;
      }
      res.body?.cancel();
    } catch {}
  }
  return originUrl;
};

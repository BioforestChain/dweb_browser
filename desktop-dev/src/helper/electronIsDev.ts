import Electron from "electron";
import process from "node:process";

const isEnvSet = "ELECTRON_IS_DEV" in process.env;
const getFromEnv = Number.parseInt(process.env.ELECTRON_IS_DEV || "", 10) === 1;

export const isElectronDev = isEnvSet ? getFromEnv : !Electron.app.isPackaged;

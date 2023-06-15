import Electron from "electron";
import path from "node:path";
import { pathToFileURL } from "node:url";
export const createResolveTo =
  (dirname: string) =>
  (...paths: string[]) =>
    path.resolve(dirname, ...paths);
    
export const ROOT = Electron.app.getAppPath();
export const resolveToRoot = createResolveTo(ROOT);

export const resolveToRootFile = (...paths: string[]) =>
  pathToFileURL(resolveToRoot(...paths));

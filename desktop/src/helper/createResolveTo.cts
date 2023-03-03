import path from "node:path";
import { pathToFileURL } from "node:url";
export const createResolveTo =
  (__dirname: string) =>
  (...paths: string[]) =>
    path.resolve(__dirname, ...paths);

export const ROOT = createResolveTo(__dirname)("../../");
export const resolveToRoot = createResolveTo(ROOT);

export const resolveToRootFile = (...paths: string[]) =>
  pathToFileURL(resolveToRoot(...paths));

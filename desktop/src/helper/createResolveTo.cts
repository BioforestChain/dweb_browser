import path from "node:path";
import { pathToFileURL } from "node:url";
export const createResolveTo =
  (__dirname: string) =>
  (...paths: string[]) =>
    path.resolve(__dirname, ...paths);

export const resolveToRoot = createResolveTo(
  createResolveTo(__dirname)("../../")
);

export const resolveToRootFile = (...paths: string[]) =>
  pathToFileURL(resolveToRoot(...paths));

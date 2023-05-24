import path from "path";
import { pathToFileURL } from "url";
export const createResolveTo = (__dirname) => (...paths) => path.resolve(__dirname, ...paths);
export const ROOT = createResolveTo(__dirname)("../../");
export const resolveToRoot = createResolveTo(ROOT);
export const resolveToRootFile = (...paths) => pathToFileURL(resolveToRoot(...paths));

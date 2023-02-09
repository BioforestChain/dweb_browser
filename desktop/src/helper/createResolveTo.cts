import path from "node:path";
export const createResolveTo =
  (__dirname: string) =>
  (...paths: string[]) =>
    path.resolve(__dirname, ...paths);

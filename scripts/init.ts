import { toolkitInit } from "../toolkit/scripts/toolkit-init.ts";

export const doInit = async () => {
  await toolkitInit();
};
if (import.meta.main) {
  doInit();
}

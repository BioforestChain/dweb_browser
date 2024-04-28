import { $ } from "./helper/exec.ts";

export const doInit = async () => {
  await $(`git submodule update --init`);
};
if (import.meta.main) {
  doInit();
}

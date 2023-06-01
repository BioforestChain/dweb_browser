export * from "./npm-build.ts";
export * from "./npm-pub.ts";
import { doBuild } from "./npm-build.ts";
import { doPub } from "./npm-pub.ts";
export const npm = async (args = Deno.args) => {
  const [cmd, ...rest] = args;
  switch (cmd) {
    case "build":
      await doBuild(rest);
      break;
    case "pub":
    case "publish":
      await doPub(rest);
      break;
  }
};

if (import.meta.main) {
  npm();
}

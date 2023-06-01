import { doBundle } from "./bundle.ts";
import { doServe } from "./serve.ts";

export const doCli = async (args = Deno.args) => {
  const [cmd, ...rest] = args;
  switch (cmd) {
    case "serve":
    case "preview":
      await doServe(rest);
      break;
    case "build":
    case "bundle":
      await doBundle(rest);
      break;
    default:
      throw new Error(`unknown command: ${cmd}`);
  }
};

if (import.meta.main) {
  doCli();
}

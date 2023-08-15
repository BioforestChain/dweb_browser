import { doBundle } from "./bundle.ts";
import { doConfig } from './config.ts';
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
    case "config":
      await doConfig(rest)
      break;
    default:
      throw new Error(`unknown command: ${cmd}`);
  }
};

doCli();

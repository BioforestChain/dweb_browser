export * from "./build_npm.ts";
export * from "./pub_npm.ts";
import { doBuildFromJson } from "./build_npm.ts";
import { cwdResolve } from "./cwd.ts";
import { doPubFromJson } from "./pub_npm.ts";
export const npm = async (args = Deno.args) => {
  const [cmd, ...rest] = args;
  switch (cmd) {
    case "build": {
      const [filepath, ...buildArgs] = rest;
      await doBuildFromJson(cwdResolve(filepath), buildArgs);
      break;
    }
    case "pub":
    case "publish": {
      const [inputFilepath, outputFilepath] = rest;
      await doPubFromJson(
        cwdResolve(inputFilepath),
        outputFilepath ? cwdResolve(outputFilepath) : undefined
      );
      break;
    }
  }
};

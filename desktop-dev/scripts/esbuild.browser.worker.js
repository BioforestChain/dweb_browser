// @ts-check
/// <reference types="node"/>

const process = require("node:process");
/**
 * @types {import("esbuild")}
 */
const esbuild = require("esbuild");
// const htmlModulesPlugin = require("esbuild-plugin-html-modules");
/**
 * @satisfies {esbuild.BuildOptions}
 */
const options = {
  bundle: true,
  format: "esm",
  entryPoints: ["./src/user/browser/browser.worker.ts"],
  outfile: "./electron/assets/browser.worker.js",
  loader: {
    ".html": "text",
  },

  // plugins: [
  //   htmlModulesPlugin()
  // ]
};

async function main() {
  if (process.argv.includes("--watch")) {
    (
      await esbuild.context({
        ...options,
        minify: false,
      })
    ).watch();
  } else {
    await esbuild.build(options);
    console.log("qqq")
  }
}

main();
// esbuild.build()
// .catch(() => process.exit(1))

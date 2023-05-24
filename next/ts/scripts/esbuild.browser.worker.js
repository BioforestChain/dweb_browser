/// <reference types="node"/>
// @ts-check
const esbuild = require("esbuild");
const htmlModulesPlugin = require("esbuild-plugin-html-modules");
const options = {
  bundle: true,
  format: "esm",
  entryPoints: ["./src/user/browser/browser.worker.ts"],
  outfile: "./bundle/browser.worker.js",
  loader: {
    ".html": "text",
  },

  // plugins: [
  //   htmlModulesPlugin()
  // ]
};

async function main() {
  const ctx = await esbuild.context(options);
  if (process.argv.includes("--watch")) {
    await ctx.watch();
  } else {
    await ctx.rebuild();
  }
}

main();
// esbuild.build()
// .catch(() => process.exit(1))

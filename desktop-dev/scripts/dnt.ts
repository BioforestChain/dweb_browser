import { build } from "../../scripts/helper/dnt/mod.ts";

// await emptyDir("./npm");

/// before build
Deno.copyFileSync(".npmrc", "electron/.npmrc");
await Deno.remove("./electron/src").catch(() => {}); /// if is symlink, will be remove

await build({
  entryPoints: ["./src/index.ts"],
  outDir: "./electron",
  shims: {
    // see JS docs for overview and more options
    deno: true,
  },
  importMap: "import_map.json",
  typeCheck: false,
  test: false,
  esModule: false,
  compilerOptions: {
    inlineSources: true,
    sourceMap: true,
  },
  package: {
    // package.json properties
    name: "@dweb-browser/desktop-sdk",
    version: Deno.args.filter((arg) => /^\d/.test(arg))[0],
    description: "Dweb Browser Development Kit",
    license: "MIT",
    config: {
      electron_mirror: "https://npm.taobao.org/mirrors/electron/",
    },
    scripts: {
      start: "electron ./",
    },
  },
  packageManager: "yarn",
  postBuild() {
    Deno.removeSync("./electron/src", { recursive: true }); /// remove nodejs ts-source
    Deno.symlinkSync("./src", "./electron/src"); /// create symlink

    /// 启动
    new Deno.Command("pnpm", {
      args: ["start", ...Deno.args.slice(Deno.args.indexOf("--start") + 1)],
      cwd: "./electron",
    }).spawn();
    // steps to run after building and before running the tests
    // Deno.copyFileSync("README.md", "npm/README.md");
  },
});

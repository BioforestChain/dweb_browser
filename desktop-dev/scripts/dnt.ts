import { build, emptyDir } from "https://deno.land/x/dnt@0.35.0/mod.ts";

// await emptyDir("./npm");
Deno.copyFileSync(".npmrc", "electron/.npmrc");

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
  packageManager: "pnpm",
  postBuild() {
    /// 启动
    new Deno.Command("pnpm", { args: ["start"], cwd: "./electron" }).spawn();
    // steps to run after building and before running the tests
    // Deno.copyFileSync("README.md", "npm/README.md");
  },
});

import path from "node:path";
import { fileURLToPath } from "node:url";
import { ESBuild } from "../../scripts/helper/ESBuild.ts";

const workspaceDir = fileURLToPath(import.meta.resolve("../"));
const resolveTo = (to: string) => path.resolve(workspaceDir, to);

export const doBundle = async () => {
  const builder = new ESBuild({
    entryPoints: [resolveTo("./electron/script/index.js")],
    outdir: resolveTo("./electron/bundle"),
    format: "cjs",
    bundle: true,
    external: ["electron", "lmdb"],
    platform: "node",
    write: true,
  });
  await builder.auto();
};

if (import.meta.main) {
  doBundle();
}

import * as fs from "node:fs";
import * as path from "node:path";
import { Flags } from "../deps.ts";
import {
  BundleFlagHelper,
  MetadataFlagHelper,
  NameFlagHelper,
} from "./helper/flags-helper.ts";

export const doBundle = async (args = Deno.args) => {
  const flags = Flags.parse(args, {
    string: ["out", "version", "id"],
    boolean: ["clear"],
    default: {
      out: "bundle",
    },
  });

  const metadataFlagHelper = new MetadataFlagHelper(flags);
  const bundleFlagHelper = new BundleFlagHelper(flags,metadataFlagHelper);
  const nameFlagHelper = new NameFlagHelper(flags, metadataFlagHelper);

  const outDir = path.resolve(Deno.cwd(), flags.out);
  if (flags.clear) {
    fs.rmSync(outDir, { recursive: true });
  }
  if (fs.existsSync(outDir)) {
    if (fs.statSync(outDir).isDirectory() === false) {
      throw new Error(`output should be an directory`);
    }
  } else {
    fs.mkdirSync(outDir, { recursive: true });
  }

  /// 写入metadata.json
  fs.writeFileSync(
    path.resolve(outDir, nameFlagHelper.metadataName),
    JSON.stringify(metadataFlagHelper.readMetadata(), null, 2)
  );
  /// 写入bundle.zip
  fs.writeFileSync(
    path.resolve(outDir, nameFlagHelper.bundleName),
    await (
      await bundleFlagHelper.bundleZip()
    ).generateAsync({ type: "nodebuffer" })
  );
  /// jszip 会导致程序一直开着，需要手动关闭
  Deno.exit();
};
if (import.meta.main) {
  await doBundle();
}

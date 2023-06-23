import crypto from "node:crypto";
import * as fs from "node:fs";
import * as path from "node:path";
import { Flags } from "../deps.ts";
import {
  BundleZipGenerator,
  MetadataJsonGenerator,
  NameFlagHelper,
} from "./helper/generator.ts";

/**
 * --out 指定输出目录(可选)
 * --version 指定app版本(可选)
 * --id 指定 appId(可选)
 * --dir 指定项目根目录(可选)
 */
export const doBundle = async (args = Deno.args) => {
  const flags = Flags.parse(args, {
    string: ["out", "version", "id", "dir"],
    boolean: ["clear", "dev"],
    default: {
      out: "bundle",
    },
  });

  const metadataFlagHelper = new MetadataJsonGenerator(flags);
  const id = metadataFlagHelper.readMetadata().id;
  const bundleFlagHelper = new BundleZipGenerator(flags, id);
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

  /// 先写入bundle.zip
  fs.writeFileSync(
    path.resolve(outDir, nameFlagHelper.bundleName),
    await (
      await bundleFlagHelper.bundleZip()
    ).generateAsync({ type: "nodebuffer" })
  );
  // 生成打包文件名称，大小
  const zip = await bundleFlagHelper.bundleZip(true);
  const zipData = await zip.generateAsync({ type: "uint8array" });
  const hasher = crypto.createHash("sha256").update(zipData);
  const metadata = metadataFlagHelper.readMetadata(true);
  metadata.bundle_size = zipData.byteLength;
  metadata.bundle_hash = "sha256:" + hasher.digest("hex");
  metadata.bundle_url = `./${nameFlagHelper.bundleName}`;
  /// 写入metadata.json
  fs.writeFileSync(
    path.resolve(outDir, nameFlagHelper.metadataName),
    JSON.stringify(metadata, null, 2)
  );
  /// jszip 会导致程序一直开着，需要手动关闭
  Deno.exit();
};
if (import.meta.main) {
  await doBundle();
}

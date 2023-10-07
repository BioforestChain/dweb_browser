import crypto from "node:crypto";
import * as fs from "node:fs";
import * as path from "node:path";
import { Command } from "./deps.ts";
import { $BundleOptions } from "./helper/const.ts";
import { BundleZipGenerator, MetadataJsonGenerator, NameFlagHelper, PlaocJsonGenerator } from "./helper/generator.ts";

export const doBundleCommand = new Command()
  .arguments("<source:string>")
  .description("Packaged source code folder.")
  .option("-o --out <out:string>", "Directory for packaged output.",{
    default:"bundle"
  })
  .option("-v --version <version:string>", "Set app packaging version.")
  .option("-d --dir <dir:string>", "Root directory of the project, generally the same level as manifest.json.")
  .option("-c --clear <clear:boolean>", "Empty the cache.")
  .option("--id <id:string>", "set app id")
  .option("--dev <dev:boolean>", "Is it development mode.")
  .action((options, metadata) => {
    doBundle({ ...options, metadata });
  });

/**
 * --out 指定输出目录(可选)
 * --version 指定app版本(可选)
 * --id 指定 appId(可选)
 * --dir 指定项目根目录(可选)
 */
export const doBundle = async (flags: $BundleOptions) => {
  const metadataFlagHelper = new MetadataJsonGenerator(flags);
  const plaocHelper = new PlaocJsonGenerator(flags);
  const data = metadataFlagHelper.readMetadata();
  const bundleFlagHelper = new BundleZipGenerator(flags,plaocHelper, data.id,data.version);
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
    await (await bundleFlagHelper.bundleZip()).generateAsync({ type: "nodebuffer" })
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
  fs.writeFileSync(path.resolve(outDir, nameFlagHelper.metadataName), JSON.stringify(metadata, null, 2));
  /// jszip 会导致程序一直开着，需要手动关闭
  Deno.exit();
};

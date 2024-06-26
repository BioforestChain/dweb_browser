import { Command } from "./deps/cliffy.ts";
import { node_crypto, node_fs, node_path } from "./deps/node.ts";
import type { $BundleOptions } from "./helper/const.ts";
import {
  BackendServerGenerator,
  BundleZipGenerator,
  MetadataJsonGenerator,
  NameFlagHelper,
  PlaocJsonGenerator,
} from "./helper/generator.ts";

export const doBundleCommand = new Command()
  .arguments("<web_public:string>")
  .description("Packaged source code folder.")
  .option("-o --out <out:string>", "Output directory.", {
    default: "bundle",
  })
  .option("--id <id:string>", "Set app id")
  .option("-v --version <version:string>", "Set app packaging version.")
  .option(
    "-c --config-dir <config_dir:string>",
    "The config directory is set to automatically traverse upwards when searching for configuration files (manifest.json/plaoc.json). The default setting for the target directory is <web_public>"
  )
  .option("-s --web-server <serve:string>", "Specify the path of the programmable backend. ")
  .option("--clear <clear:boolean>", "Empty the cache.", { default: true })
  .option("-d --dev <dev:boolean>", "Enable development mode.")
  .action((options, arg1) => {
    doBundle({ ...options, webPublic: arg1 } satisfies $BundleOptions);
  });

/**
 * --out 指定输出目录(可选)
 * --version 指定app版本(可选)
 * --id 指定 appId(可选)
 * --dir 指定项目根目录(可选)
 */
export const doBundle = async (flags: $BundleOptions) => {
  const metadataFlagHelper = new MetadataJsonGenerator(flags);
  // 注入plaoc.json
  const plaocHelper = new PlaocJsonGenerator(flags);
  // 尝试注入可编程后端
  const injectServer = new BackendServerGenerator(flags);
  const data = metadataFlagHelper.readMetadata();
  const bundleFlagHelper = new BundleZipGenerator(flags, plaocHelper, injectServer, data.id);
  const nameFlagHelper = new NameFlagHelper(flags, metadataFlagHelper);

  const outDir = node_path.resolve(Deno.cwd(), flags.out);

  if (flags.clear && node_fs.existsSync(outDir)) {
    node_fs.rmSync(outDir, { recursive: true });
  }

  if (node_fs.existsSync(outDir)) {
    if (node_fs.statSync(outDir).isDirectory() === false) {
      throw new Error(`output should be an directory`);
    }
  } else {
    node_fs.mkdirSync(outDir, { recursive: true });
  }
  /// 先写入bundle.zip
  node_fs.writeFileSync(
    node_path.resolve(outDir, nameFlagHelper.bundleName),
    await (
      await bundleFlagHelper.bundleZip()
    ).generateAsync({ type: "nodebuffer", compression: "DEFLATE", compressionOptions: { level: 9 } })
  );
  // 生成打包文件名称，大小
  const zip = await bundleFlagHelper.bundleZip(true);
  const zipData = await zip.generateAsync({
    type: "uint8array",
    compression: "DEFLATE",
    compressionOptions: { level: 9 },
  });
  const hasher = node_crypto.createHash("sha256").update(zipData);
  const metadata = metadataFlagHelper.readMetadata(true);
  metadata.bundle_size = zipData.byteLength;
  metadata.bundle_hash = "sha256:" + hasher.digest("hex");
  metadata.bundle_url = `./${nameFlagHelper.bundleName}`;
  /// 写入metadata.json
  node_fs.writeFileSync(node_path.resolve(outDir, nameFlagHelper.metadataName), JSON.stringify(metadata, null, 2));
  /// jszip 会导致程序一直开着，需要手动关闭
  Deno.exit();
};

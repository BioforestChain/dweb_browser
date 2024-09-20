import { colors, Command } from "../deps/cliffy.ts";
import { node_crypto, node_fs, node_path } from "../deps/node.ts";
import { type $BundleOptions } from "../helper/const.ts";
import { BundleResourceNameHelper, injectPrepare, MetadataJsonGenerator } from "../helper/generator.ts";
import { verifySvg } from "./verify.ts";

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
  .option("--clear <clear:boolean>", "Empty the cache.", { default: true })
  // .option("-w --watch <dev:boolean>", "Enable serve mode.", { default: false })
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
  // 验证svg
  const svgPass = await verifySvg(flags.webPublic);
  if (!svgPass) {
    return;
  }
  // 构造生成metadata
  const metadataFlagHelper = new MetadataJsonGenerator(flags);
  // 注入可编程后端和plaoc.json 生产打包资源
  const { bundleFlagHelper, bundleResourceNameHelper } = injectPrepare(flags, metadataFlagHelper);
  // 指定输出目录
  const outDir = node_path.resolve(Deno.cwd(), flags.out);
  // 清空目录
  if (flags.clear && node_fs.existsSync(outDir)) {
    node_fs.rmSync(outDir, { recursive: true });
  }
  // 看看需不需要创建打包目录
  if (node_fs.existsSync(outDir)) {
    if (node_fs.statSync(outDir).isDirectory() === false) {
      throw new Error(`output should be an directory`);
    }
  } else {
    node_fs.mkdirSync(outDir, { recursive: true });
  }

  // 重新构造出zip对象
  const zip = await bundleFlagHelper.bundleZip(true);
  const zipData = await zip.generateAsync({
    type: "uint8array",
    compression: "DEFLATE",
    compressionOptions: { level: 9 },
  });
  /// 写入bundle.zip
  node_fs.writeFileSync(node_path.resolve(outDir, bundleResourceNameHelper.bundleName()), zipData);

  const hasher = node_crypto.createHash("sha256").update(zipData);
  const metadata = metadataFlagHelper.readMetadata(true);
  metadata.bundle_size = zipData.byteLength;
  metadata.bundle_hash = "sha256:" + hasher.digest("hex");
  metadata.bundle_url = `./${bundleResourceNameHelper.bundleName()}`;
  /// 写入metadata.json
  node_fs.writeFileSync(
    node_path.resolve(outDir, BundleResourceNameHelper.metadataName),
    JSON.stringify(metadata, null, 2)
  );

  console.log(colors.green(`✅ bundle ${metadata.id} success  version:${metadata.version}`));
  /// jszip 会导致程序一直开着，需要手动关闭
  Deno.exit();
};

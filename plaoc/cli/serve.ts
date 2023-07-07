import crypto from "node:crypto";
import fs from "node:fs";
import http from "node:http";
import os from "node:os";
import path from "node:path";
import { fileURLToPath } from "node:url";
import adp from "npm:appdata-path";
import chalk from "npm:chalk";
import { Flags, debounce } from "./deps.ts";
import { WalkFiles } from "./helper/WalkDir.ts";
import { fileHasChange } from "./helper/fileHasChange.ts";
import {
  BundleZipGenerator,
  MetadataJsonGenerator,
  NameFlagHelper,
} from "./helper/generator.ts";
import { staticServe } from "./helper/http-static-helper.ts";

export const doServe = async (args = Deno.args) => {
  const flags = Flags.parse(args, {
    string: ["port", "mode"],
    boolean: ["dev"],
    collect: ["metadata"],
    default: {
      port: 8096,
      dev: true,
    },
  });

  const port = +flags.port;

  if (Number.isFinite(port) === false) {
    throw new Error(`need input '--port 8080'`);
  }

  const serveTarget = flags._.slice().shift();
  if (typeof serveTarget !== "string") {
    throw new Error(`need input 'YOUR/FOLDER/FOR/BUNDLE'`);
  }

  const metadataFlagHelper = new MetadataJsonGenerator(flags);
  const id = metadataFlagHelper.readMetadata().id;
  const bundleFlagHelper = new BundleZipGenerator(flags, id);
  const nameFlagHelper = new NameFlagHelper(flags, metadataFlagHelper);

  /// 启动http服务器
  http
    .createServer(async (req, res) => {
      console.log(chalk.blue(req.method), chalk.green(req.url));
      try {
        const url = new URL(req.url ?? "/", "http://localhost");
        if (url.pathname === "/" + nameFlagHelper.bundleName) {
          res.setHeader("Content-Type", nameFlagHelper.bundleMime);
          /// 尝试读取上次 metadata.json 生成的 zip 文件
          const zip = await bundleFlagHelper.bundleZip();
          zip
            .generateNodeStream({ compression: "STORE" })
            .pipe(res as NodeJS.WritableStream);
          return;
        } else if (url.pathname === "/" + nameFlagHelper.metadataName) {
          /// 动态生成 合成 metadata
          res.setHeader("Content-Type", nameFlagHelper.metadataMime);
          /// 每次请求的 metadata.json 的时候，我们强制重新生成 metadata 与 zip 文件
          const metadata = metadataFlagHelper.readMetadata(true);
          const zip = await bundleFlagHelper.bundleZip(true);

          const zipData = await zip.generateAsync({ type: "uint8array" });
          const hasher = crypto.createHash("sha256").update(zipData);
          metadata.bundle_size = zipData.byteLength;
          metadata.bundle_hash = "sha256:" + hasher.digest("hex");
          metadata.bundle_url = `./${nameFlagHelper.bundleName}`;

          res.setHeader("Access-Control-Allow-Origin", "*");
          res.setHeader("Access-Control-Allow-Headers", "*");
          res.setHeader("Access-Control-Allow-Methods", "*");
          res.end(JSON.stringify(metadata, null, 2));
          return;
        }

        if (bundleFlagHelper.www_dir) {
          await staticServe(bundleFlagHelper.www_dir, req, res);
        } else {
          res.statusCode = 502;
          res.end();
        }
      } catch (err) {
        res.statusCode = 500;
        const html = String.raw;
        res.setHeader("Content-Type", "text/html");
        res.end(
          html`<h1 style="color:red">${err.message}</h1>
            <hr />
            <pre>${err.stack}</pre>`
        );
      }
    })
    .listen(port, () => {
      for (const info of Object.values(os.networkInterfaces())
        .flat()
        .filter((info) => info?.family === "IPv4")) {
        console.log(
          `metadata: \thttp://${info?.address}:${port}/${nameFlagHelper.metadataName}`
        );
        // console.log(`package: \thttp://${info?.address}:${port}/${nameFlagHelper.bundleName}`)
      }
    })
    .on("close", () => {
      Deno.exit(1);
    });

  /// 将文件直接同步到jmm-apps
  const ROOT_PACKAGE = await import("../../package.json", {
    assert: { type: "json" },
  });

  const CUR_ENV_PATH = import.meta.url;
  /// 这里是判定是否是面向plaoc的框架开发者在开发emulator，方便他们将emulator同步到目标应用目录
  if (
    CUR_ENV_PATH.endsWith(".ts") &&
    /// 通过 npm 被安装
    CUR_ENV_PATH.includes("/node_module/") === false &&
    /// 通过 deno 的 npm: 协议被安装
    CUR_ENV_PATH.includes("/deno/npm/") === false
  ) {
    const emulatorSrcDir = fileURLToPath(
      import.meta.resolve("../dist/server/emulator")
    );
    if (fs.existsSync(emulatorSrcDir)) {
      const emulatorDestDir = path.join(
        adp.getAppDataPath(ROOT_PACKAGE.default.productName),
        "jmm-apps",
        id,
        "usr/server/emulator"
      );
      console.log(
        chalk.grey(
          `${chalk.bgBlue(
            "ℹ️"
          )}  由于您是plaoc的内部开发者，所以现在 ${chalk.underline.cyan(
            path.relative(Deno.cwd(), emulatorSrcDir)
          )} 会被自动同步到 ${chalk.underline.cyan(
            JSON.stringify(emulatorDestDir)
          )}。也就是说，手动刷新页面就可以看到 emulator 项目实时编译结果`
        )
      );

      const doSync = debounce(() => {
        if (fs.existsSync(emulatorDestDir)) {
          fs.rmSync(emulatorDestDir, { recursive: true });
        }
        for (const entry of WalkFiles(emulatorSrcDir)) {
          const src = path.resolve(emulatorSrcDir, entry.relativepath);
          const dest = path.resolve(emulatorDestDir, entry.relativepath);
          fs.mkdirSync(path.dirname(dest), { recursive: true });
          fs.copyFileSync(src, dest);
        }
        console.log(chalk.green("synced"));
      }, 300);

      fs.watch(emulatorSrcDir, { recursive: true }, (_type, filename) => {
        if (fileHasChange(filename)) {
          doSync();
        }
      });
      doSync();
    }
  }
};

if (import.meta.main) {
  doServe();
}

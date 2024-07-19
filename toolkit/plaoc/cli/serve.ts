import crypto from "node:crypto";
import fs from "node:fs";
import http from "node:http";
import os from "node:os";
import { generate, QRErrorCorrectLevel } from "npm:ts-qrcode-terminal";
import { colors, Command, EnumType, NumberPrompt } from "./deps/cliffy.ts";
import { SERVE_MODE, type $ServeOptions } from "./helper/const.ts";
import {
  BackendServerGenerator,
  BundleZipGenerator,
  MetadataJsonGenerator,
  NameFlagHelper,
  PlaocJsonGenerator,
} from "./helper/generator.ts";
import { staticServe } from "./helper/http-static-helper.ts";

const serveMode = new EnumType(SERVE_MODE);

export const doServeCommand = new Command()
  .type("serveMode", serveMode)
  .arguments("<web_public:string>")
  .description("Developer Service Extension Directive.")
  .option("-p --port <port:string>", "Service port.", {
    default: "8096",
  })
  .option(
    "-c --config-dir <config_dir:string>",
    "The config directory is set to automatically traverse upwards when searching for configuration files (manifest.json/plaoc.json). The default setting for the target directory is <web_public>"
  )
  .option("-s --web-server <serve:string>", "Specify the path of the programmable backend. ")
  .option("-m --mode <mode:serveMode>", "The processing mode of the service.")
  .option("-d --dev <dev:boolean>", "Enable development mode.", {
    default: true,
  })
  .action(async (options, arg1) => {
    await startServe({ ...options, webPublic: arg1 } satisfies $ServeOptions);
  });

// deno-lint-ignore require-await
export const doServe = async (flags: $ServeOptions) => {
  const port = +flags.port;
  if (Number.isFinite(port) === false) {
    throw new Error(`need input '--port 8080'`);
  }

  const serveTarget = flags.webPublic;
  if (typeof serveTarget !== "string") {
    throw new Error(`need input 'YOUR/FOLDER/FOR/BUNDLE'`);
  }

  const metadataFlagHelper = new MetadataJsonGenerator(flags);
  // 获取manifest.json文件路径，用于监听变化时重启服务
  const menifestFilePath = metadataFlagHelper.metadataFilepaths.filter(
    (item) => item.endsWith("manifest.json") && fs.existsSync(item)
  )?.[0];
  // 注入plaoc.json
  const plaocHelper = new PlaocJsonGenerator(flags);
  // 尝试注入可编程后端
  const injectServer = new BackendServerGenerator(flags);
  const data = metadataFlagHelper.readMetadata();
  const bundleFlagHelper = new BundleZipGenerator(flags, plaocHelper, injectServer, data.id);
  const nameFlagHelper = new NameFlagHelper(flags, metadataFlagHelper);

  /// 启动http服务器
  const server = http
    .createServer(async (req, res) => {
      if (req.method && req.url) {
        console.log(colors.blue(req.method), colors.green(req.url));
      }
      try {
        const url = new URL(req.url ?? "/", "http://localhost");
        if (url.pathname === "/" + nameFlagHelper.bundleName) {
          res.setHeader("Content-Type", nameFlagHelper.bundleMime);
          /// 尝试读取上次 metadata.json 生成的 zip 文件
          const zip = await bundleFlagHelper.bundleZip();
          zip.generateNodeStream({ compression: "STORE" }).pipe(res as NodeJS.WritableStream);
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
          // metadata.bundle_signature =

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
    .listen(port, async () => {
      const map: string[] = [];
      let index = 0;
      for (const info of Object.values(os.networkInterfaces())
        .flat()
        .filter((info) => info?.family === "IPv4")) {
        console.log(
          `${colors.green(`${index++}:`)} \t ${
            colors.dim("dweb://install?url=") +
            colors.blue(colors.underline(`http://${info?.address}:${port}/${nameFlagHelper.metadataName}`))
          }`
        );
        map.push(`dweb://install?url=http://${info?.address}:${port}/${nameFlagHelper.metadataName}`);
        // console.log(`package: \thttp://${info?.address}:${port}/${nameFlagHelper.bundleName}`)
      }

      const selectNumber = await NumberPrompt.prompt({
        message: "Enter the corresponding number to generate a QR code.",
        default: 0,
      });

      const link = map[selectNumber];

      if (link) {
        generate(link, {
          small: true,
          qrErrorCorrectLevel: QRErrorCorrectLevel.L,
        });
      }
    });
  // .on("close", () => {
  //   Deno.exit(1);
  // });

  return { server, menifestFilePath };
};

const startServe = async (flags: $ServeOptions) => {
  const { server, menifestFilePath } = await doServe(flags);
  server.once("restart", () => {
    server.once("close", async () => {
      await startServe(flags);
    });
    server.close();
  });
  if (menifestFilePath)
    fs.watch(menifestFilePath, (eventname, filename) => {
      if (eventname === "change" && filename?.endsWith("manifest.json")) {
        // \x1b[3J 清除所有内容
        // \x1b[H 把光标移动到行首
        // \x1b[2J 清除所有内容
        console.log("\x1b[3J\x1b[H\x1b[2J");
        server.emit("restart", []);
      }
    });
};

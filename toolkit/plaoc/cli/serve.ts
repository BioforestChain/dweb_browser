import Mime from "mime";
import crypto from "node:crypto";
import fs from "node:fs";
import http from "node:http";
import os from "node:os";
import { generate, QRErrorCorrectLevel } from "npm:ts-qrcode-terminal";
import { colors, Command, NumberPrompt } from "./deps/cliffy.ts";
import { type $ServeOptions } from "./helper/const.ts";
import { BundleResourceNameHelper, injectPrepare, MetadataJsonGenerator } from "./helper/generator.ts";
import { staticServe } from "./helper/http-static-helper.ts";

export const doServeCommand = new Command()
  .arguments("<link:string>")
  .description("Developer Service Extension Directive.")
  .option("-p --port <port:string>", "Specify the service port. default:8096.", {
    default: "8096",
  })
  .option(
    "-c --config-dir <config_dir:string>",
    "The config directory is set to automatically traverse upwards when searching for configuration files (manifest.json/plaoc.json). The default setting for the target directory is <web_public>"
  )
  .option("-s --web-server <serve:string>", "Specify the path of the programmable backend. ")
  .action((options, arg1) => {
    startServe({ ...options, webPublic: arg1 } satisfies $ServeOptions);
  });

const doServe = (flags: $ServeOptions) => {
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
  const manifestFilePath = metadataFlagHelper.metadataFilepaths.filter(
    (item) => item.endsWith(BundleResourceNameHelper.metadataName) && fs.existsSync(item)
  )?.[0];

  const { bundleFlagHelper, bundleResourceNameHelper } = injectPrepare(flags, metadataFlagHelper);
  /// 启动http服务器
  const server = http.createServer().listen(port, "0.0.0.0", async () => {
    const dwebLinks: string[] = [];
    let index = 0;
    for (const info of Object.values(os.networkInterfaces())
      .flat() // 返回一个新数组，其中所有子数组元素都以递归方式连接到其中，直到指定的深度。
      .filter((info) => info?.family === "IPv4")) {
      const hostname = info?.address ?? "";
      console.log(
        `${colors.green(`${index++}:`)} \t ${
          colors.dim("dweb://install?url=") +
          colors.blue(colors.underline(`http://${hostname}:${port}/${BundleResourceNameHelper.metadataName}`))
        }`
      );
      dwebLinks.push(`dweb://install?url=http://${hostname}:${port}/${BundleResourceNameHelper.metadataName}`);
    }
    const selectNumber = await NumberPrompt.prompt({
      message: "Enter the corresponding number to generate a QR code.",
      default: 0,
    });
    const dwebLink = dwebLinks[selectNumber];
    if (dwebLink) {
      generate(dwebLink, {
        small: true,
        qrErrorCorrectLevel: QRErrorCorrectLevel.L,
      });
    }
  });
  server.on("request", async (req, res) => {
    if (req.method && req.url) {
      console.log(colors.blue(req.method), colors.green(req.url));
    }
    try {
      const url = new URL(req.url ?? "/", "http://localhost");
      if (url.pathname === "/" + bundleResourceNameHelper.bundleName()) {
        res.setHeader("Content-Type", Mime.getType(bundleResourceNameHelper.bundleName())!);
        /// 尝试读取上次 metadata.json 生成的 zip 文件
        const zip = await bundleFlagHelper.bundleZip();
        zip.generateNodeStream({ compression: "STORE" }).pipe(res as NodeJS.WritableStream);
        return;
      } else if (url.pathname === "/" + BundleResourceNameHelper.metadataName) {
        /// 动态生成 合成 metadata
        res.setHeader("Content-Type", Mime.getType(BundleResourceNameHelper.metadataName)!);
        /// 每次请求的 metadata.json 的时候，我们强制重新生成 metadata 与 zip 文件
        const metadata = metadataFlagHelper.readMetadata(true);
        const zip = await bundleFlagHelper.bundleZip(true);

        const zipData = await zip.generateAsync({ type: "uint8array" });
        const hasher = crypto.createHash("sha256").update(zipData);
        metadata.bundle_size = zipData.byteLength;
        metadata.bundle_hash = "sha256:" + hasher.digest("hex");
        metadata.bundle_url = `./${bundleResourceNameHelper.bundleName()}`;
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
  });

  return { server, manifestFilePath };
};

export const startServe = (flags: $ServeOptions) => {
  const { server, manifestFilePath } = doServe(flags);
  server.once("restart", () => {
    server.once("close", () => {
      startServe(flags);
    });
    server.close();
  });
  if (manifestFilePath)
    fs.watch(manifestFilePath, (eventname, filename) => {
      if (eventname === "change" && filename?.endsWith("manifest.json")) {
        // \x1b[3J 清除所有内容
        // \x1b[H 把光标移动到行首
        // \x1b[2J 清除所有内容
        console.log("\x1b[3J\x1b[H\x1b[2J");
        server.emit("restart", []);
      }
    });
};

import http from "node:http";
import os from "node:os";
import { Command, EnumType, colors, createHash } from "./deps.ts";
import { $ServeOptions, SERVE_MODE } from "./helper/const.ts";
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
  .arguments("<metadata:string>")
  .description("Developer Service Extension Directive.")
  .option("-p --port <port:string>", "service port.", {
    default: "8096",
  })
  .option("-s --serve <serve:string>", "Specify the path of the programmable backend. ")
  .option("-d --dir <dir:string>", "Root directory of the project, generally the same level as manifest.json.")
  .option("-m --mode <mode:serveMode>", "The processing mode of the service.")
  .option("--dev <dev:boolean>", "Is it development mode.", {
    default: true,
  })
  .action((options, metadata) => {
    doServe({ ...options, metadata });
  });

export const doServe = async (flags: $ServeOptions) => {
  const port = +flags.port;

  if (Number.isFinite(port) === false) {
    throw new Error(`need input '--port 8080'`);
  }

  const serveTarget = flags.metadata;
  if (typeof serveTarget !== "string") {
    throw new Error(`need input 'YOUR/FOLDER/FOR/BUNDLE'`);
  }

  const metadataFlagHelper = new MetadataJsonGenerator(flags);
  // 注入plaoc.json
  const plaocHelper = new PlaocJsonGenerator(flags);
  // 尝试注入可编程后端
  const injectServer = new BackendServerGenerator(flags);
  const data = metadataFlagHelper.readMetadata();
  const bundleFlagHelper = new BundleZipGenerator(flags, plaocHelper, injectServer, data.id);
  const nameFlagHelper = new NameFlagHelper(flags, metadataFlagHelper);

  /// 启动http服务器
  http
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
          const hasher = createHash("sha256").update(zipData);
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
    .listen(port, () => {
      for (const info of Object.values(os.networkInterfaces())
        .flat()
        .filter((info) => info?.family === "IPv4")) {
        console.log(`metadata: \thttp://${info?.address}:${port}/${nameFlagHelper.metadataName}`);
        // console.log(`package: \thttp://${info?.address}:${port}/${nameFlagHelper.bundleName}`)
      }
    })
    .on("close", () => {
      Deno.exit(1);
    });
};

import crypto from "node:crypto";
import http from "node:http";
import os from "node:os";
import { Flags } from "../deps.ts";
import {
  BundleZipGenerator,
  MetadataJsonGenerator,
  NameFlagHelper,
} from "./helper/generator.ts";
import { staticServe } from "./helper/http-static-helper.ts";

export const doServe = (args = Deno.args) => {
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

  http
    .createServer(async (req, res) => {
      if (req.url === "/" + nameFlagHelper.bundleName) {
        res.setHeader("Content-Type", nameFlagHelper.bundleMime);
        /// 尝试读取上次 metadata.json 生成的 zip 文件
        const zip = await bundleFlagHelper.bundleZip();
        zip
          .generateNodeStream({ compression: "STORE" })
          .pipe(res as NodeJS.WritableStream);
        return;
      } else if (req.url === "/" + nameFlagHelper.metadataName) {
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

        res.end(JSON.stringify(metadata, null, 2));
        return;
      }

      if (bundleFlagHelper.www_dir) {
        await staticServe(bundleFlagHelper.www_dir, req, res);
      } else {
        res.statusCode = 502;
        res.end();
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
    });
};

if (import.meta.main) {
  doServe();
}

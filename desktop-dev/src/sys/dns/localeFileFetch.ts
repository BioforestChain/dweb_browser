import fs from "node:fs";
import path from "node:path";
import { Readable } from "node:stream";
import mime from "npm:mime";
import { ROOT } from "../../helper/createResolveTo.ts";
import { nativeFetchAdaptersManager } from "./nativeFetch.ts";

nativeFetchAdaptersManager.append(async (remote, parsedUrl) => {
  /// fetch("file:///sys") 匹配
  if (
    parsedUrl.protocol === "file:" &&
    parsedUrl.hostname === "" &&
    parsedUrl.pathname.startsWith("/sys/")
  ) {
    try {
      const filepath = ROOT + parsedUrl.pathname.replace("/sys/", "/assets/");
      const stats = await fs.statSync(filepath);
      if (stats.isDirectory()) {
        throw stats;
      }
      const ext = path.extname(filepath);
      return new Response(Readable.toWeb(fs.createReadStream(filepath)), {
        status: 200,
        headers: {
          "Content-Length": stats.size + "",
          "Content-Type": mime.getType(ext) || "application/octet-stream",
        },
      });
    } catch (err) {
      return new Response(String(err), { status: 404 });
    }
  }
}, -1);

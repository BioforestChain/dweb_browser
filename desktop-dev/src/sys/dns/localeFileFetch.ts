import mime from "mime";
import fs from "node:fs";
import path from "node:path";
import { Readable } from "node:stream";
import { resolveToRoot } from "../../helper/createResolveTo.ts";
import { nativeFetchAdaptersManager } from "./nativeFetch.ts";

nativeFetchAdaptersManager.append(async (remote, parsedUrl) => {
  /// fetch("file:///sys") 匹配
  console.always('接受到了查询本地文件的服务', parsedUrl)
  if (
    parsedUrl.protocol === "file:" &&
    parsedUrl.hostname === "" &&
    parsedUrl.pathname.startsWith("/sys/")
  ) {
    try {
      /// 读取 app.asar 里头的文件
      console.always("path.dirnaem", path.resolve(__dirname))
      const filepath = path.resolve(__dirname, resolveToRoot(
        parsedUrl.pathname.replace("/sys/", "/assets/")
      ));
      console.always('filepath: ', filepath)
      const stats = await fs.statSync(filepath);
      if (stats.isDirectory()) {
        throw stats;
      }
      console.always('filepath: ', filepath)
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

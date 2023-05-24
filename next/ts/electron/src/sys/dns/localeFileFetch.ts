import mime from "mime";
import fs from "fs";
import path from "path";
import { Readable } from "stream";
import { ROOT } from "../../helper/createResolveTo.js";
import { nativeFetchAdaptersManager } from "./nativeFetch.js";

nativeFetchAdaptersManager.append(async (remote, parsedUrl) => {
  /// fetch("file:///*") 匹配
  // console.log('[localFileFetch.cts] parsedUrl ', parsedUrl, " --- ", parsedUrl.protocol === "file:" && parsedUrl.hostname === "")
  if (parsedUrl.protocol === "file:" && parsedUrl.hostname === "") {
    return (async () => {
      try {
        const filepath = ROOT + parsedUrl.pathname;
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
    })();
  }
}, -1);

import mime from "mime";
import fs from "node:fs";
import path from "node:path";
import { Readable } from "node:stream";
import type { MicroModule } from "../../core/micro-module.cjs";
import { ROOT } from "../../helper/createResolveTo.cjs";

type $FetchAdapter = (
  remote: MicroModule,
  parsedUrl: URL,
  requestInit: RequestInit
) => Promise<Response | void>;

class NativeFetchAdaptersManager {
  private readonly adapterOrderMap = new Map<$FetchAdapter, number>();
  private orderdAdapters: $FetchAdapter[] = [];
  private _reorder() {
    this.orderdAdapters = [...this.adapterOrderMap]
      .sort((a, b) => a[1] - b[1])
      .map((a) => a[0]);
  }
  get adapters() {
    return this.orderdAdapters as ReadonlyArray<$FetchAdapter>;
  }
  append(adapter: $FetchAdapter, order = 0) {
    this.adapterOrderMap.set(adapter, order);
    this._reorder();
    return () => this.remove(adapter);
  }

  remove(adapter: $FetchAdapter) {
    if (this.adapterOrderMap.delete(adapter) != null) {
      this._reorder();
      return true;
    }
    return false;
  }
}

export const nativeFetchAdaptersManager = new NativeFetchAdaptersManager();

export const localeFileFetch: $FetchAdapter = async (remote, parsedUrl) => {
  /// fetch("file:///*")
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
};

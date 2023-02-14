import mime from "mime";
import fs from "node:fs";
import path from "node:path";
import { Readable } from "node:stream";
import { fileURLToPath } from "node:url";
import type { Ipc } from "../../core/ipc/ipc.cjs";
import { MicroModule } from "../../core/micro-module.cjs";
import { $readRequestAsIpcRequest } from "../../helper/$readRequestAsIpcRequest.cjs";
import { normalizeFetchArgs } from "../../helper/normalizeFetchArgs.cjs";
import type { $PromiseMaybe } from "../../helper/types.cjs";
import type { DnsNMM } from "./dns.cjs";

/**
 * 重写全局 fetch 函数，使得支持 fetch("file://*.dweb/*") 与 fetch("file:///*")
 */
export const hookFetch = (app_mm: DnsNMM) => {
  const connects = new WeakMap<
    MicroModule,
    Map<
      $MMID,
      $PromiseMaybe<{
        ipc: Ipc;
      }>
    >
  >();
  const native_fetch = globalThis.fetch;
  mime.define({
    "application/javascript": [".mjs", ".cjs", ".jsx"],
  });
  globalThis.fetch = function fetch(
    this: unknown,
    url: RequestInfo | URL,
    init?: RequestInit
  ) {
    /// 如果上下文是 MicroModule，那么进入特殊的解析模式
    if (this instanceof MicroModule) {
      const from_app = this;
      const args = normalizeFetchArgs(url, init);
      const { parsed_url } = args;
      /// fetch("file://*.dweb/*")
      if (
        parsed_url.protocol === "file:" &&
        parsed_url.hostname.endsWith(".dweb")
      ) {
        const mmid = parsed_url.hostname as $MMID;
        /// 拦截到了，走自定义总线
        let from_app_ipcs = connects.get(from_app);
        if (from_app_ipcs === undefined) {
          from_app_ipcs = new Map();
          connects.set(from_app, from_app_ipcs);
        }

        /// 与指定应用建立通讯
        let ipc_promise = from_app_ipcs.get(mmid);
        if (ipc_promise === undefined) {
          /// 初始化互联
          ipc_promise = (async () => {
            const app = await app_mm.open(parsed_url.hostname as $MMID);
            const ipc = await app.connect(from_app);
            // 监听生命周期 释放引用
            ipc.onClose(() => {
              from_app_ipcs?.delete(mmid);
            });
            return {
              ipc,
            };
          })();
          from_app_ipcs.set(mmid, ipc_promise);
        }

        return (async () => {
          const { ipc } = await ipc_promise;
          const ipc_req_init = await $readRequestAsIpcRequest(
            args.request_init
          );
          const ipc_response = await ipc.request(parsed_url.href, ipc_req_init);

          return ipc_response.asResponse(parsed_url.href);
        })();
      }
      /// fetch("file:///*")
      if (parsed_url.protocol === "file:" && parsed_url.hostname === "") {
        return (async () => {
          try {
            const filepath = fileURLToPath(parsed_url);
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
    }

    return native_fetch(url, init);
  };
};

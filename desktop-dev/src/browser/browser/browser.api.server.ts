import {
  Ipc,
  IpcEvent,
  IpcHeaders,
  IpcRequest,
  IpcResponse,
} from "../../core/ipc/index.ts";
import { $MMID, $Schema1ToType } from "../../helper/types.ts";
import { createHttpDwebServer } from "../../sys/http-server/$createHttpDwebServer.ts";
import { getAllApps } from "../jmm/jmm.api.serve.ts";
import { JsMMMetadata, JsMicroModule } from "../jmm/micro-module.js.ts";
import type { BrowserNMM } from "./browser.ts";

export async function createAPIServer(this: BrowserNMM) {
  // 为 下载页面做 准备
  this.apiServer = await createHttpDwebServer(this, {
    subdomain: "api",
    port: 433,
  });
  // 数据体
  // ServerUrlInfo {
  //   host: 'api.browser.dweb:433',
  //   internal_origin: 'http://api.browser.dweb-433.localhost:22605',
  //   public_origin: 'http://localhost:22605'
  // }
  const apiReadableStreamIpc = await this.apiServer.listen();
  apiReadableStreamIpc.onRequest(onRequest.bind(this));
}

async function onRequest(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
  let href = request.parsed_url.href;
  // dweb_deeplink 请求
  if (request.parsed_url.protocol === "dweb:") {
    return this.nativeFetch(href)
  }
  const mmid = request.parsed_url.searchParams.get("mmid")
  if (mmid)  {
    href = href.replace("browser.dweb",mmid)
  }
  return this.nativeFetch(href)
}

export async function getAppsInfo(
  this: BrowserNMM,
  // deno-lint-ignore ban-types
  _args: $Schema1ToType<{}>,
  ipc: Ipc,
  request: IpcRequest
) {
  const appsInfo = await getAllApps();
  ipc.postMessage(
    IpcResponse.fromText(
      request.req_id,
      200,
      undefined,
      JSON.stringify(appsInfo),
      ipc
    )
  );
}

export async function updateContent(
  this: BrowserNMM,
  // deno-lint-ignore ban-types
  _args: $Schema1ToType<{}>,
  ipc: Ipc,
  request: IpcRequest
) {
  const href = request.parsed_url.searchParams.get("url");
  if (href === null) {
    ipc.postMessage(
      IpcResponse.fromText(request.req_id, 400, undefined, "缺少 url 参数", ipc)
    );
    return;
  }
  const regexp =
    /^(https?|http):\/\/([a-z0-9-]+(.[a-z0-9-]+)*(:[0-9]+)?)(\/.*)?$/i;
  if (regexp.test(href)) {
    this.contentBV.loadWithHistory(href);
    ipc.postMessage(
       IpcResponse.fromText(request.req_id, 200, undefined, "ok", ipc)
    );
    return;
  }
  ipc.postMessage(
    IpcResponse.fromText(
      request.req_id,
      400,
      undefined,
      "非法的 url 参数:" + href,
      ipc
    )
  );
}

export async function canGoBack(
  this: BrowserNMM,
  // deno-lint-ignore ban-types
  _args: $Schema1ToType<{}>,
  ipc: Ipc,
  request: IpcRequest
) {
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id,
      200,
      new IpcHeaders({
        "Content-Type": "application/json",
      }),
      JSON.stringify({
        value: this.contentBV.canGoBack(),
      }),
      ipc
    )
  );
}

export async function canGoForward(
  this: BrowserNMM,
  // deno-lint-ignore ban-types
  _args: $Schema1ToType<{}>,
  ipc: Ipc,
  request: IpcRequest
) {
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id,
      200,
      new IpcHeaders({
        "Content-Type": "application/json",
      }),
      JSON.stringify({
        value: this.contentBV.canGoForward(),
      }),
      ipc
    )
  );
}

export async function goBack(
  this: BrowserNMM,
  // deno-lint-ignore ban-types
  _args: $Schema1ToType<{}>,
  ipc: Ipc,
  request: IpcRequest
) {
  this.contentBV.goBack();
  ipc.postMessage(
     IpcResponse.fromText(
      request.req_id,
      200,
      new IpcHeaders({
        "Content-Type": "application/json",
      }),
      JSON.stringify({
        value: "ok",
      }),
      ipc
    )
  );
}

export async function goForward(
  this: BrowserNMM,
  // deno-lint-ignore ban-types
  _args: $Schema1ToType<{}>,
  ipc: Ipc,
  request: IpcRequest
) {
  this.contentBV.goForward();
  ipc.postMessage(
    IpcResponse.fromText(
      request.req_id,
      200,
      new IpcHeaders({
        "Content-Type": "application/json",
      }),
      JSON.stringify({
        value: "ok",
      }),
      ipc
    )
  );
}

export async function refresh(
  this: BrowserNMM,
  // deno-lint-ignore ban-types
  _args: $Schema1ToType<{}>,
  ipc: Ipc,
  request: IpcRequest
) {
  try {
    this.contentBV.reload();
  } catch (err) {
    console.error("error", err);
    throw new Error(`refresh err`);
  }

  ipc.postMessage(
    IpcResponse.fromText(
      request.req_id,
      200,
      new IpcHeaders({
        "Content-Type": "application/json",
      }),
      JSON.stringify({
        value: "ok",
      }),
      ipc
    )
  );
}

export async function openApp(
  this: BrowserNMM,
  // deno-lint-ignore ban-types
  _args: $Schema1ToType<{}>,
  ipc: Ipc,
  request: IpcRequest
) {
  const searchParams = request.parsed_url.searchParams;
  const mmid = searchParams.get("app_id") as $MMID | null;
  const postMessage = async (statusCode: number, message: string) => {
    ipc.postMessage(
      await IpcResponse.fromText(
        request.req_id,
        statusCode,
        new IpcHeaders({
          "Content-Type": "application/json",
        }),
        JSON.stringify({
          value: message,
        }),
        ipc
      )
    );
  };

  if (mmid === null) {
    return postMessage(400, "缺少 app_id 参数");
  }

  // 测试 std 结束
  if (mmid === "app.std.dweb") {
    console.always("属于 std");

    const jsMM = new JsMicroModule(
      new JsMMMetadata({
        id: mmid as $MMID,
        server: {
          root: "/usr",
          entry: "server/std.server.js",
        },
      })
    );

    // 需要检查是否已经安装了应用 如果已经安装了就不要再安装了
    // 还需要判断 应用是否已经更新了

    this.context?.dns.install(jsMM);
    const [jsIpc] = await this.context?.dns.connect(mmid as $MMID)!;
    // 如果 对应app的全部 devTools 中有没有关闭的，就无法再次打开
    console.always("jsIpc: ", jsIpc.remote.mmid);
    jsIpc.postMessage(IpcEvent.fromText("activity", ""));
    console.always("activity", mmid);
    return postMessage(200, "o,");
  }

  // 应该到jmm去找
  const microModule = (await this.context?.dns.query(mmid)) as
    | JsMicroModule
    | undefined;
  if (!microModule) {
    return postMessage(400, "不存在该app");
  }
  const { root, entry } = microModule.metadata.config.server;
  const jsMM = new JsMicroModule(
    new JsMMMetadata({
      id: mmid as $MMID,
      server: {
        root,
        entry,
      },
    })
  );

  // 需要检查是否已经安装了应用 如果已经安装了就不要再安装了
  // 还需要判断 应用是否已经更新了

  this.context?.dns.install(jsMM);
  const [jsIpc] = await this.context?.dns.connect(mmid as $MMID)!;
  // 如果 对应app的全部 devTools 中有没有关闭的，就无法再次打开
  jsIpc.postMessage(IpcEvent.fromText("activity", ""));
  console.always("activity", mmid);
  return postMessage(200, "o,");
}

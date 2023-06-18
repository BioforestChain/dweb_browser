import {
  Ipc,
  IpcEvent,
  IpcHeaders,
  IpcRequest,
  IpcResponse,
} from "../../core/ipc/index.ts";
import { $MMID } from "../../helper/types.ts";
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
  const pathname = request.parsed_url.pathname;
  switch (pathname) {
    case "/appsInfo":
      getAppsInfo.bind(this)(request, ipc);
      break;
    case "/update/content":
      updateContent.bind(this)(request, ipc);
      break;
    case "/can-go-back":
      canGoBack.bind(this)(request, ipc);
      break;
    case "/can-go-forward":
      canGoForward.bind(this)(request, ipc);
      break;
    case "/go-back":
      goBack.bind(this)(request, ipc);
      break;
    case "/go-forward":
      goForward.bind(this)(request, ipc);
      break;
    case "/refresh":
      refresh.bind(this)(request, ipc);
      break;
    case "/external":
      external.bind(this)(request, ipc);
      break;
    case "/openApp":
      open.bind(this)(request, ipc);
      break;
    default:
      console.error(
        "browser",
        "还有没有匹配的 api 请求 pathname ===",
        pathname
      );
  }
}

async function getAppsInfo(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
  const appsInfo = await getAllApps();
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id,
      200,
      undefined,
      JSON.stringify(appsInfo),
      ipc
    )
  );
}

async function updateContent(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
  const href = request.parsed_url.searchParams.get("url");
  if (href === null) {
    ipc.postMessage(
      await IpcResponse.fromText(
        request.req_id,
        400,
        undefined,
        "缺少 url 参数",
        ipc
      )
    );
    return;
  }
  const regexp =
    /^(https?|http):\/\/([a-z0-9-]+(.[a-z0-9-]+)*(:[0-9]+)?)(\/.*)?$/i;
  if (regexp.test(href)) {
    this.contentBV!.loadWithHistory(href);
    ipc.postMessage(
      await IpcResponse.fromText(request.req_id, 200, undefined, "ok", ipc)
    );
    return;
  }
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id,
      400,
      undefined,
      "非法的 url 参数:" + href,
      ipc
    )
  );
}

async function canGoBack(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id,
      200,
      new IpcHeaders({
        "Content-Type": "application/json",
      }),
      JSON.stringify({
        value: this.contentBV!.canGoBack(),
      }),
      ipc
    )
  );
}

async function canGoForward(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
  ipc.postMessage(
    await IpcResponse.fromText(
      request.req_id,
      200,
      new IpcHeaders({
        "Content-Type": "application/json",
      }),
      JSON.stringify({
        value: this.contentBV!.canGoForward(),
      }),
      ipc
    )
  );
}

async function goBack(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
  this.contentBV!.goBack();
  ipc.postMessage(
    await IpcResponse.fromText(
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

async function goForward(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
  this.contentBV!.goForward();
  ipc.postMessage(
    await IpcResponse.fromText(
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

async function refresh(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
  try {
    this.contentBV!.reload();
  } catch (err) {
    console.error("error", err);
    throw new Error(`refresh err`);
  }

  ipc.postMessage(
    await IpcResponse.fromText(
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

async function external(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
  const externalUrl = request.parsed_url.searchParams.get("url");
  if (externalUrl === null) {
    ipc.postMessage(
      await IpcResponse.fromText(
        request.req_id,
        400,
        undefined,
        "缺少 url 参数",
        ipc
      )
    );
    return;
  }
  const _url = new URL(externalUrl);

  // 下面的数据是测试数据实际操作不需要如下处理
  if (_url.hostname === "shop.plaoc.com" && _url.pathname.endsWith(".json")) {
    // 测试用的 http://127.0.0.1:8096/metadata.json 本地服务的地址
    // 连接 jmm 发起 request
    const [jmmIpc] = await this.context!.dns.connect("jmm.browser.dweb");
    await jmmIpc.request(
      // 需要通过 deep_link 启动 jmm
      `dweb:install?url=http://127.0.0.1:8096/metadata.json`
    );
    ipc.postMessage(
      await IpcResponse.fromText(request.req_id, 200, undefined, "ok", ipc)
    );
  }
}

async function open(this: BrowserNMM, request: IpcRequest, ipc: Ipc) {
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
  if(mmid === "app.std.dweb"){
    console.always('属于 std')

    const jsMM = new JsMicroModule(
      new JsMMMetadata({
        id: mmid as $MMID,
        server: {
          root: "/usr",
          entry: "server/std.server.js"
        }
      })
    )
  
    // 需要检查是否已经安装了应用 如果已经安装了就不要再安装了
    // 还需要判断 应用是否已经更新了 
  
    this.context?.dns.install(jsMM);
    const [jsIpc] = await this.context?.dns.connect(mmid as $MMID)!
    // 如果 对应app的全部 devTools 中有没有关闭的，就无法再次打开
    console.always('jsIpc: ', jsIpc.remote.mmid)
    jsIpc.postMessage(IpcEvent.fromText("activity", ""));
    console.always('activity', mmid)
    return postMessage(200, "o,")
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

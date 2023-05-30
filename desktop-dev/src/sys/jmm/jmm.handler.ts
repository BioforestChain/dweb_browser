import type { $Schema1ToType } from "../../helper/types.ts";
import type { JmmNMM } from "./jmm.ts";

/**
 * 功能：
 * 打开一个新的 webveiw 页面
 * @param this
 * @param args
 * @param client_ipc
 * @param ipcRequest
 * @returns
 */
export async function install(
  jmm: JmmNMM,
  args: $Schema1ToType<{ metadataUrl: "string" }>
) {
  // 需要同时查询参数传递进去
  if (jmm.wwwServer === undefined)
    throw new Error(`this.wwwServer === undefined`);
  const interUrl = jmm.wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
    url.pathname = "/index.html";
  }).href;
  const url = `file://mwebview.sys.dweb/open_new_webveiw_at_focused?url=${interUrl}`;
  const body = JSON.stringify({ metadataUrl: args.metadataUrl });
  await jmm.nativeFetch(url, {
    method: "POST",
    body,
  });
  return true;
}

export async function pause(jmm: JmmNMM, args: $Schema1ToType<{}>) {
  console.log("jmm","................ 下载暂停但是还没有处理");
  return true;
}

export async function resume(jmm: JmmNMM, args: $Schema1ToType<{}>) {
  console.log("jmm","................ 从新下载但是还没有处理");
  return true;
}

// 业务逻辑是会 停止下载 立即关闭下载页面
export async function cancel(jmm: JmmNMM, args: $Schema1ToType<{}>) {
  console.log("jmm","................ 从新下载但是还没有处理");
  return true;
}

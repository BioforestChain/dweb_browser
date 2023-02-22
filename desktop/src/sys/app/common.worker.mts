// 全部 dweb_app 提供 基础worker
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { createHttpDwebServer } from "../../sys/http-server/$listenHelper.cjs";
/**
 * 执行入口函数
 */
export const main = async () => {
    /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人KWKW
    // origin === http://app.w85defe5.dweb-80.localhost:22605
    const { origin, start } = await createHttpDwebServer(jsProcess, {});
    (await start()).onRequest(async (request, httpServerIpc) =>{
      console.log('common.worker.mts 接受到了请求 把请求转发出去 request', request)
      // request.parsed_url 可以拿到 w85defe5.app.dweb host
      // 把全部的请求发送给 app.sys.dweb 程序
      // app.sys.dweb 提供全全部的请求处理
      // 请求的处理必须要添加 appId
      const host = new URL(origin).host.split(".")
      const _url = `file://app.sys.dweb/server?url=${origin}${request.url}`
      console.log('[]common.worker.mts:_url: ', _url)
      const response = await jsProcess.fetch(_url)
      httpServerIpc.postMessage(
        await IpcResponse.fromResponse(
          request.req_id,
          response,
          httpServerIpc
        )
      );
    });
    console.log('执行了 common.worker.mts main 载入 html origin:', origin)
    // 这里有问题，不需要打开一个新的window
    await openIndexHtmlAtMWebview(origin)
};

main().catch(console.error);

  /**
 * 打开对应的 html
 * @param origin 
 * @returns 
 */
async function openIndexHtmlAtMWebview(origin:string){
  console.log('--------common.worker.mts, origin: ', origin)
  const view_id = await jsProcess
                        .fetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(origin)}`)
                        .text();
  return view_id
}
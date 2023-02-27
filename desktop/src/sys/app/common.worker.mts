// 全部 dweb_app 提供 基础worker
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { createHttpDwebServer } from "../../sys/http-server/$listenHelper.cjs";
 
/**
 * 执行入口函数
 */
export const main = async () => {
    /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人KWKW
    // origin === http://app.w85defe5.dweb-80.localhost:22605
    const { origin, listen } = await createHttpDwebServer(jsProcess, {});
    (await listen()).onRequest(async (request, httpServerIpc) =>{
   
      // request.parsed_url 可以拿到 w85defe5.app.dweb host
      // 把全部的请求发送给 app.sys.dweb 程序
      // app.sys.dweb 提供全全部的请求处理
      // 请求的处理必须要添加 appId
      const host = new URL(origin).host.split(".")
      const _url = `file://www.sys.dweb/server?url=${origin}${request.url}`
      const response = await jsProcess.fetch(_url)
      httpServerIpc.postMessage(
        await IpcResponse.fromResponse(
          request.req_id,
          response,
          httpServerIpc
        )
      );
    });
    
    await openIndexHtmlAtMWebview(origin)
};

main().catch(console.error);

  /**
 * 打开对应的 html
 * @param origin 
 * @returns 
 */
async function openIndexHtmlAtMWebview(origin:string){
  const view_id = await jsProcess
                        .fetch(`file://mwebview.sys.dweb/open?url=${encodeURIComponent(origin)}`)
                        .text();
  return view_id
}
// 全部 dweb_app 提供 基础worker
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { createHttpDwebServer } from "../../sys/http-server/$listenHelper.cjs";
/**
 * 执行入口函数
 */
export const main = async () => {
    /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人KWKW
    const { origin, start } = await createHttpDwebServer(jsProcess, {});
    (await start()).onRequest(async (request, httpServerIpc) =>{
      console.log('common.worker.mts 接受到了 request', request)
      // request.parsed_url 可以拿到 w85defe5.app.dweb host

      // 把全部的请求发送给 app.sys.dweb 程序
      // app.sys.dweb 提供全全部的请求处理
      // 请求的处理必须要添加 appId
      const _url = `file://app.sys.dweb/server?url=${request.url}`
      console.log('url: ', _url)
      jsProcess
      fetch(_url)
      .then(async(res: Response) => {
        console.log('打开了 指定的应用')
      })
    } );
    console.log('执行了 common.worker.mts main')
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
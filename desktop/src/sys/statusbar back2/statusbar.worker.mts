/// <reference path="../../sys/js-process/js-process.worker.d.ts"/>
 
import { IpcHeaders } from "../../core/ipc/IpcHeaders.cjs";
import { IpcResponse } from "../../core/ipc/IpcResponse.cjs";
import { createHttpDwebServer } from "../../sys/http-server/$listenHelper.cjs";
import html from "./assets/index.html" 

export const main = async () => {
    console.log("[statusbar.worker.mts] main self: ", self)
    
    /// 申请端口监听，不同的端口会给出不同的域名和控制句柄，控制句柄不要泄露给任何人
    const { origin, listen } = await createHttpDwebServer(jsProcess, {});
    console.log("[statusbar.worker.mts] origin:", origin)
    ;(await listen()).onRequest(async (request, httpServerIpc) => {
        console.log('[statusbar.worker.mts] 接受到了请求----： ', request)
        if (
            request.parsed_url.pathname === "/" ||
            request.parsed_url.pathname === "/index.html"
        ) {
            /// 收到请求
            httpServerIpc.postMessage(
                IpcResponse.fromText(
                    request.req_id,
                    200,
                    html,
                    new IpcHeaders({
                        "Content-Type": "text/html",
                    })
                )
            );
            return
        }

        if(request.parsed_url.pathname === "/port"){
            console.log("接受到了port", request.text())
        }

        httpServerIpc.postMessage(
            IpcResponse.fromText(request.req_id, 404, "No Found")
        );
        

        // setInterval(() => {
        //     httpServerIpc.postMessage(
        //         IpcResponse.fromText(request.req_id, 404, "No Found")
        //     );
        // }, 1000)

        // setTimeout(() => {
        //     postMessage("workerResult from statusbar.worker.mts");
        //     console.log('发送了postmessage')
        // }, 3000)
    });
  
};
main().catch(console.error);

 

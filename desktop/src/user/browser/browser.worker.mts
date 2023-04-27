/// <reference path="../../sys/js-process/js-process.worker.d.ts"/>
import { wwwServerOnRequest } from "./www-server-on-request.mjs"
import { createApiServerOnRequest } from "./api-server-on-request.mjs"
import { log } from "../../helper/devtools.cjs"
import { nativeOpen } from "../tool/tool.native.mjs"

const main = async () => {
  log.green('[browser.worker.mts bootstrap]')

  const { IpcEvent } = ipc;
  const wwwServer = await http.createHttpDwebServer(jsProcess, { subdomain: "www", port: 443 });
  const apiServer = await http.createHttpDwebServer(jsProcess, { subdomain: "api", port: 443 });
  // console.log('url: ', wwwServer.startResult.urlInfo.internal_origin)
  // http://www.browser.sys.dweb-443.localhost:22605/index.html?X-Dweb-Host=www.browser.sys.dweb%3A443#/toast)
  ; (await wwwServer.listen()).onRequest(wwwServerOnRequest)
    ; (await apiServer.listen()).onRequest(await createApiServerOnRequest(wwwServer.startResult.urlInfo.internal_origin, apiServer.startResult.urlInfo))
  // 打开 browser.sys.dweb 配套的html 页面
  // 验证 JSMM 之间通信 实现消息推送 不需要打开匹配的html
  {
    const interUrl = wwwServer.startResult.urlInfo.buildInternalUrl((url) => {
      url.pathname = "/index.html";
    }).href
    console.log("cot#open interUrl=>", interUrl)
    const view_id = await nativeOpen(interUrl)
  }

  // 等待他人的连接
  {
    jsProcess.onConnect((ipc) => {
      console.log('browser.worker.mts onConnect')
      ipc.onEvent((event, ipc) => {
        console.log("got event:", ipc.remote.mmid, event.name, event.text);
        setTimeout(() => {
          ipc.postMessage(IpcEvent.fromText(event.name, "echo:" + event.text));
        }, 500);
      });

      ipc.onMessage(() => {
        console.error('ipc onmessage')
      })
    });
  }
}

main();

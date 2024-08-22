import { INCOME, Router, jsProcess, streamRead, type $Core } from "@plaoc/server/middlewares";

const app = new Router();

app.use(async (event) => {
  console.log("api server:=>", event.request.url, jsProcess.mmid);
  // 测试普通拦截请求
  if (event.url.pathname.includes("/barcode-scanning")) {
    // 转发给不同的模块
    const response = await jsProcess.nativeFetch("file://barcode-scanning.sys.dweb/process", {
      method: event.method,
      body: event.body,
    });
    if (response.ok) {
      return response;
    } else {
      return Response.json(`decode error:${await response.text()}`);
    }
  }
  if (event.url.pathname.includes("/websocket")) {
    return testWebsocket(event);
  }
});

/**websocket测试 */
const testWebsocket = (event: $Core.IpcFetchEvent) => {
  const ipcRequest = event.ipcRequest;
  if (!event.ipcRequest.hasDuplex) {
    return { status: 500 };
  }
  const pureServerChannel = ipcRequest.getChannel();

  const channel = pureServerChannel.start();
  void (async () => {
    //  绑定自己前端发送的数据通道
    for await (const pureFrame of streamRead(channel[INCOME].stream)) {
      console.log("后端收到消息：", pureFrame);
    }
  })();
  return { status: 101 };
};

export default app;

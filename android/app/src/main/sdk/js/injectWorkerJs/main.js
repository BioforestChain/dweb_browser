/**
const webview_id = await fetch('file://mwebview.sys.dweb/open').number();
for await (const line of fetch(`file://mwebview.sys.dweb/listen/${webview_id}`).stream('jsonlines')) {
    const request = IpcRequest.from(await fetch(`file://mwebview.sys.dweb/request/${line.request_id}`).json());

    const response = IpcResponse(request, {...data});
    await fetch(`file://mwebview.sys.dweb/response`, { body:response });
}
 */

globalThis.fetch = (origin) => {
  try {
    globalThis.getConnectChannel(origin)
  } catch (error) {
    console.log(error)
  }
}

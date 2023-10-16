//@ts-check
const ws = new WebSocket(new URL("/super-channel", location.href.replace("http", "ws")));
/**
 * @type {WorkerGlobalScope& typeof globalThis}
 */
const worker = self;
self.onmessage = (event) => {
  ws.send(event.data);
};

ws.onmessage = async (event) => {
  /**
   * @type {MessageEvent<Blob|string>}
   */
  const data = event.data;
  if (data instanceof Blob) {
    const buffer = await data.arrayBuffer();
    worker.postMessage(buffer, [buffer]);
  } else {
    worker.postMessage(data);
  }
};

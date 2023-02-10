import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";

const html = String.raw;

export const CODE= async (request: IpcRequest) => html`
  <!DOCTYPE html>
  <html lang="en">
    <head>
      <meta charset="UTF-8" />
      <meta http-equiv="X-UA-Compatible" content="IE=edge" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
      <title>Desktop</title>
    </head>
    <body>
      <h1>你好，这是来自 WebWorker 的响应！</h1>
      <ol>
        <li>url:${request.url}</li>
        <li>method:${request.method}</li>
        <li>rawHeaders:${JSON.stringify(request.headers)}</li>
        <li>body:${await request.text()}</li>
      </ol>
      <div>
        <button id="test-readwrite-stream">启动双向信息流测试</button>
        <pre id="readwrite-stream-log"></pre>
      </div>
    </body>
    <script type="module" src="./desktop.web.mjs"></script>
  </html>
`;

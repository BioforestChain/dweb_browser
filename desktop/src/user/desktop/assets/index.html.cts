import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";

const html = String.raw;

export const CODE = async (request: IpcRequest) => html`
  <!DOCTYPE html>
  <html lang="en">
    <head>
      <meta charset="UTF-8" />
      <meta http-equiv="X-UA-Compatible" content="IE=edge" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
      <link rel="stylesheet" href="https://unpkg.com/@picocss/pico@1.*/css/pico.min.css">
      <title>Desktop</title>
      <style>
        :root {
          background: rgba(255, 255, 255, 0.9);
        }
        li {
          word-break: break-all;
        }
      </style>
    </head>
    <body>
      <h1>你好，这是来自 WebWorker 的响应！</h1>
      <ol>
        <li>url:${request.url}</li>
        <li>method:${request.method}</li>
        <li>rawHeaders:${JSON.stringify(request.headers, null, 2)}</li>
        <li>body:${await request.body.text()}</li>
      </ol>
      <div class="actions">
        <button id="open-btn">打开新窗口</button>
        <a href="/index.html?qaq=666" target="_blank">打开新窗口</a>
        <button id="close-btn">关闭当前窗口</button>
        <hr />
        <button id="open-camera">Open Camera</button>
        <video id="camera-view"></video>
      </div>
    </body>
    <script type="module" src="./desktop.web.mjs"></script>
  </html>
`;

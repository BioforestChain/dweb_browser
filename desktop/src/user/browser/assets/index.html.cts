import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";

const html = String.raw;

export const CODE = async (request: IpcRequest) => html`
  <!DOCTYPE html>
  <html lang="en">
    <head>
      <meta charset="UTF-8" />
      <meta http-equiv="X-UA-Compatible" content="IE=edge" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
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
        <!-- 这里需要分为两个部分 ，一个是状态栏等部分，一个是 第三方应用部分，
        问题是如何实现组合？？
        需要向这里注入一系列基础组件
        -->
      <h1>browser.web.html！</h1>
    </body>
    <!-- <script type="module" src="./desktop.web.mjs"></script> -->
  </html>
`;

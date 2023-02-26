import type { IpcRequest } from "../../../core/ipc/IpcRequest.cjs";
 
export const CODE = async (request: IpcRequest) => String.raw`
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
      <script type="text/javascript" src="./browser.web.mjs"></script>
      <home-page></home-pagep>
    </body>
  </html>
`;

var __freeze = Object.freeze;
var __defProp = Object.defineProperty;
var __template = (cooked, raw) => __freeze(__defProp(cooked, "raw", { value: __freeze(raw || cooked.slice()) }));

// src/user/desktop/assets/desktop.web.mjs.cts
var script = () => {
  const logEle = document.querySelector(
    "#readwrite-stream-log"
  );
  const log = (...logs) => {
    logEle.append(document.createTextNode(logs.join(" ") + "\n"));
  };
  const $ = (selector) => document.querySelector(selector);
  $("#open-btn").onclick = async () => {
    open(`/index.html?qaq=${encodeURIComponent(Date.now())}`);
  };
  $("#close-btn").onclick = async () => {
    close();
  };
  const cameraView = $("#camera-view");
  $("#open-camera").onclick = async () => {
    cameraView.setAttribute("playsinline", "");
    cameraView.setAttribute("autoplay", "");
    cameraView.setAttribute("muted", "");
    cameraView.style.width = "200px";
    cameraView.style.height = "200px";
    navigator.mediaDevices.getUserMedia(
      /* Setting up the constraint */
      {
        audio: false,
        video: {
          facingMode: "user"
          // Can be 'user' or 'environment' to access back or front camera (NEAT!)
        }
      }
    ).then(
      function success(stream) {
        cameraView.srcObject = stream;
        console.log(stream);
        cameraView.play();
      },
      (err) => {
        console.error(err);
      }
    );
  };
  window.onbeforeunload = (event) => {
    event.returnValue = "\u522B\u8D70\uFF01\uFF01";
  };
};
var CODE = async (require2) => {
  return script.toString().match(/\{([\w\W]+)\}/)[1];
};

// src/user/desktop/assets/index.html.cts
var html = String.raw;
var _a;
var CODE2 = async (request) => html(_a || (_a = __template(['\n  <!DOCTYPE html>\n  <html lang="en">\n    <head>\n      <meta charset="UTF-8" />\n      <meta http-equiv="X-UA-Compatible" content="IE=edge" />\n      <meta name="viewport" content="width=device-width, initial-scale=1.0" />\n      <link rel="stylesheet" href="https://unpkg.com/@picocss/pico@1.*/css/pico.min.css">\n      <title>Desktop</title>\n      <style>\n        :root {\n          background: rgba(255, 255, 255, 0.9);\n        }\n        li {\n          word-break: break-all;\n        }\n      </style>\n    </head>\n    <body>\n      <h1>\u4F60\u597D\uFF0C\u8FD9\u662F\u6765\u81EA WebWorker \u7684\u54CD\u5E94\uFF01</h1>\n      <ol>\n        <li>url:', "</li>\n        <li>method:", "</li>\n        <li>rawHeaders:", "</li>\n        <li>body:", '</li>\n      </ol>\n      <div class="actions">\n        <button id="open-btn">\u6253\u5F00\u65B0\u7A97\u53E3</button>\n        <a href="/index.html?qaq=666" target="_blank">\u6253\u5F00\u65B0\u7A97\u53E3</a>\n        <button id="close-btn">\u5173\u95ED\u5F53\u524D\u7A97\u53E3</button>\n        <hr />\n        <button id="open-camera">Open Camera</button>\n        <video id="camera-view"></video>\n      </div>\n    </body>\n    <script type="module" src="./desktop.web.mjs"><\/script>\n  </html>\n'])), request.url, request.method, JSON.stringify(request.headers, null, 2), await request.body.text());

// src/user/desktop/desktop.worker.mts
console.log("ookkkkk, i'm in worker");
var main = async () => {
  const { IpcHeaders, IpcResponse } = ipc;
  const { createHttpDwebServer } = http;
  const httpDwebServer = await createHttpDwebServer(jsProcess, {});
  console.log("will do listen!!", httpDwebServer.startResult.urlInfo.host);
  (await httpDwebServer.listen()).onRequest(async (request, httpServerIpc) => {
    console.log("worker on request", request.parsed_url);
    if (request.parsed_url.pathname === "/" || request.parsed_url.pathname === "/index.html") {
      console.log("request body text:", await request.body.text());
      httpServerIpc.postMessage(
        IpcResponse.fromText(
          request.req_id,
          200,
          new IpcHeaders({
            "Content-Type": "text/html",
            "Access-Control-Allow-Origin": "*",
            "Access-Control-Allow-Headers": "*",
            // 要支持 X-Dweb-Host
            "Access-Control-Allow-Methods": "*"
          }),
          await CODE2(request),
          httpServerIpc
        )
      );
    } else if (request.parsed_url.pathname === "/desktop.web.mjs") {
      httpServerIpc.postMessage(
        IpcResponse.fromText(
          request.req_id,
          200,
          new IpcHeaders({
            "Content-Type": "application/javascript"
          }),
          await CODE(request),
          httpServerIpc
        )
      );
    } else {
      httpServerIpc.postMessage(
        IpcResponse.fromText(
          request.req_id,
          404,
          void 0,
          "No Found",
          httpServerIpc
        )
      );
    }
  });
  console.log("http \u670D\u52A1\u521B\u5EFA\u6210\u529F");
  const main_url = httpDwebServer.startResult.urlInfo.buildInternalUrl("/index.html").href;
  {
  }
  {
    console.log("\u6253\u5F00\u6D4F\u89C8\u5668\u9875\u9762", main_url);
    const view_id = await jsProcess.nativeFetch(
      `file://mwebview.sys.dweb/open?url=${encodeURIComponent(main_url)}`
    ).text();
  }
};
main().catch(console.error);
export {
  main
};

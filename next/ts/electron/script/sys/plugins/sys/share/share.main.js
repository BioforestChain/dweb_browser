"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.ShareNMM = void 0;
const micro_module_native_js_1 = require("../../../../core/micro-module.native.js");
const devtools_js_1 = require("../../../../helper/devtools.js");
const handlers_js_1 = require("./handlers.js");
class ShareNMM extends micro_module_native_js_1.NativeMicroModule {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "mmid", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "share.sys.dweb"
        });
        Object.defineProperty(this, "httpNMM", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: void 0
        });
        Object.defineProperty(this, "waitForOperationResMap", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: new Map()
        });
    }
    async _bootstrap(context) {
        devtools_js_1.log.green(`[${this.mmid}] _bootstrap`);
        this.registerCommonIpcOnMessageHandler({
            method: "POST",
            pathname: "/share",
            matchMode: "full",
            input: {},
            output: "object",
            handler: handlers_js_1.share.bind(this)
        });
        // // create_stream_ipc
        // this.registerCommonIpcOnMessageHandler({
        //   method: "POST",
        //   pathname: "/share/create_stream_ipc",
        //   matchMode: "full",
        //   input: {},
        //   output: "object",
        //   handler: createStreamIpc.bind(this)
        // }); 
    }
    // private _share = async (req: IncomingMessage, res: OutgoingMessage) => {
    //   const origin = req.headers.origin;
    //   if(origin === undefined) throw new Error(`${this.mmid} _install origin === undefined`)
    //   const searchStr = req.url?.split("?")[1];
    //   if(searchStr === undefined) throw new Error(`${this.mmid} _share searchStr === undefined`)
    //   const query: querystring.ParsedUrlQuery= querystring.parse(searchStr);
    //   const title = query.title ? query.title : "";
    //   const text = query.text ? query.text : "";
    //   const _url = query.url ? query.url : "";
    //   if(typeof title !== "string") throw new Error(`tpeof title !== string`);
    //   if(typeof text !== "string") throw new Error(`typeof text !== string`);
    //   if(typeof _url !== "string") throw new Error(`typeof _url !== string`);
    //   let chunks = ""
    //   req.setEncoding('binary');
    //   req.on('data', chunk => chunks += chunk)
    //   req.on('end', () => {
    //     let file = querystring.parse(chunks, '\r\n', ':');
    //     let fileInfo = file['Content-Disposition'];
    //     let filename: string = "";
    //     if(fileInfo && typeof fileInfo === "object") {
    //       for(let value of fileInfo){
    //         if(value.includes("filename=")){
    //           filename = value.split("filename=")[1].slice(1, -1);
    //         }
    //       }
    //     }
    //     const url = `file://mwebview.sys.dweb/webview_execute_javascript_by_webview_url?`
    //     const init: RequestInit = {
    //       body: createShareUI(title, text, _url, filename),
    //       method: "POST",
    //       headers: {
    //         "webview_url": origin
    //       }
    //     }
    //     this.nativeFetch(url, init)
    //     res.end(JSON.stringify({
    //       success: true,
    //       message: "ok"
    //     }))
    //   })
    // } 
    _shutdown() {
        throw new Error("Method not implemented.");
    }
}
exports.ShareNMM = ShareNMM;
function createShareUI(title, text, url, filename) {
    const elContainerStyle = `
    position: fixed; 
    z-index: 999999999; 
    left: 0px; 
    top: 0px; 
    width: 100%; 
    height: 100%; 
    display: flex; 
    justify-content: center; 
    align-items: center; 
    background: #00000099;
  `;
    const elTitleStyle = `
    font-weight: 900;
  `;
    const elCotentStyle = `
    margin: 5px 0px 20px 0px;
  `;
    return `
    (() => {
      const elContainer = document.createElement('div');

      const elPanel = document.createElement('div');

      const elTitleLabel = document.createElement('h3');
      const elTextLabel = document.createElement('h3');
      const elUrlLabel = document.createElement('h3');
      const elFileLabel = document.createElement('h3');

      const elTitleContent = document.createElement('p');
      const elTextContent = document.createElement('p');
      const elUrlContent = document.createElement('p');
      const elFileContent = document.createElement('p');

      function onClick(){
        elContainer.remove()
        elContainer.removeEventListener('click', onClick)
      }

      elContainer.style = \`${elContainerStyle}\`;
      elContainer.addEventListener('click', onClick)

      elPanel.style = 'width: 90%; padding: 20px 10px; border-radius: 5px; background: #FFFFFF;';

      elTitleLabel.style = \`${elTitleStyle}\`
      elTextLabel.style = \`${elTitleStyle}\`
      elUrlLabel.style = \`${elTitleStyle}\`
      elFileLabel.style = \`${elTitleStyle}\`
      elTitleLabel.innerHTML = '标题:';
      elTextLabel.innerHTML = '文字内容:';
      elUrlLabel.innerHTML = '链接:';
      elFileLabel.innerHTML = '文件：'

      elTitleContent.style = \`${elCotentStyle}\`
      elTextContent.style = \`${elCotentStyle}\`
      elUrlContent.style = \`${elCotentStyle}\`
      elFileContent.style = \`${elCotentStyle}\`
      elTitleContent.innerHTML = '${title}';
      elTextContent.innerHTML = '${text}';
      elUrlContent.innerHTML = '${url}';
      elFileContent.innerHTML = '${filename === "" ? "没有选择任何文件" : filename}'

      elPanel.append(elTitleLabel, elTitleContent, elTextLabel, elTextContent, elUrlLabel, elUrlContent, elFileLabel, elFileContent)
      elContainer.append(elPanel);
      document.body.append(elContainer);
    })()
  `;
}

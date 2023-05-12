import { PromiseOut } from "../../../helper/PromiseOut.mjs";
import WebviewTag = Electron.WebviewTag;

class Webview {
  constructor(readonly id: number, readonly src: string) {}
  webContentId = -1;
  webContentId_devTools = -1;
  private _api!: WebviewTag;
  get api(): WebviewTag {
    return this._api;
  }
  doReady(value: WebviewTag) {
    this._api = value;
    this._api_po.resolve(value);
    console.log('执行了 doReady')
  }
  private _api_po = new PromiseOut<WebviewTag>();
  ready() {
    return this._api_po.promise;
  }
  closing = false;
  state = {
    zIndex: 0,
    openingIndex: 0,
    closingIndex: 0,
    scale: 1,
    opacity: 1,
    // translateY: 0,
  };
}

export {
  Webview
}

  
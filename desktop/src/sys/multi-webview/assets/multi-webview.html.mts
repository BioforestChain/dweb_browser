/// <reference lib="DOM"/>

import { proxy } from "comlink";
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { repeat } from "lit/directives/repeat.js";
import { styleMap } from "lit/directives/style-map.js";
import {
  exportApis,
  mainApis,
} from "../../../helper/openNativeWindow.preload.mjs";
import { Webview } from "./multi-webview.mjs"
import "./multi-webview-content.html.mjs"
import "./multi-webview-devtools.html.mjs"
import WebviewTag = Electron.WebviewTag;
import type { 
  CustomEventDomReadyDetail,
  CustomEventAnimationendDetail
} from "./multi-webview-content.html.mjs"

@customElement("view-tree")
export class ViewTree extends LitElement {
  // Define scoped styles right with your component, in plain CSS
  static override styles = [
    css`
      :host {
        display: flex;
        flex-direction: row;

        height: 100%;

        grid-template-areas: "layer";
      }
      * {
        box-sizing: border-box;
      }
      .layer {
        grid-area: layer;
        /* flex: 1; */
        display: grid;
        grid-template-areas: "layer-content";
        height: 100%;
        padding: 0.5em 1em;
      }
      .app-container{
        flex-grow: 0;
        flex-shrink: 0;
        width: 375px;
        height: 812px;
      }
      .webview-container {
        position: relative;
        grid-area: layer-content;

        height: 100%;

        display: grid;
        grid-template-areas: "webview";
        grid-template-rows: 1fr;
      }

      .webview-statusbar{
        position: absolute;
        left: 0px;
        top: 0px;
        z-index: 100;
        width: 100%;
        height: 80px;
        background: #9993;
      }

      .webview-content{
        position: absolute;
        left: 0px;
        top: 0px;
        box-sizing: border-box;
        width: 100%;
        min-height: 100%;
        z-index: 0;
        border: 10px solid blue;
      }

      .devtool-container {
        grid-area: layer-content;

        height: 100%;

        display: grid;
        grid-template-areas: "toolbar" "devtool";
        grid-template-rows: 2em 1fr;
      }
    `,
    css`
      .webview {
        grid-area: webview;

        width: 100%;
        height: 100%;
        border: 0;
        outline: 1px solid red;
        border-radius: 1em;
        overflow: hidden;
      }
      .devtool {
        grid-area: devtool;

        width: 100%;
        height: 100%;
        border: 0;
        outline: 1px solid blue;
        border-radius: 1em;
        overflow: hidden;
      }
      .dev-tools-container{
        min-width:500px;
      }
      .toolbar {
        grid-area: toolbar;

        display: flex;
        height: 100%;
        flex-direction: row;
        align-items: center;
      }
      fieldset {
        border: 0;
        padding: 0;
      }
      fieldset:disabled {
        display: none;
      }
    `,
    css`
      :host {
        --easing: cubic-bezier(0.36, 0.66, 0.04, 1);
      }
      .opening > .ani-view {
        animation: slideIn 3520ms var(--easing) forwards;
      }
      .closing > .ani-view {
        animation: slideOut 830ms var(--easing) forwards;
      }
      @keyframes slideIn {
        0% {
          transform: translateY(60%) translateZ(0);
          opacity: 0.4;
        }
        100% {
          transform: translateY(0%) translateZ(0);
          opacity: 1;
        }
      }
      @keyframes slideOut {
        0% {
          transform: translateY(0%) translateZ(0);
          opacity: 1;
        }
        30% {
          transform: translateY(-30%) translateZ(0) scale(0.4);
          opacity: 0.6;
        }
        100% {
          transform: translateY(-100%) translateZ(0) scale(0.3);
          opacity: 0.5;
        }
      }
    `,
    css`
      .stack {
        /* display: inline-grid;
        place-items: center;
        align-items: flex-end; */
      }
      .stack > * {
        grid-column-start: 1;
        grid-row-start: 1;
        width: 100%;
        transition-duration: 520ms;
        transition-timing-function: var(--easing);
        /* pointer-events: none; */
        transition-property: transform, opacity;
        // backdrop-filter: blur(5px);
        z-index: var(--z-index, 1);
        transform: translateZ(0) scale(var(--scale, 1));
        opacity: var(--opacity, 1);
      }
      .stack > .closing {
        pointer-events: none;
      }
      
      
      /* 
      .stack > .opening {
        transform: translateY(min(10%, 10px)) translateZ(0) scale(0.9);
        z-index: 1;
        opacity: 0.6;
      }
      .stack > .opening.opening-2 {
        transform: translateY(min(5%, 5px)) translateZ(0) scale(0.95);
        z-index: 2;
        opacity: 0.8;
      }
      .stack > .opening.opening-1 {
        transform: translateY(0) translateZ(0) scale(1);
        z-index: 3;
        opacity: 1;
        pointer-events: unset;
      } */
    `,
  ];

  // Declare reactive properties
  @property()
  name?: string = "Multi Webview";

  private webviews: Array<Webview> = [];
  /** 对webview视图进行状态整理 */
  private _restateWebviews() {
    let index_acc = 0;

    let closing_acc = 0;
    let opening_acc = 0;

    let scale_sub = 0.05;
    let scale_acc = 1 + scale_sub;

    // let y_sub = 5;
    // let y_acc = 0 + y_sub;

    let opacity_sub = 0.1;
    let opacity_acc = 1 + opacity_sub;
    for (const webview of this.webviews) {
      webview.state.zIndex = this.webviews.length - ++index_acc;

      if (webview.closing) {
        webview.state.closingIndex = closing_acc++;
      } else {
        {
          webview.state.scale = scale_acc -= scale_sub;
          scale_sub = Math.max(0, scale_sub - 0.01);
        }
        {
          webview.state.opacity = opacity_acc - opacity_sub;
          opacity_acc = Math.max(0, opacity_acc - opacity_sub);
        }
        {
          webview.state.openingIndex = opening_acc++;
        }
      }
    }
    this.requestUpdate("webviews");
  }

  private _id_acc = 0;

  openWebview(src: string) {
    const webview_id = this._id_acc++;
    this.webviews.unshift(new Webview(webview_id, src));
    this._restateWebviews();
    return webview_id;
  }
  closeWebview(webview_id: number) {
    const webview = this.webviews.find((dialog) => dialog.id === webview_id);
    if (webview === undefined) {
      return false;
    }
    webview.closing = true;
    this._restateWebviews();
    return true;
  }
  private _removeWebview(webview: Webview) {
    const index = this.webviews.indexOf(webview);
    if (index === -1) {
      return false;
    }
    this.webviews.splice(index, 1);
    this._restateWebviews();
    return true;
  }

  private async onWebviewReady(webview: Webview, ele: WebviewTag) {
    webview.webContentId = ele.getWebContentsId();
    console.log('-开支执行doReady')
    webview.doReady(ele);
    mainApis.denyWindowOpenHandler(
      webview.webContentId,
      proxy((detail) => {
        console.log(detail);
        this.openWebview(detail.url);
      })
    );
    mainApis.onDestroy(
      webview.webContentId,
      proxy(() => {
        this.closeWebview(webview.id);
        console.log("Destroy!!");
      })
    );
    const webcontents = await mainApis.getWenContents(webview.webContentId);
    console.log("webcontents", webcontents);
  }

  private async onDevtoolReady(webview: Webview, ele_devTool: WebviewTag) {
    await webview.ready();
    if (webview.webContentId_devTools === ele_devTool.getWebContentsId()) {
      return;
    }
    webview.webContentId_devTools = ele_devTool.getWebContentsId();
    console.log(" webview.webContentId_devTools: ",  webview.webContentId_devTools)
    await mainApis.openDevTools(
      webview.webContentId,
      undefined,
      webview.webContentId_devTools
    );
  }

  private async destroyWebview(webview: Webview) {
    await mainApis.destroy(webview.webContentId);
  }

  // Render the UI as a function of component state
  override render() {
    return html`
      <div class="layer stack app-container">
        ${repeat(
          this.webviews,
          (dialog) => dialog.id,
          (webview) => {
            console.log('content - webview:', webview)
            const zIndexStr = `z-index: ${webview.state.zIndex};`
            const _styleMap = styleMap({
              zIndex: webview.state.zIndex + "",
             
            })
            console.log('this.webvew.src: ', webview.src)
            return html`
              <multi-webview-content
                .customWebview=${webview}
                .closing=${webview.closing}
                .zIndex=${webview.state.zIndex}
                .scale=${webview.state.scale}
                .opacity=${webview.state.opacity}
                .customWebviewId=${webview.id}
                .src=${webview.src}
                style="${_styleMap}"
                @animationend=${(event: CustomEvent<CustomEventAnimationendDetail>) => {
                   if (event.detail.event.animationName === "slideOut" && event.detail.customWebview.closing) {
                    this._removeWebview(webview);
                  }
                }} 
                @dom-ready=${(event: CustomEvent<CustomEventDomReadyDetail>) => {
                  console.log('内容准备完毕')
                  this.onWebviewReady(webview, event.detail.event.target as WebviewTag);
                }}
              ></multi-webview-content>
            `;
          }
        )}
      </div>
      <div class="layer stack dev-tools-container" style="flex: 2.5;">
        ${repeat(
          this.webviews,
          (dialog) => dialog.id,
          (webview) => {
            const _styleMap = styleMap({
              zIndex: webview.state.zIndex + "",
            })
            
            return html`
              <multi-webview-devtools
                .customWebview=${webview}
                .closing=${webview.closing}
                .zIndex=${webview.state.zIndex}
                .scale=${webview.state.scale}
                .opacity=${webview.state.opacity}
                .customWebviewId=${webview.id}
                style="${_styleMap}"
                @dom-ready=${(event: CustomEvent & { target: WebviewTag }) => {
                  this.onDevtoolReady(webview, event.detail.event.target as WebviewTag);
                }}
                @destroy-webview=${() =>this.destroyWebview(webview)}
              ></multi-webview-devtools>
            `
          }
        )}
      </div>
    `;
  }
}

// class Webview {
//   constructor(readonly id: number, readonly src: string) {}
//   webContentId = -1;
//   webContentId_devTools = -1;
//   private _api!: WebviewTag;
//   get api(): WebviewTag {
//     return this._api;
//   }
//   doReady(value: WebviewTag) {
//     this._api = value;
//     this._api_po.resolve(value);
//   }
//   private _api_po = new PromiseOut<WebviewTag>();
//   ready() {
//     return this._api_po.promise;
//   }
//   closing = false;
//   state = {
//     zIndex: 0,
//     openingIndex: 0,
//     closingIndex: 0,
//     scale: 1,
//     opacity: 1,
//     // translateY: 0,
//   };
// }

const viewTree = new ViewTree();
document.body.appendChild(viewTree);
 
export const APIS = {
  openWebview: viewTree.openWebview.bind(viewTree),
  closeWebview: viewTree.closeWebview.bind(viewTree),
};

exportApis(APIS);

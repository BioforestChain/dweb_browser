/// <reference lib="DOM"/>

import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
import { repeat } from "lit/directives/repeat.js";

@customElement("view-tree")
export class ViewTree extends LitElement {
  // Define scoped styles right with your component, in plain CSS
  static override styles = [
    css`
      :host {
        display: flex;
        flex-direction: column;

        height: 100%;

        grid-template-areas: "layer";
      }
      .layer {
        grid-area: layer;

        display: grid;
        grid-template-areas: "webview";

        height: 100%;
        padding: 1em;
      }
      .webview-container {
        grid-area: webview;

        outline: 1px solid red;
        border-radius: 1em;
        overflow: hidden;
        height: 100%;
      }
      .webview {
        width: 100%;
        height: 100%;
        border: 0;
        animation: slideIn 520ms ease-out forwards;
      }
      .webview.closing {
        animation: slideOut 520ms ease-in forwards;
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
        100% {
          transform: translateY(-100%) translateZ(0);
          opacity: 0.4;
        }
      }
    `,
    css`
      .stack {
        display: inline-grid;
        place-items: center;
        align-items: flex-end;
      }
      .stack > * {
        grid-column-start: 1;
        grid-row-start: 1;
        transform: translateY(min(10%, 10px)) translateZ(0) scale(0.9);
        z-index: 1;
        width: 100%;
        opacity: 0.6;

        transition-duration: 520ms;
        transition-timing-function: ease-out;
        pointer-event: none;
        transition-property: transform, opacity;
        // backdrop-filter: blur(5px);
      }
      .stack > *:nth-child(2) {
        transform: translateY(min(5%, 5px)) translateZ(0) scale(0.95);
        z-index: 2;
        opacity: 0.8;
      }
      .stack > *:nth-child(1) {
        transform: translateY(0) translateZ(0) scale(1);
        z-index: 3;
        opacity: 1;
        pointer-event: unset;
      }
    `,
  ];

  // Declare reactive properties
  @property()
  name?: string = "Multi Webview";

  private webviews: Array<{ src: string; id: number; closing: boolean }> = [];
  private _id_acc = 0;

  openWebview(src: string) {
    const webview_id = this._id_acc++;
    this.webviews.unshift({ src: src, id: webview_id, closing: false });
    this.requestUpdate("webviews");
    return webview_id;
  }
  closeWebview(webview_id: number) {
    const webview = this.webviews.find((dialog) => dialog.id === webview_id);
    if (webview === undefined) {
      return false;
    }
    webview.closing = true;
    this.requestUpdate("webviews");
    return true;
  }
  private _removeWebview(webview_id: number) {
    const index = this.webviews.findIndex((dialog) => dialog.id === webview_id);
    if (index === -1) {
      return false;
    }
    this.webviews.splice(index, 1);
    this.requestUpdate("webviews");
    return true;
  }

  private onWebviewLoad(webview: HTMLIFrameElement, webview_id: number) {
    webview.contentWindow!.close = () => {
      this.closeWebview(webview_id);
    };
  }

  // Render the UI as a function of component state
  override render() {
    return html`
      <div class="layer stack">
        ${repeat(
          this.webviews,
          (dialog) => dialog.id,
          ({ id, src, closing }, index) => {
            console.log(id, src);
            return html`
              <div class="webview-container">
                <iframe
                  id="view-${id}"
                  class="webview ${closing ? "closing" : ""}"
                  src=${src}
                  partition="trusted"
                  @animationend=${(event: any) => {
                    closing && this._removeWebview(id);
                  }}
                  @load=${(event: any) => this.onWebviewLoad(event.target, id)}
                ></iframe>
              </div>
            `;
          }
        )}
      </div>
    `;
  }
}

const viewTree = new ViewTree();
document.body.appendChild(viewTree);
console.log(viewTree);

const nww = nw.Window.get(window);
nww.on("new-win-policy", function (frame, url, policy) {
  policy.ignore();

  viewTree.openWebview(url);
});
nww.on("close", () => {
  console.log("closed");
});

export const APIS = {
  openWebview: viewTree.openWebview.bind(viewTree),
};

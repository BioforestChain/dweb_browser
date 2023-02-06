/// <reference lib="DOM"/>

import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";

@customElement("view-tree")
export class ViewTree extends LitElement {
  // Define scoped styles right with your component, in plain CSS
  static override styles = css`
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
      /* 最高是 1, 其它是 0,-1,-2 */
      z-index: var(--index);
      /* 从 1 开始,1/2,1/3,1/4... */
      --n-scale: calc(1 / ((var(--index, 1) - 2) * -1));
      --translate: calc((1 - var(--n-scale)) * 1em);
      transform: translate(var(--translate), var(--translate));
      opacity: var(--n-scale);

      transition-duration: 520ms;
      transition-timing-function: ease-out;
    }
  `;

  // Declare reactive properties
  @property()
  name?: string = "Multi Webview";

  private url_tree = new Map<number, { src: string; id: number }[]>();
  private _url_id_acc = 0;

  openWebview(id: number, src: string) {
    let urls = this.url_tree.get(id);
    if (urls === undefined) {
      this.url_tree.set(id, (urls = []));
    }
    const url_id = this._url_id_acc++;
    urls.push({ src: src, id: url_id });
    this.requestUpdate("url_tree");
    return url_id;
  }

  // Render the UI as a function of component state
  override render() {
    return html`
      ${[...this.url_tree.entries()].map(([id, urls]) => {
        return html`<div id="layer-${id}" class="layer">
          ${urls.map(({ src, id }, index, list) => {
            return html`
              <div
                class="webview-container"
                style="--index:${index + 2 - list.length};"
              >
                <webview
                  id="view-${id}"
                  src=${src}
                  partition="trusted"
                ></webview>
              </div>
            `;
          })}
        </div>`;
      })}
    `;
  }
}

const viewTree = new ViewTree();
document.body.appendChild(viewTree);

export const APIS = {
  openWebview: viewTree.openWebview.bind(viewTree),
};

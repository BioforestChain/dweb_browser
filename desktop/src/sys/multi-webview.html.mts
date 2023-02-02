/// <reference lib="DOM"/>

import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";

@customElement("view-tree")
export class ViewTree extends LitElement {
  // Define scoped styles right with your component, in plain CSS
  static override styles = css`
    :host {
      color: blue;
    }
    webview {
      outline: 1px solid red;
      min-height: 80vh;
    }
  `;

  // Declare reactive properties
  @property()
  name?: string = "Multi Webview";

  private url_tree = new Map<number, { src: string; id: number }[]>();
  private _url_id_acc = 0;

  openWebview(id: number, url: string) {
    debugger;
    let urls = this.url_tree.get(id);
    if (urls === undefined) {
      this.url_tree.set(id, (urls = []));
    }
    const url_id = this._url_id_acc++;
    urls.push({ src: new URL(url, location.href).href, id: url_id });
    this.requestUpdate("url_tree");
    return url_id;
  }

  // Render the UI as a function of component state
  override render() {
    return html`<p>Hello, ${this.name}!</p>
      ${[...this.url_tree.entries()].map(([id, urls]) => {
        return html`<div id="layer-${id}">
          ${urls.map(({ src, id }) => {
            return html`<webview id="view-${id}" src=${src}></webview>`;
          })}
        </div>`;
      })} `;
  }
}

const viewTree = new ViewTree();
document.body.appendChild(viewTree);

export const APIS = {
  openWebview: viewTree.openWebview.bind(viewTree),
};

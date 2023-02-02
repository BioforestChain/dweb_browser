var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
var __metadata = (this && this.__metadata) || function (k, v) {
    if (typeof Reflect === "object" && typeof Reflect.metadata === "function") return Reflect.metadata(k, v);
};
import { css, html, LitElement } from "lit";
import { customElement, property } from "lit/decorators.js";
let ViewTree = class ViewTree extends LitElement {
    constructor() {
        super(...arguments);
        // Declare reactive properties
        this.name = "Multi Webview";
        this.url_tree = new Map();
        this._url_id_acc = 0;
    }
    openWebview(id, url) {
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
    render() {
        return html `<p>Hello, ${this.name}!</p>
      ${[...this.url_tree.entries()].map(([id, urls]) => {
            return html `<div id="layer-${id}">
          ${urls.map(({ src, id }) => {
                return html `<iframe id="view-${id}" src=${src}></iframe>`;
            })}
        </div>`;
        })} `;
    }
};
// Define scoped styles right with your component, in plain CSS
ViewTree.styles = css `
    :host {
      color: blue;
    }
    webview {
      outline: 1px solid red;
    }
  `;
__decorate([
    property(),
    __metadata("design:type", String)
], ViewTree.prototype, "name", void 0);
ViewTree = __decorate([
    customElement("view-tree")
], ViewTree);
export { ViewTree };
const viewTree = new ViewTree();
document.body.appendChild(viewTree);
export const APIS = {
    openWebview: viewTree.openWebview.bind(viewTree),
};

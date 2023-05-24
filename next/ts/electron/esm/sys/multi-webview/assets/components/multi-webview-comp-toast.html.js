var __decorate = (this && this.__decorate) || function (decorators, target, key, desc) {
    var c = arguments.length, r = c < 3 ? target : desc === null ? desc = Object.getOwnPropertyDescriptor(target, key) : desc, d;
    if (typeof Reflect === "object" && typeof Reflect.decorate === "function") r = Reflect.decorate(decorators, target, key, desc);
    else for (var i = decorators.length - 1; i >= 0; i--) if (d = decorators[i]) r = (c < 3 ? d(r) : c > 3 ? d(target, key, r) : d(target, key)) || r;
    return c > 3 && r && Object.defineProperty(target, key, r), r;
};
import { css, html, LitElement } from "lit";
import { customElement, property, state } from "lit/decorators.js";
import { classMap } from "lit/directives/class-map.js";
let MultiWebviewCompToast = class MultiWebviewCompToast extends LitElement {
    constructor() {
        super(...arguments);
        Object.defineProperty(this, "_message", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "test message"
        });
        Object.defineProperty(this, "_duration", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: `1000`
        });
        Object.defineProperty(this, "_position", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: "top"
        });
        Object.defineProperty(this, "_beforeEntry", {
            enumerable: true,
            configurable: true,
            writable: true,
            value: true
        });
    }
    firstUpdated() {
        setTimeout(() => {
            this._beforeEntry = false;
        }, 0);
    }
    transitionend(e) {
        if (this._beforeEntry) {
            e.target.remove();
            return;
        }
        setTimeout(() => {
            this._beforeEntry = true;
        }, parseInt(this._duration));
    }
    render() {
        const containerClassMap = {
            container: true,
            before_entry: this._beforeEntry ? true : false,
            after_entry: this._beforeEntry ? false : true,
            container_bottom: this._position === "bottom" ? true : false,
            container_top: this._position === "bottom" ? false : true
        };
        return html `
      <div 
        class=${classMap(containerClassMap)}
        @transitionend=${this.transitionend}  
      >
        <p class="message">${this._message}</p>
      </div>
    `;
    }
};
Object.defineProperty(MultiWebviewCompToast, "styles", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: createAllCSS()
});
Object.defineProperty(MultiWebviewCompToast, "properties", {
    enumerable: true,
    configurable: true,
    writable: true,
    value: {
        _beforeEntry: { state: true }
    }
});
__decorate([
    property({ type: String })
], MultiWebviewCompToast.prototype, "_message", void 0);
__decorate([
    property({ type: String })
], MultiWebviewCompToast.prototype, "_duration", void 0);
__decorate([
    property({ type: String })
], MultiWebviewCompToast.prototype, "_position", void 0);
__decorate([
    state()
], MultiWebviewCompToast.prototype, "_beforeEntry", void 0);
MultiWebviewCompToast = __decorate([
    customElement("multi-webview-comp-toast")
], MultiWebviewCompToast);
export { MultiWebviewCompToast };
function createAllCSS() {
    return [
        css `
      .container{
        position: absolute;
        left: 0px;
        box-sizing: border-box;
        padding: 0px 20px;
        width: 100%;
        transition: all 0.25s ease-in-out;
      }

      .container_bottom{
        bottom: 0px;
      }

      .container_top{
        top: 0px;
      }

      .before_entry{
        transform: translateX(-100vw);
      }

      .after_entry{
        transform: translateX(0vw)
      }

      .message{
        box-sizing: border-box;
        padding: 0px 6px;
        width: 100%;
        height: 38px;
        color: #FFFFFF;
        line-height: 38px;
        text-align: left;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
        background: #eee;
        border-radius: 5px;
        background: #1677ff;
      }
    `
    ];
}
